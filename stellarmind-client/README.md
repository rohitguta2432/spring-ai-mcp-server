# Cosmos DAC Database MCP Server

**Chat-to-SQL with pgvector, safely.**  
A Spring Boot MCP server that turns natural-language questions into **read-only** SQL, retrieves schema context via **pgvector**, and returns results.

---

## Overview
- **Natural language → SQL** (LLM via Spring AI; provider-agnostic)
- **Retrieval-augmented** using `gtw.knowledge_chunks` (pgvector)
- **Read-only guardrails** (SELECT / WITH only)
- **JDBC execution** with structured logging

**Architecture:**  
**User → MCP Client → MCP Server (MCP Tool: `executeDataQuery`) → pgvector context → LLM → Safe SQL → DB results**

---

## Quick Start

### Prereqs
- Java 17+, PostgreSQL 14+ with `pgvector`
- OpenAI (or Bedrock) credentials

### Database (once)
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
