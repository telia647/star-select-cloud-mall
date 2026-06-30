# AI Service Design

## Scope

`mall-ai` adds AI customer service, private RAG knowledge base, and read-only order Agent capability to the existing mall system.

P0 scope:

- Knowledge document CRUD.
- Text chunking and vector indexing.
- RAG chat with knowledge references.
- Read-only order detail tool call.
- Conversation, message, model call, and tool call logs.

Out of scope for P0:

- File upload parsing.
- Image or voice input.
- Refund, cancel order, or other write tools.
- Human客服 handoff.
- Multi-model UI switching.

## Runtime Decisions

- Chat model: DeepSeek through Spring AI DeepSeek starter.
- Embedding model: OpenAI-compatible Spring AI embedding starter, configured by environment variables.
- Vector database: Milvus in the VM Docker Compose stack.
- API keys: environment variables only, never committed.

## Required Environment Variables

For `mall-ai` in IDEA:

```text
MALL_VM_HOST=192.168.150.106
MALL_NACOS_ADDR=192.168.150.106:8848
MALL_MYSQL_HOST=192.168.150.106
MALL_MYSQL_PORT=3306
MALL_MYSQL_USERNAME=root
MALL_MYSQL_PASSWORD=root
MALL_REDIS_HOST=192.168.150.106
MALL_REDIS_PORT=6379
MALL_JWT_SECRET=replace-with-at-least-32-byte-secret

MALL_AI_DEEPSEEK_API_KEY=<your-deepseek-key>
MALL_AI_DEEPSEEK_BASE_URL=https://api.deepseek.com
MALL_AI_DEEPSEEK_CHAT_MODEL=deepseek-chat

MALL_AI_EMBEDDING_API_KEY=<your-embedding-key>
MALL_AI_EMBEDDING_BASE_URL=<openai-compatible-embedding-base-url>
MALL_AI_EMBEDDING_MODEL=<embedding-model>

MALL_MILVUS_HOST=192.168.150.106
MALL_MILVUS_PORT=19530
```

If DeepSeek does not provide an embedding endpoint in the chosen account, keep DeepSeek for chat and point the embedding variables to another OpenAI-compatible embedding provider.

## API Summary

C-side:

```text
POST /api/ai/chat
GET  /api/ai/conversations
GET  /api/ai/conversations/{conversationId}/messages
```

Admin:

```text
GET    /api/ai/admin/knowledge/docs
POST   /api/ai/admin/knowledge/docs
GET    /api/ai/admin/knowledge/docs/{id}
PUT    /api/ai/admin/knowledge/docs/{id}
DELETE /api/ai/admin/knowledge/docs/{id}
POST   /api/ai/admin/knowledge/docs/{id}/embedding
GET    /api/ai/admin/logs/model-calls
GET    /api/ai/admin/logs/tool-calls
```

## RAG Flow

```text
User question
-> mall-ai /ai/chat
-> VectorStore similarity search TopK
-> Prompt with retrieved knowledge
-> DeepSeek chat model
-> Save conversation/message/model logs/references
-> Return answer and references
```

## Order Tool Flow

```text
User asks order question
-> mall-ai detects orderNo
-> Feign calls mall-order user endpoint with current user header
-> Save tool call log
-> Prompt includes order JSON
-> Return natural language answer and order card data
```

## Safety Rules

- `/api/ai/**` requires login.
- `/api/ai/admin/**` requires `ADMIN`.
- P0 tools are read-only.
- User order tool uses current user id and cannot read other users' orders.
- Model/tool failures are logged and returned as friendly messages.
