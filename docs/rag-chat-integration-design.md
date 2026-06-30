# RAG 与智能客服联动调优设计文档

> 范围：智能客服对话链路中 RAG 检索的可观测性、命中率与降级策略。
> 参考：`docs/开发流程.md` 五阶段流程，`docs/rag-knowledge-base-design.md` 模块基线。
> 触发：单条索引接口成功落库，但对话中知识库内容未被引用，链路断点不明。

---

## 一、需求定义

### 问题描述
- 单条索引接口返回成功，MySQL `ai_knowledge_chunk` 和 Milvus 都已落数据
- 智能客服对话时，用户问知识库内有的内容（如"暗号是什么"），LLM 没有引用知识库信息
- 即"索引成功 ≠ 检索命中 ≠ 注入 Prompt"，链路断在哪一段未知

### 业务目标
让用户在智能客服对话中，能稳定地通过知识库内容得到精准回答。

### 核心需求
1. **可观测**：能看到一次对话里 RAG 是否触发、检索到几条、相似度多少、是否被注入 Prompt
2. **可命中**：知识库存在相关文档时，检索结果非空且相关
3. **可降级**：检索失败/超时不阻塞对话，但要记录原因

### 验收标准
- 在管理后台录入"暗号是星选666"文档并索引成功后
- 用户问"暗号是什么" → 后端日志能看到 `RAG hit N docs`，且 LLM 回答含"星选666"
- 后端日志能区分四种状态：未触发 / 检索失败 / 检索为空 / 命中 N 条
- 对话端在 Milvus / Embedding API 故障时仍能完成对话（降级而非报错）

### 异常场景

| 场景 | 期望 |
|---|---|
| Embedding API 慢 | 6s 超时跳过，对话不卡 |
| Milvus 空集合 | 检索返回空，对话正常进行 |
| 相似度阈值过滤掉全部结果 | 日志记录"召回 N 条但被阈值过滤" |
| status 过滤把启用文档过滤掉 | 排查 metadata.status 值是否正确写入 |
| Milvus 启动中 | 提示"正在启动中，请稍后"，不进入 30s 冷却 |

---

## 二、架构设计

### 当前链路

```
用户消息
  └─ AiChatService.streamInternal()
      └─ CompletableFuture.supplyAsync(retrieve, executor)   [并行启动]
      └─ retrieve():
          ├─ SearchRequest{ topK=5, threshold=0.65 }
          ├─ vectorStore.similaritySearch(req)
          │   └─ Milvus client: embedding 查询向量 → 向量相似度搜索
          └─ filter: metadata.status == ENABLED(1)
      └─ awaitDocs(6 秒超时)
      └─ buildPrompt(question, history, documents, toolResult)
      └─ DeepSeek 流式输出
```

### 可疑环节诊断点

| 环节 | 可疑点 | 排查方式 |
|---|---|---|
| ① supplyAsync 超时 | 6s 内 embedding+Milvus 没完成 | 加日志：`RAG started` / `RAG returned in Xms` |
| ② similaritySearch 返回 | 调用是否抛错？返回多少条？分数多少？ | 日志输出 raw 结果条数和分数 |
| ③ threshold 过滤 | 0.65 对中文短文档可能过高 | 暂时降为 0.3 或先输出 raw 分数 |
| ④ status 过滤 | metadata 里 status 类型不匹配（Integer vs Long） | 日志输出每条 doc 的 metadata.status 实际类型和值 |
| ⑤ buildPrompt | documents 拼进去但 LLM 没遵循？ | 看 prompt 完整内容 |

### 设计决策候选

#### A. 增加可观测性（必做）

`retrieve()` 增加 INFO 日志，覆盖整条链路：
- **start**：question 长度、topK、threshold
- **raw**：原始召回条数 + 前 3 条的 score + status 类型与值
- **after-filter**：过滤后条数
- **elapsed**：总耗时 ms

#### B. 阈值与过滤策略调整

- 当前 threshold=0.65 偏高，对短文档（"暗号是星选666"才 6 个字符）embedding 后相似度通常 < 0.5
- 候选方案：
  - **方案 1**：降阈值到 0.3（可能引入噪声）
  - **方案 2**：阈值降到 0.3 但 topK 保持 5，让大模型自己判断
  - **方案 3**：去掉阈值，只看 topK（最常用的做法）
  - **方案 4**：动态阈值（短查询用低阈值，长查询用高阈值）

**推荐方案 3**：去阈值，由 topK + LLM 自身判断决定。先验证 RAG 是否真的工作，再考虑加阈值。

#### C. status 过滤简化

- 当前 Java 侧用 `Integer.valueOf(1).equals(toInteger(metadata.get("status")))` 过滤
- Milvus metadata 反序列化可能是 Long / Integer / String，类型不匹配会被过滤掉
- 候选：写入时把 status 存为字符串 `"1"/"0"`，过滤时也比字符串

#### D. Prompt 注入强化

- 当前 `buildPrompt` 里 RAG 片段没强调"如果片段与问题相关，必须使用片段内容"
- 候选：在 systemPrompt 或 buildPrompt 加一句强引导

### 风险与对策

| 风险 | 对策 |
|---|---|
| 加日志后泄漏用户问题到日志 | 日志只输出问题长度和分数，不打全文（或仅 DEBUG 级别） |
| 去掉阈值引入噪声 | LLM systemPrompt 已要求"没有依据时明确说明，不要编造" |
| 改阈值需重启服务 | 通过 `mall.ai.rag.similarity-threshold` 配置项管理 |
| Milvus 启动期间冷却 30s | 已修：识别 "Proxy is not ready" 关键字不进入冷却 |

---

## 三、开发与实现

### 任务清单

| # | 任务 | 关键文件 | 状态 |
|---|------|---------|------|
| 1 | `retrieve()` 加 INFO 日志（start/raw/filter/elapsed） | `service/AiChatService.java` | ✅ |
| 2 | 降低 similarity-threshold 默认值（0.65 → 0.3） | `application.yml` | ✅ |
| 3 | 日志输出 metadata.status 类型与值，便于排查过滤 | `service/AiChatService.java` | ✅ |
| 4 | `buildPromptFromParts` 强化 RAG 引导语 | `service/AiChatService.java` | ✅ |
| 5 | 测试链路：录文档 → 索引 → 提问 → 看日志 → 看回答 | 手工验证 | ⏳ 待用户跑 |

### 实现细节

**1. retrieve() 全链路日志**

`AiChatService.retrieve()` 现在输出 4 类日志：
- `RAG start questionLen=X topK=Y threshold=Z` — 入参
- `RAG raw[i] score=... statusType=... statusValue=... title=...` — 前 3 条原始结果（含 status 字段类型，用于排查过滤问题）
- `RAG hit raw=N filtered=M elapsed=Xms` — 总览
- `RAG retrieve failed elapsed=Xms: <错误信息>` — 失败原因

**2. 阈值调整**

`application.yml`：`similarity-threshold` 默认 0.3（原 0.65）。可通过 `MALL_AI_RAG_SIMILARITY_THRESHOLD` 环境变量覆盖。

**3. Prompt 强化**

`buildPromptFromParts` 在拼入知识库片段后追加：
> 以上知识库片段如与用户问题相关，必须直接使用其中信息回答，不要忽略也不要改写为模糊表达。

仅在 `documents.isEmpty() == false` 时追加，避免空检索误导。

---

## 四、测试与质量保障

### 测试计划

| # | 场景 | 预期 | 状态 |
|---|------|------|------|
| T1 | 知识库录入"暗号是星选666"，问"暗号是什么" | 日志 `RAG hit raw=N filtered≥1`，回答含"星选666" | ⏳ 待用户跑 |
| T2 | 知识库空，提问普通问题 | 日志 `RAG hit raw=0`，对话正常完成 | ⏳ |
| T3 | Milvus 关停，提问 | 日志 `RAG retrieve failed`，对话仍能完成 | ⏳ |
| T4 | Milvus 启动中（30s 内）提问 | 提示启动中，对话降级，不进 30s 冷却 | ✅（前次已修） |
| T5 | 知识库多篇文档，问其中一个主题 | topK 内能召回正确文档，回答正确引用 | ⏳ |

### 验证步骤（用户操作）

1. 重启 `mall-ai` 服务（生效新代码 + 0.3 阈值）
2. 管理后台 → 知识库 → 确认"暗号是星选666"文档状态=已索引
3. 智能客服对话框问"暗号是什么"
4. 后端日志应看到：
   ```
   RAG start questionLen=6 topK=5 threshold=0.3
   RAG raw[0] score=0.XX statusType=Long(或Integer) statusValue=1 title=...
   RAG hit raw=N filtered≥1 elapsed=XXXms
   ```
5. 前端回答应包含"星选666"

### 故障排查矩阵

| 现象 | 检查 | 处置 |
|---|---|---|
| `raw=0` | embedding 模型问题，或问题与知识库语义不匹配 | 换更相关的问题；或检查 embedding API |
| `raw>0, filtered=0` | status 类型不匹配 | 看日志 `statusType`，调整 `toInteger` 或写入侧统一类型 |
| `RAG retrieve failed` | Milvus 不可用 | 看 `docker logs mall-milvus` |
| `RAG hit>0 但回答没引用` | prompt 强化不够 / LLM 不遵循 | 调整 systemPrompt 或更换 LLM |
| `elapsed > 5000ms` | embedding API 慢 | 换国内服务或加缓存 |

---

## 五、发布与部署

### 配置项

- `mall.ai.rag.top-k` (默认 5)
- `mall.ai.rag.similarity-threshold` (默认 0.65，调试期临时改 0)

### 回滚

代码 revert，配置回到 0.65 阈值。

### 监控

- 日志 grep `RAG hit \d+` 统计命中率
- 日志 grep `RAG retrieve failed` 统计失败率

---

## 附：变更历史

| 日期 | 变更 |
|---|---|
| 2026-06-30 | 文档初版，完成需求与架构设计，待用户确认方向后进入实现 |
| 2026-06-30 | 完成第三阶段实现（日志、阈值、prompt 强化），第四阶段待用户跑通验证 |

---

## 待用户验证

按"四、测试与质量保障 → 验证步骤"操作。把后端日志贴出来即可判断 RAG 是否打通；如还不命中，按"故障排查矩阵"定位。
