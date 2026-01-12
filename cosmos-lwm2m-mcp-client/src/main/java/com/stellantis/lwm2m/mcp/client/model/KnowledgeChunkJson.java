package com.stellantis.lwm2m.mcp.client.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;

/**
 * gtw.knowledge_chunks - content: TEXT (was varchar(255) -> caused DataIntegrityViolation) -
 * embedding: pgvector(1536) stored via FloatArrayConverter
 */
@Entity
@Table(name = "knowledge_chunks_json", schema = "gtw")
public class KnowledgeChunkJson {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Chunk text content (can be long) */
  @Column(name = "content", columnDefinition = "text", nullable = false)
  private String content;

  @Column(columnDefinition = "vector(1536)", nullable = false)
  @ColumnTransformer(write = "?::vector", read = "embedding::text")
  @Convert(converter = PgVectorFloatArrayConverter.class)
  private float[] embedding;

  /** Convenience alias for libraries expecting getText() */
  public String getText() {
    return content;
  }

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Override
  public String toString() {
    return "KnowledgeChunk{id="
        + id
        + ", content='"
        + (content == null
            ? "null"
            : (content.length() > 80 ? content.substring(0, 80) + "..." : content))
        + "', embedding="
        + (embedding == null ? "null" : ("Float[" + embedding.length + "]"))
        + ", createdAt="
        + createdAt
        + "}";
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public float[] getEmbedding() {
    return embedding;
  }

  public void setEmbedding(float[] embedding) {
    this.embedding = embedding;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
