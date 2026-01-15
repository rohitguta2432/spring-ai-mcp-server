package com.stellantis.lwm2m.mcp.client.cot.service;

import com.stellantis.lwm2m.mcp.client.model.KnowledgeChunkJson;
import com.stellantis.lwm2m.mcp.client.repository.KnowledgeChunkJsonRepository;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for performing post-retrieval reranking using a hybrid BM25 + Embedding
 * scoring approach directly in PostgreSQL.
 */
@Service
public class RerankerService {

  private static final Logger log = LoggerFactory.getLogger(RerankerService.class);

  private final KnowledgeChunkJsonRepository repo;

  public RerankerService(KnowledgeChunkJsonRepository repo) {
    this.repo = repo;
  }

  public List<KnowledgeChunkJson> rerank(
      List<KnowledgeChunkJson> candidates, float[] queryVec, String queryText, int topN) {

    log.debug("RerankerService: received {} candidates for reranking", candidates.size());

    List<Long> ids =
        candidates.stream().map(KnowledgeChunkJson::getId).collect(Collectors.toList());

    if (ids.isEmpty()) {
      log.warn("RerankerService: no candidate IDs available, returning empty list");
      return List.of();
    }

    log.debug("RerankerService: reranking {} candidates for query '{}'", ids.size(), queryText);

    String queryVecStr = Arrays.toString(queryVec).replace('(', '[').replace(')', ']');

    // Correct way to log first few vector elements
    String firstDims =
        IntStream.range(0, Math.min(queryVec.length, 5))
            .mapToObj(i -> queryVec[i])
            .map(String::valueOf)
            .collect(Collectors.joining(", "));
    log.trace("RerankerService: query vector (first few dims)=[{}]", firstDims);

    List<KnowledgeChunkJson> results = repo.findHybridRanked(queryVecStr, queryText, ids, topN);
    log.info("RerankerService: reranking complete, returning {} results", results.size());
    return results;
  }
}
