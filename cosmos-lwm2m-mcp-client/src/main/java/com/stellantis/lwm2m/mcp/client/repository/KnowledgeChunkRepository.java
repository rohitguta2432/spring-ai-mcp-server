package com.stellantis.lwm2m.mcp.client.repository;

import com.stellantis.lwm2m.mcp.client.model.KnowledgeChunk;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

  // Vector similarity search using pgvector <-> operator
  @Query(
      value =
          "SELECT * FROM gtw.knowledge_chunks "
              + "ORDER BY embedding <-> CAST(:queryVec AS vector) "
              + "LIMIT :k",
      nativeQuery = true)
  List<KnowledgeChunk> findMostRelevant(@Param("queryVec") String queryVec, @Param("k") int k);
}
