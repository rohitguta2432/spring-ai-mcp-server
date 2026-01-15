# StellarMIND

**Chat-to-SQL with pgvector.**  
A Spring Boot MCP server (StellarMIND) that turns natural-language questions into **read-only SQL**, retrieves schema context via **pgvector**, and returns results.

---

## Overview
- **Natural language → SQL** (LLM via Spring AI; provider-agnostic)
- **Retrieval-augmented** using `gtw.knowledge_chunks` (pgvector)
- **Supports both static embeddings & dynamic schema retrieval**
- **Read-only queries only** (`SELECT`, `WITH`)
- **JDBC execution** with structured logging

**Architecture Flow:**  
**User → MCP Client → MCP Server (MCP Tool: `executeDataQuery`) → pgvector context → LLM → Safe SQL → DB results**


---

## Prerequisites
- JDK **17+**
- PostgreSQL **14+** with `pgvector` extension installed
- OpenAI (or Bedrock) credentials (for embeddings + LLM)
- Maven 3.8+

---

## Database Setup (Run Once)
```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE SCHEMA IF NOT EXISTS gtw;

CREATE TABLE IF NOT EXISTS gtw.knowledge_chunks (
  id BIGSERIAL PRIMARY KEY,
  content TEXT NOT NULL,
  embedding vector(1536) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);

-- Optional for large corpora:
CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_embedding
  ON gtw.knowledge_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
# spring-ai-mcp-server
