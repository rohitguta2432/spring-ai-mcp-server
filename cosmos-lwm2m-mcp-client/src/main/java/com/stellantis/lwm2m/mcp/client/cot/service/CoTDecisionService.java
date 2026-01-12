package com.stellantis.lwm2m.mcp.client.cot.service;

import com.stellantis.lwm2m.mcp.client.cot.CoTDecisionResult;
import com.stellantis.lwm2m.mcp.client.execption.SqlGenerationException;
import com.stellantis.lwm2m.mcp.client.model.KnowledgeChunkJson;
import com.stellantis.lwm2m.mcp.client.repository.KnowledgeChunkJsonRepository;
import com.stellantis.lwm2m.mcp.client.service.embeddings.HybridEmbeddingModel;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * CoTDecisionService is responsible for: - Embedding the incoming natural language query. -
 * Retrieving top-k most relevant schema/table chunks from the repository. - Parsing schema, table,
 * columns, and relationships from chunk content. - Deciding the final schema.table for SQL
 * generation.
 *
 * <p>If multiple schemas contain the same table name, user disambiguation is required. Otherwise,
 * the best candidate is auto-selected with its columns + relationships + full schema context.
 */
@Service
public class CoTDecisionService {

  private static final Logger log = LoggerFactory.getLogger(CoTDecisionService.class);

  private final KnowledgeChunkJsonRepository repo;
  private final HybridEmbeddingModel embeddingService;
  private final RerankerService rerankerService;

  public CoTDecisionService(
      KnowledgeChunkJsonRepository repo,
      HybridEmbeddingModel embeddingService,
      RerankerService rerankerService) {
    this.repo = repo;
    this.embeddingService = embeddingService;
    this.rerankerService = rerankerService;
  }

  public CoTDecisionResult decide(String queryVec, int k, UUID conversationId) {
    log.debug("🔍 Step 1: Received queryVec='{}', topK={}", queryVec, k);

    float[] embedding = embeddingService.embed(queryVec);
    log.debug("🔍 Step 2: Generated embedding size={}", (embedding != null ? embedding.length : 0));

    if (embedding == null || embedding.length == 0) {
      throw new SqlGenerationException("Empty embedding vector from embedding model");
    }

    List<KnowledgeChunkJson> candidates = repo.findMostRelevants(embedding, k);
    log.debug("🔍 Step 4: Repo returned {} candidates", candidates.size());

    // Rerank candidates using hybrid BM25 + Embedding scoring
    candidates = rerankerService.rerank(candidates, embedding, queryVec, 2);

    log.debug("🔍 Step 4b: Reranker returned {} candidates", candidates.size());
    List<String> fullSchemaContexts =
        candidates.stream().map(KnowledgeChunkJson::getContent).toList();

    return new CoTDecisionResult(
        false, "", List.of(), "", List.of(), List.of(), fullSchemaContexts);
  }
}
