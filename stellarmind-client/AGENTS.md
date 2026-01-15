# MCP Agents & Tooling

This repo exposes a Model Context Protocol (MCP) tool backed by a Spring Boot service to answer data questions with **safe, read-only SQL**.

## Roles & Flow
- **User** → asks a natural-language question.
- **LLM (ChatClient)** → may call tools.
- **Tool (`executeDataQuery`)** → routes to service that:
    1) Retrieves schema context via pgvector
    2) Generates SQL with the LLM
    3) Enforces read-only rules
    4) Executes via JDBC and returns rows

## Current Tool (Agent Capability)
### `executeDataQuery`
- **Class:** `com.stellantis.lwm2m.mcp.server.tool.DacDbTool`
- **Method:** `String executeDataQuery(String userQuery)`
- **Description:** Convert NL → SQL, validate as read-only, execute, return results.

## Guardrails (Server-Side)
- Only **SELECT** or **WITH…SELECT** queries allowed.
- Rejects DML/DDL and admin ops: `INSERT`, `UPDATE`, `DELETE`, `DROP`, `ALTER`, `CREATE`, `TRUNCATE`, `VACUUM`, `ANALYZE`, `GRANT`, `REVOKE`, `CALL`, `DO`, `COPY`, `LISTEN/NOTIFY`, `SET`, `EXPLAIN`.
- No multiple statements; semicolons stripped.

## Retrieval (pgvector)
- Table: `gtw.knowledge_chunks(content TEXT, embedding VECTOR(1536), created_at TIMESTAMPTZ)`
- Similarity: `ORDER BY embedding <-> CAST(:queryVec AS vector) LIMIT :k`
- Embeddings are provider-agnostic via an `EmbeddingService` abstraction.

## Configuration (Models are provider-agnostic)
Models are selected in `application.yml` via Spring AI (OpenAI today; Bedrock later) — no code changes required.

**Example (OpenAI):**
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat.options.model: gpt-4.1
      embedding.options.model: text-embedding-3-large
