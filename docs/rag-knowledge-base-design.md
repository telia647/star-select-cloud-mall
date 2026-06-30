# 知识库模块（RAG 索引）设计文档

> 范围：mall-ai 私有知识库的离线索引链路、向量化、检索、与对话流程的集成。
> 参考：`docs/开发流程.md` 五阶段流程。

---

## 一、需求定义

### 业务目标
为商城智能客服提供私有知识来源，使大模型基于真实平台政策（退款、物流、活动规则）和商品信息回答用户问题，避免编造。

### 功能清单
1. 知识文档 CRUD（标题、分类、内容、启停状态）
2. 单条文档手动索引（拆分 → 向量化 → 写入向量库）
3. 商城商品批量同步为知识文档并索引
4. 索引状态可见（待索引 / 已索引 / 失败）+ 错误信息
5. 已索引文档支持重新索引（先删后写）
6. 批量同步返回新增 / 更新 / 失败统计

### 验收标准
- 索引成功后向量库可检索到内容
- 索引失败时 MySQL 与 Milvus 最终一致（无孤儿向量）
- 重新索引时旧向量必须被清除
- 用户提问能命中知识库内容（如"暗号是什么"）
- Snowflake ID 在前端展示不丢精度

### 异常场景处理
| 场景 | 处理 |
|---|---|
| Milvus 不可用 | 索引状态置 FAILED，记录错误信息 |
| 文档内容为空 | 拒绝索引，返回 BAD_REQUEST |
| DB 磁盘满 / 写失败 | `@Transactional` 回滚，Milvus 不动 |
| Embedding API 超时 | 索引失败，提示重试；对话端 6s 超时跳过 RAG |
| Milvus 写入半失败 | 下次索引时 `deleteChunks` 双层兜底清理孤儿 |

---

## 二、架构设计

### 数据架构

| 层 | 存储 | 集合 / 表 | 内容 |
|---|---|---|---|
| 元数据层 | MySQL | `ai_knowledge_doc` | 标题、分类、原文、状态、索引状态、错误信息、创建/更新人 |
| 切片层 | MySQL | `ai_knowledge_chunk` | `doc_id`、`chunk_no`、`content`、`vector_id`（关联键） |
| 向量层 | Milvus | `mall_knowledge_chunk_v2` | 1024 维向量 + chunk 文本 + metadata(`docId/docTitle/category/status/chunkNo`) |

**关联关系**：MySQL 的 `vector_id`（如 `doc-123-chunk-0`）是跨系统的关联键，本身不是向量值。

### 技术栈

| 组件 | 选型 | 备注 |
|---|---|---|
| Chat 模型 | DeepSeek `deepseek-chat` | 通过 Spring AI ChatClient |
| Embedding 模型 | 阿里 DashScope `text-embedding-v4` | 1024 维，国内访问稳定 |
| 向量数据库 | Milvus 2.5.4 standalone | IVF_FLAT + COSINE，懒加载封装 |
| 元数据库 | MySQL 8.4 | Flyway 自动建表 |

### 核心流程

**单条索引 `index(id)`**：
```
读 doc
  └─ deleteChunks(id)              # 双层兜底删 Milvus 孤儿
  └─ chunker.split → parts
  └─ for each chunk: 写 MySQL（事务）
  └─ for each chunk: 删 Milvus 旧 vectorId → 写新向量
  └─ 状态 = INDEXED / FAILED
```

**批量同步 `syncProducts()`**：
```
查 mall_product 上架商品
  └─ upsert 三种文档：
      ├─ "商城商品总览"   （目录概览）
      ├─ "商品分类-X"     （按分类聚合）
      └─ "商品-Y"          （单商品详情）
  └─ upsert 时已存在→更新内容并置 PENDING；不存在→新建
  └─ 对每个 doc 调用 index(id)
  └─ 返回 KnowledgeSyncResultResponse {created, updated, failed, items}
```

**在线检索**（在 `AiChatService`）：
```
用户问题
  └─ [并行] retrieve(question)
      └─ embedding → Milvus similaritySearch(topK=5, threshold=0.65)
      └─ 过滤 status=ENABLED
      └─ 6s 超时则跳过 RAG，不阻塞对话
  └─ [并行] resolveTool（订单 / 商品工具调用）
  └─ buildPrompt(question, history, documents, toolResult)
  └─ DeepSeek 流式输出
```

### 关键设计决策

| 决策 | 理由 |
|---|---|
| DB 先写、Milvus 后写 | DB 是事实来源；Milvus 失败可基于 DB 重建 |
| vectorId 固定为 `doc-{id}-chunk-{i}` | 可重入；重新索引时按 ID 精准删除，无孤儿 |
| `deleteChunks` 双层兜底 | 先按 docId metadata 删，失败再按 vectorId 列表删 |
| Snowflake ID 序列化为字符串 | 避免 JS Number 精度丢失（19位 > 2^53） |
| `KnowledgeSyncResultResponse` 统计返回 | 前端可展示明细而非笼统"成功/失败" |
| Embedding 模型与维度严格配对 | `text-embedding-v4` = 1024 维，Milvus collection 维度同步 |
| update() 自动置 PENDING | 防止用户修改文档但忘记重索引 |
| LazyMilvusVectorStore 懒初始化 | 首次访问才建立 gRPC，失败 30 秒冷却 |
| 对话端 DB 写后置 | SSE 第一个字节前零 DB 操作，conversationId 用 IdWorker 预生成 |

### 风险与对策

| 风险 | 对策 |
|---|---|
| Milvus gRPC 慢连接 / 不稳定 | LazyMilvusVectorStore + 30s 冷却 + connect-timeout 30s |
| 索引中断产生孤儿向量 | deleteChunks 双层兜底 + 重入式索引（vectorId 固定可覆盖） |
| 跨服务 ID 精度丢失 | KnowledgeDocResponse `@JsonSerialize(ToStringSerializer.class)` |
| 修改文档忘记重索引 | update() 自动重置 embeddingStatus=PENDING |
| Embedding API 国内访问慢 | 对话端 RAG 异步并行 + 6s 超时跳过 |
| 切换 embedding 模型导致维度变化 | 使用新 collection 名（如 `_v2`），保留旧数据可回滚 |

---

## 三、开发与实现

### 任务清单

| # | 任务 | 关键文件 | 状态 |
|---|------|---------|------|
| 1 | 实体 / Mapper / DTO / Controller | `entity/AiKnowledge*`, `controller/AiAdminKnowledgeController` | ✅ |
| 2 | 文本切片器 | `service/KnowledgeChunker.java` | ✅ |
| 3 | `index()` DB 先写、Milvus 后写 | `service/KnowledgeService.java` | ✅ |
| 4 | `deleteChunks` 双层兜底 | `service/KnowledgeService.java` | ✅ |
| 5 | `syncProducts` upsert + 统计 | `service/KnowledgeService.java` | ✅ |
| 6 | `KnowledgeSyncResultResponse` DTO | `dto/KnowledgeSyncResultResponse.java` | ✅ |
| 7 | DTO ID 字符串序列化 | `dto/KnowledgeDocResponse.java` | ✅ |
| 8 | LazyMilvusVectorStore 懒加载 + 冷却 | `config/LazyMilvusVectorStoreConfig.java` | ✅ |
| 9 | 前端列表 / 编辑 / 状态展示 | `mall-web/src/views/AdminAiKnowledgeView.vue` | ✅ |
| 10 | 前端单条重索引确认弹框 | `AdminAiKnowledgeView.vue` | ✅ |
| 11 | 前端批量同步结果展示 | `AdminAiKnowledgeView.vue` | ✅ |
| 12 | 切片单元测试 | `test/.../KnowledgeChunkerTest.java` | ✅ |

### 接口契约

**单条索引**
```
POST /ai/admin/knowledge/docs/{id}/embedding
→ Result<KnowledgeDocResponse>
```

**批量同步**
```
POST /ai/admin/knowledge/docs/sync-products
→ Result<KnowledgeSyncResultResponse>
  { created, updated, failed, items: [KnowledgeDocResponse] }
```

**KnowledgeDocResponse**
```json
{
  "id": "2071185512583942146",   // string，避免 JS 精度丢失
  "title": "...",
  "category": "...",
  "content": "...",
  "status": 1,                    // 0 停用 / 1 启用
  "embeddingStatus": 1,           // 0 待索引 / 1 已索引 / 2 失败
  "lastEmbeddingError": null,
  "updatedAt": "2026-06-28T..."
}
```

---

## 四、测试与质量保障

### 功能用例

| # | 场景 | 预期 |
|---|------|------|
| F1 | 新文档首次索引 | 状态 PENDING → INDEXED |
| F2 | 已索引文档点索引 | 前端弹确认框，确认后旧向量被清，新向量写入 |
| F3 | 文档内容修改 | 状态自动重置为 PENDING |
| F4 | 内容为空索引 | 报错 "knowledge content is empty" |
| F5 | Milvus 不可用 | 状态 FAILED，错误信息含原因 |
| F6 | 批量同步 30+ 文档 | 全部成功，统计明细正确 |
| F7 | 用户问"暗号是什么" | RAG 检索命中，LLM 返回知识库内容 |
| F8 | 索引后删除文档 | MySQL 记录清除，Milvus 向量清除 |

### 专项测试
- **切片算法**：`KnowledgeChunkerTest` 覆盖边界长度、重叠区域、中英文混合
- **重入测试**：同一文档连续索引 10 次，Milvus chunk 数量稳定
- **并发测试**：syncProducts 期间不阻塞用户对话（DB 写已后置到 LLM 流之后）
- **降级测试**：Milvus 关停后，对话端 RAG 跳过，工具调用与基础对话仍可用

### 已知限制
- Embedding API 在国内访问偶发慢，对话端 RAG 6s 超时直接跳过
- Milvus 集合维度一旦创建无法修改，更换模型需要新 collection 名
- 切片粒度（chunkSize=700, overlap=100）针对中文 FAQ 调优，长篇技术文档可能需要重调

---

## 五、发布与部署

### 前置条件
1. VM 配置 Docker 镜像加速器（参见 `deploy/README.md`）
2. `.env` 配置：
   ```
   MALL_AI_EMBEDDING_API_KEY=<DashScope key>
   MALL_AI_EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode
   MALL_AI_EMBEDDING_MODEL=text-embedding-v4
   MALL_MILVUS_EMBEDDING_DIMENSION=1024
   MALL_MILVUS_COLLECTION=mall_knowledge_chunk_v2
   ```
3. Flyway 自动建 4 张 ai 表
4. Milvus 启动后 collection 自动创建（`initialize-schema: true`）

### 发布步骤
1. `docker compose up -d` 启动中间件
2. 启动 `mall-ai`，Flyway 建表
3. 管理员录入文档 → 点索引，或调用 `sync-products` 初始化商品知识
4. 验证用户端对话 RAG 命中

### 回滚预案
- 代码 revert 到上一 commit
- 向量数据可直接 DROP Milvus collection，下次启动重建并重新索引
- MySQL 元数据保留，不影响其他模块

### 监控指标
- `embeddingStatus=FAILED` 的文档数 → 失败率
- 索引接口 P95 耗时
- Milvus 健康（`http://<host>:9091/healthz`）
- 对话日志中 `RAG retrieve failed` 频次

---

## 附：变更历史

| 日期 | 变更 |
|---|---|
| 2026-06-28 | 模块完整重写：对话流 DB 写后置、RAG/工具并行、双层兜底删除、统计返回 |
