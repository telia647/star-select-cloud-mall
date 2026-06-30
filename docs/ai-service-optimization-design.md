# mall-ai 模块优化设计文档

> 范围：针对 `mall-ai` 服务在事务范围、向量库交互、商品目录查询、SSE 推送、可读性与配置规范等维度的系统性优化。
> 参考：`docs/开发流程.md` 五阶段流程，`docs/ai-service-design.md` 模块基线，`docs/rag-knowledge-base-design.md` RAG 子模块。
> 触发：知识库与智能客服功能已上线，复盘 `AiChatService` / `KnowledgeService` / `LazyMilvusVectorStoreConfig` 时识别出 12 处影响并发承载、数据一致性与可维护性的问题。

---

## 一、需求定义

### 业务目标
在不改动对外 API、不调整表结构的前提下，让 `mall-ai` 模块在高并发对话与大批量知识库同步场景下保持稳定，并清理近期快速迭代中遗留的死代码、风格不一致等可维护性债务。

### 问题清单

| 编号 | 现状 | 影响 |
| --- | --- | --- |
| P1-1 | `AiChatService.chat()` 标 `@Transactional`，方法内含 DeepSeek 同步调用、远程 HTTP、跨库 JdbcTemplate 查询 | 单请求长时间占用 HikariCP 连接，10 连接默认池在 10 并发即耗尽，3s 超时后报 `Unable to acquire JDBC Connection`；模型超时还会触发事务回滚，出现"答复发出但 DB 未落"或半截记录 |
| P1-2 | `KnowledgeService.syncProducts()` 整方法 `@Transactional`，循环内嵌套 `index()`（含 Milvus RPC） | 数百商品的同步会持续几分钟、长锁住 `ai_knowledge_doc`；中途一条 Milvus 写入失败 → 事务回滚 → Milvus 已写向量无法回滚，留下"DB 不存在但向量库存在"的脏数据 |
| P1-3 | `KnowledgeService.index()` 对每个 chunk 单独调 `vectorStore.add(List.of(...))` | 每 chunk 一次 OpenAI embedding + 一次 Milvus 插入 RPC；N=8 时索引一篇文档 ≈ 1.6s，且容易触发 OpenAI 429 限流 |
| P1-4 | `AiChatService.listProductItems()` 每条商品意图问询都全表 JOIN 扫描 `pms_product`/`pms_category`/`pms_shop`/`pms_sku` | 商品目录变动慢但查询高频，无缓存导致 mall_product 库被 AI 流量持续压打，单次 100~300ms |
| P1-5 | `AiChatService.sendChunked()` 对快捷回答按 2 字符 + `Thread.sleep(15)` 拆段发送 | 30 字回答被拖到 ~450ms 才发完，且持续占用 `aiChatExecutor` 线程；线程池被占满后真正需要 LLM 的请求排队 |
| P2-6 | `AiChatService` 存在死代码：`buildPrompt`、`appendRecentMessages`、`saveAssistantSafely`、`isKnowledgeIntent`、内部 record `ChatContext` | 1200 行类阅读成本高、易误改不会执行的分支 |
| P2-7 | `AiChatService` 内所有中文字符串、关键字均为 `\uXXXX` 转义，`KnowledgeService` 却是裸中文 | 风格不统一；review、grep、IDE 跳转都不可读；新增关键字易写错 |
| P2-8 | `loadHistoryContext` 与 `appendRecentMessages` 重复实现历史拼接 | 维护时容易改 A 漏 B，问题 6 解决后自动消失 |
| P2-9 | `LazyMilvusVectorStoreConfig.vectorStore(...)` 通过 17 个 `@Value` 注入 | 新增配置要同步改方法签名 + 传参顺序 + record，参数顺序写错编译不报错 |
| P3-10 | `application.yml` 仅配 `connection-timeout`/`max-lifetime`，未显式配 `maximum-pool-size`/`minimum-idle` | 默认池 10，结合 P1-1 在中低并发就触顶；缺少容量基线 |
| P3-11 | `AdminAiKnowledgeView.vue` 中 `form.id: number \| undefined`，但 `KnowledgeDoc.id` 类型实为 `string` | 编辑路径在 TS 严格模式下不会拦截类型不一致，运行时可能让"编辑"误走"新建" |
| P3-12 | `LazyMilvusVectorStoreConfig.markUnavailable` 将原始 Milvus 异常消息原样存为 `lastFailureMessage`，30s 内每次调用都抛出 | 错误消息可能含主机、端口、认证细节，前端展示会造成信息泄漏 |

### 核心需求
1. **稳定性**：常见对话 / 知识库同步路径不再占用 DB 连接超过 100ms，DB 池容量与连接占用解耦。
2. **数据一致性**：DB 与 Milvus 之间的写入失败不再产生"半边脏数据"。
3. **吞吐**：知识库索引、商品意图问询的单次耗时降至当前 1/5 以内。
4. **可维护性**：删除死代码、统一字符串风格、统一配置绑定方式，让 `AiChatService` 主线只剩"快捷回答 / 工具直答 / RAG + LLM"三条路径。
5. **安全性**：Milvus 错误信息不外泄到调用方。

### 验收标准
- `AiChatService.chat()` 与 `streamChat()` 在执行 DeepSeek 调用、`OrderClient` 调用、跨库查询期间不持有 mall_ai 的事务/连接。
- `KnowledgeService.syncProducts()` 在写入 DB 后提交，再异步推送向量；中途 Milvus 失败仅把单条文档标 `EMBEDDING_FAILED`，不回滚已成功的其他文档。
- `KnowledgeService.index()` 对一篇 N 个 chunk 的文档触发 1 次 OpenAI embedding 批量调用 + 1 次 Milvus 批量插入。
- 商品目录类问题（"商城有什么手机"等）在 60s 窗口内命中本地缓存，仅首请求落 DB。
- 快捷回答端到端耗时 < 50ms，发送线程发完即释放。
- `AiChatService` 类瘦身至少 80 行（去除 P2-6 死代码 + P2-7 风格统一后行数）。
- `LazyMilvusVectorStoreConfig` 使用 `@ConfigurationProperties` 单一绑定。
- Milvus 故障期间调用方拿到的错误消息为通用文案，原始细节仅留在 `log.warn`。

### 异常场景

| 场景 | 期望行为 |
| --- | --- |
| DeepSeek 超时 / 5xx | 不影响已写入用户消息；助手消息以兜底文案落库，模型调用日志记录失败原因 |
| Milvus 在 `syncProducts` 中途宕机 | 已成功的 doc 保留 `EMBEDDING_INDEXED`，失败 doc 标 `EMBEDDING_FAILED`，管理页可单独重试 |
| OpenAI embedding 限流 | 批量调用整体失败时，doc 标 `EMBEDDING_FAILED`，不留半截 chunk 向量 |
| 商品目录缓存过期瞬间高并发 | Caffeine `refreshAfterWrite` 单飞，仅一个请求穿透 |
| 前端编辑现有文档保存 | 走 `update` 接口（PUT `/ai/admin/knowledge/docs/{id}`），不会因 id 类型不一致退化为新建 |

---

## 二、架构设计

### 改造前后对比

#### P1-1 事务范围
```
当前：
  chat()  ── @Transactional ────────────────────────────────────┐
            ├─ resolveConversationId (selectById)               │
            ├─ localQuickAnswer                                  │
            ├─ resolveTool → OrderClient HTTP + JdbcTemplate     │ 全程持锁
            ├─ retrieve → Milvus + embedding                     │
            ├─ chatClient.call() ── DeepSeek 5~15s              │
            └─ persistAll → insert × 2 + update × 1              │
                                                                 ┘
改造后：
  chat()  ── 无事务 ──────────────────────────────────────────┐
            ├─ resolveConversationId                          │
            ├─ resolveTool / retrieve / DeepSeek 调用         │ 不占连接
            └─ persistAll() ── @Transactional(内部) ──┐      │
                              insert × 2 + update × 1 │      │
                              （<50ms）              ┘      │
                                                              ┘
```

#### P1-2 / P1-3 知识同步与索引
```
当前：
  syncProducts() ── @Transactional ──────────────────────────────┐
    upsertProductDoc → DB insert/update                          │
    for each doc:                                                │
      index(id) ──┬─ delete old chunks (DB + Milvus by filter)   │ 几分钟
                  ├─ for each chunk:                              │ 持锁
                  │    chunkMapper.insert                         │
                  │    vectorStore.delete(id)                     │
                  │    vectorStore.add(单条 Document)             │
                  └─ doc.embeddingStatus = INDEXED/FAILED         │
                                                                 ┘

改造后：
  syncProducts():
    1) 阶段 A：@Transactional 内只做 DB 写入
       upsert doc rows + chunk rows，标 EMBEDDING_PENDING
       事务提交 → 释放连接
    2) 阶段 B：循环逐 doc 执行 indexVectorOnly(docId)
       indexVectorOnly:
         - 收集该 doc 全部 chunk
         - 一次性 vectorStore.delete(filter docId=?)
         - 一次性 vectorStore.add(List<Document> 批量)
         - 更新 doc.embeddingStatus（独立短事务）
       单 doc 失败仅标 FAILED，不影响其他 doc
```

#### P1-4 商品目录缓存
- 使用现有的 Caffeine（若未引入则在父 POM 增加 `caffeine`）。
- `ProductCatalogCache` 单例 bean，键为 `"all"`，TTL 60s，`refreshAfterWrite` 防止穿透。
- `queryProductCatalog` 与 `listProductItems` 调用统一走该缓存。
- 不做主动驱逐：60s 窗口内的延迟可接受；后续若需要可加 `pms_product` 变更事件。

#### P1-5 快捷回答 SSE
- 删除 `sendChunked` 的字符级拆分逻辑，改为一次性 `sendEvent(emitter, "delta", text)` 后立即 `done`。
- 快捷回答仍走 `aiChatExecutor`，但执行时长从 ~450ms 降至 ~5ms。

#### P2-9 Milvus 配置
- 新增 `MilvusClientProperties`（`@ConfigurationProperties(prefix="spring.ai.vectorstore.milvus")`）。
- `LazyMilvusVectorStoreConfig.vectorStore(EmbeddingModel, MilvusClientProperties)`，构造函数瘦身。

#### P3-12 错误信息脱敏
- `markUnavailable` 内部仍记录原始消息到日志（`log.warn("Milvus unavailable: {}", original)`）。
- `lastFailureMessage` 字段固定为 `"向量库暂时不可用，请稍后重试"`。

### 风险与对策

| 风险 | 对策 |
| --- | --- |
| 拆分事务后写入失败仍可能产生半截记录（user message 写入但 assistant message 未写） | `persistAll` 单事务包住两条 message + 一条 conversation 的所有写入，失败整体回滚；外层只 `log.warn` |
| 商品目录缓存与"立即生效"诉求冲突 | TTL 60s 已是业务可接受窗口；管理后台仍直接查 DB 不走缓存；在 README/runbook 记录 |
| 批量 embedding 调用失败影响面变大 | 失败时把该 doc 整体标 `EMBEDDING_FAILED`，不会留 N-1 个孤儿 chunk；管理页可针对性重试 |
| 删除死代码可能误删被反射或外部依赖使用的方法 | 全部为 `private`，IDE 索引确认零引用后再删 |
| 中文转义改裸中文后编码异常 | 父 POM 已配 `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>`；本地构建 + Docker 构建分别验证 |

---

## 三、开发与协同管控

### 任务拆解

| 编号 | 任务 | 文件 | 状态 |
| --- | --- | --- | --- |
| T1 | 移除 `chat()` 方法级 `@Transactional`，在 `persistAll`/`persistAnswer` 添加细粒度事务 | `AiChatService.java` | ✅ 已完成（2026-06-30，改用 `TransactionTemplate` 编程式事务，解决 Spring 自调用代理失效问题；新增 `TransactionTemplate` bean） |
| T2 | 拆分 `syncProducts`：阶段 A DB 事务、阶段 B 单 doc 向量索引 | `KnowledgeService.java` | ✅ 已完成（2026-06-30，`syncProducts` 去大事务；`index()` 改三段：DB 事务 → 向量库 RPC → 状态事务，Milvus 失败仅标 FAILED 不回滚 DB） |
| T3 | `index()` 改批量：聚合 chunk → 一次 `vectorStore.add(documents)` | `KnowledgeService.java` | ✅ 已完成（2026-06-30，N 个 chunk 合并为一次 `vectorStore.add(List<Document>)`） |
| T4 | 商品目录引入 Caffeine 缓存 `ProductCatalogCache` | `AiChatService.java`，可能新增 `cache/ProductCatalogCache.java` | ✅ 已完成（2026-06-30，`mall-ai/pom.xml` 引入 caffeine 依赖；`AiChatService` 内部持有 Caffeine `Cache<String, List<ProductSearchItem>>`，TTL 60s；`listProductItems` 改走缓存，`loadProductItemsFromDb` 作为回源） |
| T5 | `sendChunked` 改一次性发送 | `AiChatService.java` | ✅ 已完成（2026-06-30，去掉 2 字符 + 15ms sleep 拆段，直接一次性 `sendEvent("delta", text)`） |
| T6 | 删除死代码 `buildPrompt` / `appendRecentMessages` / `saveAssistantSafely` / `isKnowledgeIntent` / `ChatContext` | `AiChatService.java` | ✅ 已完成（2026-06-30，确认零引用后删除约 90 行） |
| T7 | 将 `\uXXXX` 转义全部替换为裸中文 | `AiChatService.java` | ✅ 已完成（2026-06-30，临时 Python 脚本批量还原近 600 处，父 POM 已配 UTF-8） |
| T8 | 用 `@ConfigurationProperties` 重构 Milvus 配置 | `LazyMilvusVectorStoreConfig.java`，新增 `MilvusClientProperties.java` | ✅ 已完成（2026-06-30，新增嵌套结构 `MilvusClientProperties` 与 `MilvusClientProperties.Client`，去掉 17 个 `@Value` 注入） |
| T9 | `application.yml` 显式配 `maximum-pool-size`/`minimum-idle` | `mall-ai/src/main/resources/application.yml` | ✅ 已完成（2026-06-30，max 20 / min idle 5 / idle 5min，均支持环境变量覆盖） |
| T10 | 前端 `form.id` 类型与 `KnowledgeDoc.id` 对齐为 `string` | `AdminAiKnowledgeView.vue` | ✅ 已完成（2026-06-30，`form.id` 与 `updateKnowledgeDoc(id)` 统一为 `string`；后端 id 已 `ToStringSerializer`，vue-tsc 通过） |
| T11 | `markUnavailable` 错误消息脱敏 | `LazyMilvusVectorStoreConfig.java` | ✅ 已完成（2026-06-30，`lastFailureMessage` 固定为"向量库暂时不可用，请稍后重试"，原始异常仅 `log.warn`） |
| T12 | RAG 检索 / 工具超时参数化 | `AiProperties.java`、`AiChatService.streamChatInternal`、`application.yml` | ✅ 已完成（2026-06-30 联调追加，原 `ragFuture.get(6, SECONDS)` / `toolFuture.get(8, SECONDS)` 改为读取 `mall.ai.rag.retrieve-timeout-ms`（默认 15s）和 `tool-timeout-ms`（默认 8s），支持环境变量覆盖） |
| T13 | RAG 链路打点 | `AiChatService.retrieve` | ✅ 已完成（2026-06-30 联调追加，新增 `RAG similaritySearch returned size= elapsed=` 日志行，区分阈值过滤、status 过滤、RPC 失败三种状态） |

### 实施顺序
- **批次 A（高优 / 内部改造）**：T1、T2、T3、T5——风险低、无 API 变更。
- **批次 B（依赖引入）**：T4、T8、T9——需检查父 POM 与配置注入。
- **批次 C（清理类）**：T6、T7、T11——可读性优化。
- **批次 D（前端）**：T10——独立部署链路。

每个批次独立提交、独立验证。

### 代码规范
- 沿用现有 Spring Boot/Spring AI 习惯：`record` DTO、`@Transactional` 仅在写库方法。
- 中文字符串使用 UTF-8 裸文，禁止再写 `\uXXXX` 转义。
- 新增 bean 优先 constructor injection，禁止 `@Autowired` 字段注入。

---

## 四、测试与质量保障

### 单元测试
- `AiChatServiceTest`：
  - 模拟 DeepSeek 慢响应（5s），断言 mall_ai 数据库连接在 chat() 执行期间未被持有（通过 HikariCP `getActiveConnections()` 抽样）。
  - 模拟 `persistAll` 中 `messageMapper.insert` 抛异常，断言 `conversation` 与两条 message 全部回滚。
- `KnowledgeServiceTest`：
  - 模拟 `vectorStore.add` 对第 3 个 doc 抛异常，断言前 2 个 doc 保留 `EMBEDDING_INDEXED`，第 3 个标 `EMBEDDING_FAILED`，后续 doc 继续处理。
  - 断言 `index(docId)` 仅触发一次 `vectorStore.add(List<Document>)`，参数 size = chunk 数。
- `ProductCatalogCacheTest`：
  - 连续 100 次 `listProductItems()`，断言 `JdbcTemplate.query` 仅被调用 1 次（首次穿透）。
  - 等待 TTL + 1s 后再次调用，断言重新落库。

### 集成测试
- 启动完整 `mall-ai` + Milvus + MySQL，执行：
  - 200 个商品的 `syncProducts`，期间手动 `docker stop milvus`，确认已落库 doc 状态正确，未产生孤儿向量。
  - 并发 30 个 `streamChat` 请求（消息混合：快捷、商品意图、订单意图、RAG），确认无 `Unable to acquire JDBC Connection`。

### 专项测试
- **性能**：参考 `docs/performance-testing.md`，AI 模块单接口 P95 目标：
  - `/ai/chat`（非快捷）：< 8s（受 DeepSeek 主导，本次改造不影响）
  - `/ai/chat`（快捷）：< 100ms（原 ~500ms）
  - `/ai/admin/knowledge/docs/{id}/embedding`：< 1s/篇（原 ~1.6s/篇）
  - `/ai/admin/knowledge/docs/sync-products`（100 篇）：< 30s（原 > 2min）
- **安全**：人工触发 Milvus 故障，确认接口返回不含主机/端口/认证细节。
- **回归**：现有用例（订单查询、商品目录、RAG 命中、快捷问答）行为不变。

### 缺陷管理
- 所有 P1 任务必须配套单测，否则不合并主干。
- P2、P3 任务可仅靠手测 + 既有回归用例覆盖。

---

## 五、发布与部署

### 前置条件
- 父 POM `caffeine` 依赖（若未引入）通过审查。
- `MEMORY.md` / `runbook.md` 更新缓存 TTL、最大池大小等运行时常量。
- 测试环境完整跑过批次 A→D 的回归测试。

### 发布步骤
1. 后端按批次 A → B → C 顺序合并到 `dev`，每批次跑一次 `mvn -pl mall-ai -am test`。
2. 前端 T10 单独 PR，跑 `npm run type-check && npm run build`。
3. 合并到 `main` 后构建镜像（沿用现有 GitHub Actions 流水线，参考 `docs/github-release.md`）。
4. 灰度：先在测试环境观察 1 天对话量与索引耗时，再切生产。
5. 生产滚动重启 `mall-ai`（K8s `kubectl rollout restart deployment/mall-ai` 或 Docker Compose `docker compose up -d mall-ai`）。

### 回滚方案
- 镜像粒度回滚：保留上一个稳定 tag，发现 P0 问题 5 分钟内 `kubectl rollout undo`。
- 数据回滚：本次无 schema 变更，无需 DB 回滚；若 `syncProducts` 中途失败，管理页"重新索引"按钮覆盖即可。

### 上线后巡检
- 监控 1~2 小时：
  - HikariCP 活跃连接数（应稳定 < 5）
  - DeepSeek 调用耗时 P95、失败率
  - Milvus 调用错误率
  - `/ai/chat` QPS 与 P95
- 业务巡检：抽 5 条会话验证回答质量未退化。

---

## 变更历史

| 日期 | 版本 | 内容 | 状态 |
| --- | --- | --- | --- |
| 2026-06-30 | v0.1 | 起草文档，识别 12 处优化点并按五阶段拆解 | 等待用户确认实施范围 |
| 2026-06-30 | v0.2 | 完成批次 A（T1/T2/T3/T5）+ 顺手完成 T11；mall-ai 模块编译通过 | 待联调验证 |
| 2026-06-30 | v0.3 | 完成批次 B（T8/T9）+ 批次 C（T6/T7）+ 批次 D（T10）+ T4 Caffeine 缓存；mall-ai 主代码+测试代码、前端 vue-tsc 全部通过 | 待联调验证 |
| 2026-06-30 | v0.4 | 联调时发现 RAG 6s 硬编码超时不够、链路断点不可见。新增 T12 RAG 检索/工具超时参数化（`mall.ai.rag.retrieve-timeout-ms` 默认 15s，`tool-timeout-ms` 默认 8s）、T13 `retrieve()` 增加 `similaritySearch returned size= elapsed=` 耗时打点 | 已联调验证 |
| 2026-06-30 | v0.5 | 联调中定位到 Milvus 嵌入式 etcd 模式 crash loop（25 次重启）。`deploy/docker-compose.yml` 改造为 etcd + MinIO + Milvus 三容器 standalone 模式；因 VM 老内核 `getrandom` 系统调用兼容性问题，最终版本固化为 etcd v3.5.5 + MinIO 2023-03 + Milvus v2.3.21 | 已联调验证，RAG 链路端到端通过（命中 size=1，elapsed 524~769ms） |
