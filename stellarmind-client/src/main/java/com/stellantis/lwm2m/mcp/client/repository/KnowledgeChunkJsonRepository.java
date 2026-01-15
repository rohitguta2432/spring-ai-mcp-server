package com.stellantis.lwm2m.mcp.client.repository;

import com.stellantis.lwm2m.mcp.client.model.KnowledgeChunk;
import com.stellantis.lwm2m.mcp.client.model.KnowledgeChunkJson;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeChunkJsonRepository extends JpaRepository<KnowledgeChunkJson, Long> {

  // Vector similarity search using pgvector <-> operator
  @Query(
      value =
          "SELECT * FROM gtw.knowledge_chunks "
              + "ORDER BY embedding <-> CAST(:queryVec AS vector) "
              + "LIMIT :k",
      nativeQuery = true)
  List<KnowledgeChunk> findMostRelevant(@Param("queryVec") String queryVec, @Param("k") int k);

  @Query(
      value =
          "SELECT * FROM gtw.knowledge_chunks_json "
              + "ORDER BY embedding <-> CAST(:queryVec AS vector) "
              + "LIMIT :k",
      nativeQuery = true)
  List<KnowledgeChunkJson> findMostRelevants(@Param("queryVec") String queryVec, @Param("k") int k);

  @Query(
      value =
          """
        SELECT *
        FROM gtw.knowledge_chunks_json
        ORDER BY embedding <-> CAST(:embedding AS vector)
        LIMIT :topK
        """,
      nativeQuery = true)
  List<KnowledgeChunkJson> findMostRelevants(
      @Param("embedding") float[] embedding, @Param("topK") int topK);

  /**
   * Hybrid BM25 + Embedding reranking query - Combines lexical (BM25) and semantic (pgvector)
   * relevance - Reranks only the given candidate IDs
   */
  @Query(
      value =
          """
                  WITH params AS (
                      SELECT
                          CAST(:queryVec AS vector) AS qv,
                          plainto_tsquery('english', :queryText) AS tsq
                  )
                  SELECT
                      kc.*,  -- all mapped entity columns
                      (1 - (kc.embedding <=> (SELECT qv FROM params))) AS cosine_sim,
                      ts_rank_cd(to_tsvector('english', kc.content), (SELECT tsq FROM params)) AS bm25_score,
                      (0.6 * (1 - (kc.embedding <=> (SELECT qv FROM params))) +
                       0.4 * ts_rank_cd(to_tsvector('english', kc.content), (SELECT tsq FROM params))) AS hybrid_score
                  FROM gtw.knowledge_chunks_json kc
                  WHERE kc.id IN (:ids)
                  ORDER BY hybrid_score DESC
                  LIMIT :topN
                  """,
      nativeQuery = true)
  List<KnowledgeChunkJson> findHybridRanked(
      @Param("queryVec") String queryVec,
      @Param("queryText") String queryText,
      @Param("ids") List<Long> ids,
      @Param("topN") int topN);
}
