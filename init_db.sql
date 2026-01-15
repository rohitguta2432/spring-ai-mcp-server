CREATE EXTENSION IF NOT EXISTS vector;
CREATE SCHEMA IF NOT EXISTS gtw;

CREATE TABLE IF NOT EXISTS gtw.knowledge_chunks (
  id BIGSERIAL PRIMARY KEY,
  content TEXT NOT NULL,
  embedding vector(1536) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_embedding
  ON gtw.knowledge_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
