package com.stellantis.lwm2m.mcp.client.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stellantis.lwm2m.mcp.client.model.KnowledgeChunkJson;
import com.stellantis.lwm2m.mcp.client.repository.KnowledgeChunkJsonRepository;
import com.stellantis.lwm2m.mcp.client.service.embeddings.HybridEmbeddingModel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Loads JSON schema knowledge files from the classpath and indexes them into {@code
 * gtw.knowledge_chunks_json} with embeddings at application startup.
 *
 * <p>Files are expected under {@code resources/json/}:
 *
 * <ul>
 *   <li>json/bs_schema.json
 *   <li>json/gtw_schema.json
 *   <li>json/cross_schema_relationships.json
 * </ul>
 *
 * <p>Each table in the JSON schema is processed as a separate knowledge chunk for better retrieval
 * granularity. Schema-level metadata (name, synonyms, description) is included in each chunk.
 * Embeddings are generated via {@link HybridEmbeddingModel}, so the provider can be swapped (OpenAI
 * ↔ Bedrock) through configuration only.
 */
@Service
public class JsonSchemaLoaderWithRelationships {

  private static final Logger log =
      LoggerFactory.getLogger(JsonSchemaLoaderWithRelationships.class);

  private final KnowledgeChunkJsonRepository knowledgeChunkJsonRepository;
  private final HybridEmbeddingModel embeddingService;
  private final ObjectMapper objectMapper;

  @Value("${embedding.enabled:false}")
  private boolean embeddingEnabled;

  @Value("${json.schema.chunk-by-table:false}")
  private boolean chunkByTable;

  // Default JSON resources to load
  private static final List<String> DEFAULT_JSON_RESOURCES =
      List.of(
          "json/bs_schema.json", "json/gtw_schema.json", "json/cross_schema_relationships.json");

  public JsonSchemaLoaderWithRelationships(
      KnowledgeChunkJsonRepository knowledgeChunkJsonRepository,
      HybridEmbeddingModel embeddingService,
      ObjectMapper objectMapper) {
    this.knowledgeChunkJsonRepository = knowledgeChunkJsonRepository;
    this.embeddingService = embeddingService;
    this.objectMapper = objectMapper;
  }

  /**
   * Bootstrap entrypoint. Runs once when the application is ready. Clears existing chunks and
   * reloads JSON schema definitions.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void loadOnStartup() {
    if (!embeddingEnabled) {
      log.info(
          "JsonSchemaLoader: embeddings disabled via property 'embedding.enabled=false'. Skipping load.");
      return;
    }

    log.info(
        "JsonSchemaLoader: starting schema load. resources={}, chunkByTable={}",
        DEFAULT_JSON_RESOURCES.size(),
        chunkByTable);

    try {
      long countBefore = knowledgeChunkJsonRepository.count();
      knowledgeChunkJsonRepository.deleteAll();
      log.info("JsonSchemaLoader: cleared existing chunks. previousCount={}", countBefore);

      for (String resourcePath : DEFAULT_JSON_RESOURCES) {
        if (chunkByTable && !resourcePath.contains("cross-schema")) {
          indexResourceByTable(resourcePath);
        } else {
          indexResourceAsWhole(resourcePath);
        }
      }

      long countAfter = knowledgeChunkJsonRepository.count();
      log.info("JsonSchemaLoader: completed load. chunksIndexed={}", countAfter);

    } catch (Exception e) {
      log.error("JsonSchemaLoader: failed to load schemas", e);
    }
  }

  /**
   * Reads a JSON schema file and processes each table as a separate chunk.
   *
   * @param classpathJson path under resources (e.g. json/gtw_schema.json)
   */
  private void indexResourceByTable(String classpathJson) {
    final long t0 = System.nanoTime();
    log.info("JsonSchemaLoader: indexing resource by table path='{}'", classpathJson);

    try {
      String jsonContent = readClasspathFile(classpathJson);
      if (!StringUtils.hasText(jsonContent)) {
        log.warn("JsonSchemaLoader: empty content for '{}'; skipping.", classpathJson);
        return;
      }

      JsonNode rootNode = objectMapper.readTree(jsonContent);
      JsonNode tablesArray = rootNode.get("tables");

      if (tablesArray == null || !tablesArray.isArray()) {
        log.warn("JsonSchemaLoader: no 'tables' array found in '{}'; skipping.", classpathJson);
        return;
      }

      int tableCount = 0;

      // Extract schema-level metadata
      String schemaName =
          rootNode.path("schema_name").asText(extractSchemaNameFromPath(classpathJson));
      String schemaDescription = rootNode.path("description").asText("");

      // Extract schema synonyms
      JsonNode schemaSynonyms = rootNode.path("synonyms");
      StringBuilder schemaSynonymsStr = new StringBuilder();
      if (schemaSynonyms.isArray() && schemaSynonyms.size() > 0) {
        for (int i = 0; i < schemaSynonyms.size(); i++) {
          if (i > 0) schemaSynonymsStr.append(", ");
          schemaSynonymsStr.append(schemaSynonyms.get(i).asText());
        }
      }

      log.debug(
          "JsonSchemaLoader: schema='{}', synonyms='{}', description='{}'",
          schemaName,
          schemaSynonymsStr,
          schemaDescription);

      for (JsonNode tableNode : tablesArray) {
        String tableName = tableNode.path("name").asText("unknown");
        String tableContent =
            createTableContent(
                tableNode, schemaName, schemaDescription, schemaSynonymsStr.toString());

        if (StringUtils.hasText(tableContent)) {
          indexSingleChunk(tableContent, String.format("%s.%s", schemaName, tableName));
          tableCount++;
        }
      }

      log.info(
          "JsonSchemaLoader: processed '{}' into {} table chunks. timeMs={}",
          classpathJson,
          tableCount,
          toMs(System.nanoTime() - t0));

    } catch (IOException io) {
      log.error("JsonSchemaLoader: I/O error reading '{}'", classpathJson, io);
    } catch (Exception e) {
      log.error("JsonSchemaLoader: failed to index by table '{}'", classpathJson, e);
    }
  }

  /**
   * Reads a JSON schema file and processes it as a single chunk. Used for cross-schema
   * relationships and patterns.
   *
   * @param classpathJson path under resources (e.g. json/cross_schema_relationships.json)
   */
  private void indexResourceAsWhole(String classpathJson) {
    final long t0 = System.nanoTime();
    log.info("JsonSchemaLoader: indexing resource as whole path='{}'", classpathJson);

    try {
      String content = readClasspathFile(classpathJson);
      if (!StringUtils.hasText(content)) {
        log.warn("JsonSchemaLoader: empty content for '{}'; skipping.", classpathJson);
        return;
      }

      String schemaName = extractSchemaNameFromPath(classpathJson);
      indexSingleChunk(content, schemaName);

      log.info(
          "JsonSchemaLoader: processed '{}' as single chunk. timeMs={}",
          classpathJson,
          toMs(System.nanoTime() - t0));

    } catch (IOException io) {
      log.error("JsonSchemaLoader: I/O error reading '{}'", classpathJson, io);
    } catch (Exception e) {
      log.error("JsonSchemaLoader: failed to index as whole '{}'", classpathJson, e);
    }
  }

  /**
   * Builds chunk content for a table including schema (with synonyms and description), table (with
   * synonyms), DDL, columns, relationships (FKs, references, joins, cross-schema), and sample
   * queries.
   */
  private String createTableContent(
      JsonNode tableNode, String schemaName, String schemaDescription, String schemaSynonyms) {

    StringBuilder content = new StringBuilder();

    String tableName = tableNode.path("name").asText("");
    String description = tableNode.path("description").asText("");
    String ddl = tableNode.path("schema").asText("");

    // Schema Context with synonyms and description
    content.append("Schema: ").append(schemaName);
    if (!schemaSynonyms.isEmpty()) {
      content.append(" (synonyms: ").append(schemaSynonyms).append(")");
    }
    content.append("\n");

    if (!schemaDescription.isEmpty()) {
      content.append("Schema Description: ").append(schemaDescription).append("\n");
    }

    // Table information WITH SYNONYMS
    content.append("Table: ").append(tableName);

    // Extract and append table synonyms
    JsonNode tableSynonyms = tableNode.path("synonyms");
    if (tableSynonyms.isArray() && tableSynonyms.size() > 0) {
      content.append(" (synonyms: ");
      for (int i = 0; i < tableSynonyms.size(); i++) {
        if (i > 0) content.append(", ");
        content.append(tableSynonyms.get(i).asText());
      }
      content.append(")");
    }
    content.append("\n");

    content.append("Description: ").append(description).append("\n");
    content.append("DDL: ").append(ddl).append("\n\n");

    // Columns
    JsonNode columnsArray = tableNode.path("columns");
    if (columnsArray.isArray()) {
      content.append("Columns:\n");
      for (JsonNode col : columnsArray) {
        content
            .append("- ")
            .append(col.path("name").asText(""))
            .append(": ")
            .append(col.path("description").asText(""));
        JsonNode synonyms = col.path("synonyms");
        if (synonyms.isArray() && synonyms.size() > 0) {
          content.append(" (synonyms: ");
          for (int i = 0; i < synonyms.size(); i++) {
            if (i > 0) content.append(", ");
            content.append(synonyms.get(i).asText());
          }
          content.append(")");
        }
        content.append("\n");
      }
      content.append("\n");
    }

    // Relationships
    JsonNode relationships = tableNode.path("relationships");
    if (!relationships.isMissingNode()) {
      content.append("Relationships:\n");

      JsonNode fks = relationships.path("foreign_keys");
      if (fks.isArray()) {
        for (JsonNode fk : fks) {
          content
              .append("- FK: ")
              .append(fk.path("column").asText(""))
              .append(" → ")
              .append(fk.path("references_table").asText(""))
              .append("(")
              .append(fk.path("references_column").asText(""))
              .append(")")
              .append(" [")
              .append(fk.path("relationship_type").asText(""))
              .append("]\n");
        }
      }

      JsonNode refs = relationships.path("referenced_by");
      if (refs.isArray()) {
        for (JsonNode ref : refs) {
          content
              .append("- Referenced by: ")
              .append(ref.path("table").asText(""))
              .append(".")
              .append(ref.path("column").asText(""))
              .append(" [")
              .append(ref.path("relationship_type").asText(""))
              .append("]\n");
        }
      }

      JsonNode joins = relationships.path("common_joins");
      if (joins.isArray()) {
        for (JsonNode join : joins) {
          content.append("- Common join: ").append(join.asText("")).append("\n");
        }
      }

      JsonNode crossSchema = relationships.path("cross_schema_relationships");
      if (crossSchema.isArray()) {
        for (JsonNode cs : crossSchema) {
          content
              .append("- Cross-schema relation: ")
              .append(cs.path("table").asText(""))
              .append(" ON ")
              .append(cs.path("join_condition").asText(""))
              .append("\n");
        }
      }
      content.append("\n");
    }

    // Sample Queries
    JsonNode sampleQueries = tableNode.path("sample_queries");
    if (sampleQueries.isArray()) {
      content.append("Sample Queries:\n");
      for (JsonNode sq : sampleQueries) {
        content.append("Q: ").append(sq.path("user_input").asText("")).append("\n");
        content.append("SQL: ").append(sq.path("query").asText("")).append("\n");
      }
    }

    return content.toString().trim();
  }

  /** Generates embedding and persists a knowledge chunk. */
  private void indexSingleChunk(String content, String identifier) {
    try {
      float[] embedding = embeddingService.embed(content);
      if (embedding == null || embedding.length == 0) {
        log.warn("JsonSchemaLoader: empty embedding for '{}'; skipping.", identifier);
        return;
      }

      KnowledgeChunkJson chunk = new KnowledgeChunkJson();
      chunk.setContent(content);
      chunk.setEmbedding(embedding);
      chunk.setCreatedAt(LocalDateTime.now());

      knowledgeChunkJsonRepository.save(chunk);

      log.debug(
          "JsonSchemaLoader: stored chunk for '{}'. contentChars={} dims={}",
          identifier,
          content.length(),
          embedding.length);

    } catch (Exception e) {
      log.error("JsonSchemaLoader: failed to index chunk '{}'", identifier, e);
    }
  }

  /** Extracts schema name from file path (e.g., json/gtw_schema.json → gtw). */
  private String extractSchemaNameFromPath(String classpathJson) {
    String filename = classpathJson.substring(classpathJson.lastIndexOf('/') + 1);
    String basename = filename.substring(0, filename.lastIndexOf('.'));

    // normalize: remove "_schema" or "-schema"
    if (basename.endsWith("_schema")) {
      basename = basename.substring(0, basename.length() - "_schema".length());
    } else if (basename.endsWith("-schema")) {
      basename = basename.substring(0, basename.length() - "-schema".length());
    }

    return basename; // will now be "bs" or "gtw"
  }

  /** Reads a classpath resource fully as UTF-8 text. */
  private String readClasspathFile(String classpathPath) throws IOException {
    ClassPathResource resource = new ClassPathResource(classpathPath);
    if (!resource.exists()) {
      throw new IOException("Classpath resource not found: " + classpathPath);
    }
    try (InputStream in = resource.getInputStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static long toMs(long nanos) {
    return nanos / 1_000_000L;
  }
}
