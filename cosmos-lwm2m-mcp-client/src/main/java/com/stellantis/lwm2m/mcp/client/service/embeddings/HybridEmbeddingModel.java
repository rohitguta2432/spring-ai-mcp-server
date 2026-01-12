package com.stellantis.lwm2m.mcp.client.service.embeddings;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * {@code HybridEmbeddingModel} provides a fault-tolerant embedding service that combines two
 * underlying {@link EmbeddingModel} implementations:
 *
 * <ul>
 *   <li><b>OpenAI</b> – typically used for general-purpose, high-quality embeddings.
 *   <li><b>Bedrock Titan</b> – AWS Bedrock-based embeddings for enterprise environments.
 * </ul>
 *
 * <p>The hybrid model is configured with a <i>primary</i> provider ("OPENAI" or "BEDROCK"). At
 * runtime, all embedding requests are first delegated to the primary provider. If the primary call
 * fails (e.g., network error, service unavailability), the request will <b>automatically fall
 * back</b> to the secondary provider.
 *
 * <p>This ensures higher reliability when embedding vectors are required in mission-critical
 * pipelines such as RAG (Retrieval Augmented Generation), semantic search, or knowledge base
 * indexing.
 *
 * <h3>Example Usage</h3>
 *
 * <pre>{@code
 * EmbeddingModel openaiModel = ...
 * EmbeddingModel bedrockModel = ...
 *
 * HybridEmbeddingModel hybrid = new HybridEmbeddingModel(openaiModel, bedrockModel, "OPENAI");
 *
 * float[] vector = hybrid.embed("Hello world");
 * }</pre>
 *
 * @author
 */
public class HybridEmbeddingModel implements EmbeddingModel {

  private static final Logger log = LoggerFactory.getLogger(HybridEmbeddingModel.class);

  private final EmbeddingModel openai;
  private final EmbeddingModel bedrock;
  private final String primary;

  /**
   * Creates a new {@code HybridEmbeddingModel}.
   *
   * @param openai the OpenAI embedding model implementation
   * @param bedrock the Bedrock Titan embedding model implementation
   * @param primary the preferred provider ("OPENAI" or "BEDROCK")
   */
  public HybridEmbeddingModel(EmbeddingModel openai, EmbeddingModel bedrock, String primary) {
    this.openai = openai;
    this.bedrock = bedrock;
    this.primary = primary;
    log.info("HybridEmbeddingModel initialized with primary provider: {}", primary);
  }

  private EmbeddingModel getPrimary() {
    return "openai".equalsIgnoreCase(primary) ? openai : bedrock;
  }

  private EmbeddingModel getSecondary() {
    return "openai".equalsIgnoreCase(primary) ? bedrock : openai;
  }

  /**
   * Executes an embedding request using the primary provider, with automatic fallback to the
   * secondary provider in case of failure.
   *
   * @param request the embedding request
   * @return the embedding response from either the primary or fallback provider
   */
  @Override
  public EmbeddingResponse call(EmbeddingRequest request) {
    try {
      log.debug("Embedding request using primary provider: {}", primary);
      return getPrimary().call(request);
    } catch (Exception ex) {
      log.warn(
          "Primary provider {} failed, falling back to secondary. Error: {}",
          primary,
          ex.getMessage());
      return getSecondary().call(request);
    }
  }

  /**
   * Embeds a single text string.
   *
   * @param text the text to embed
   * @return the embedding vector
   */
  @Override
  public float[] embed(String text) {
    try {
      log.debug("Embedding text with primary provider: {}", primary);
      return getPrimary().embed(text);
    } catch (Exception ex) {
      log.warn(
          "Primary provider {} failed, retrying with secondary. Error: {}",
          primary,
          ex.getMessage());
      return getSecondary().embed(text);
    }
  }

  /**
   * Embeds a single {@link Document}.
   *
   * @param document the document to embed
   * @return the embedding vector
   */
  @Override
  public float[] embed(Document document) {
    try {
      log.debug("Embedding document with primary provider: {}", primary);
      return getPrimary().embed(document);
    } catch (Exception ex) {
      log.warn(
          "Primary provider {} failed, retrying with secondary. Error: {}",
          primary,
          ex.getMessage());
      return getSecondary().embed(document);
    }
  }

  /**
   * Generates embeddings for a list of input texts.
   *
   * @param texts the list of texts
   * @return a list of embedding vectors
   */
  @Override
  public EmbeddingResponse embedForResponse(List<String> texts) {
    try {
      log.debug("Embedding batch with primary provider: {}", primary);
      return getPrimary().embedForResponse(texts);
    } catch (Exception ex) {
      log.warn(
          "Primary provider {} failed, retrying with secondary. Error: {}",
          primary,
          ex.getMessage());
      return getSecondary().embedForResponse(texts);
    }
  }

  /**
   * Returns the embedding dimensions from the primary provider.
   *
   * @return the dimensionality of embedding vectors
   */
  @Override
  public int dimensions() {
    int dims = getPrimary().dimensions();
    log.debug("Embedding dimensions from primary provider {}: {}", primary, dims);
    return dims;
  }
}
