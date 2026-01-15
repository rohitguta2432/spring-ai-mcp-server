package com.stellantis.lwm2m.mcp.client.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stellantis.lwm2m.mcp.client.cot.dto.SchemaColumns;
import java.util.ArrayList;
import java.util.List;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * Repository for Text2SQL schema metadata stored in Redis.
 *
 * <p>Key format: text2sql:schema:{schemaName}:{tableName}
 *
 * <p>Value format (JSON): { "columns": ["col1", "col2", ...] }
 */
@Repository
public class RedisSchemaMetadataRepository {

  private static final Logger log = LoggerFactory.getLogger(RedisSchemaMetadataRepository.class);

  /** Redis key pattern: text2sql:schema:{schema}:{table} */
  private static final String KEY_PATTERN = "text2sql:schema:%s:%s";

  private final RedissonClient redissonClient;
  private final ObjectMapper objectMapper;

  public RedisSchemaMetadataRepository(RedissonClient redissonClient, ObjectMapper objectMapper) {
    this.redissonClient = redissonClient;
    this.objectMapper = objectMapper;
  }

  private String buildKey(String schemaName, String tableName) {
    return KEY_PATTERN.formatted(schemaName, tableName);
  }

  /**
   * Load column metadata for a given table from Redis.
   *
   * @param schemaName schema name, e.g. "gtw"
   * @param tableName table name, e.g. "device_operation_reference"
   * @return SchemaColumns or null if key is missing or deserialization fails
   */
  public SchemaColumns getSchemaColumns(String schemaName, String tableName) {
    String key = buildKey(schemaName, tableName);
    try {
      RBucket<String> bucket = redissonClient.getBucket(key);
      String value = bucket.get();
      if (value == null) {
        log.debug("No schema metadata found in Redis for key={}", key);
        return null;
      }
      return objectMapper.readValue(value, SchemaColumns.class);
    } catch (Exception e) {
      log.error(
          "Failed to read schema metadata from Redis for schema={}, table={}: {}",
          schemaName,
          tableName,
          e.getMessage(),
          e);
      return null;
    }
  }

  /**
   * Save / overwrite the full column list for a table into Redis.
   *
   * <p>Typical usage: schema-sync job reading information_schema.columns.
   *
   * @param schemaName schema name, e.g. "gtw"
   * @param tableName table name, e.g. "device_operation_reference"
   * @param columns list of column names (case-sensitive as in DB)
   */
  public void saveSchemaColumns(String schemaName, String tableName, List<String> columns) {
    String key = buildKey(schemaName, tableName);
    try {
      List<String> safeColumns = (columns != null) ? new ArrayList<>(columns) : new ArrayList<>();

      SchemaColumns schemaColumns = new SchemaColumns(safeColumns);
      String json = objectMapper.writeValueAsString(schemaColumns);

      RBucket<String> bucket = redissonClient.getBucket(key);
      bucket.set(json);

      log.info(
          "Saved schema metadata for key={} with {} columns", key, schemaColumns.columns().size());
    } catch (Exception e) {
      log.error(
          "Failed to save schema metadata to Redis for schema={}, table={}: {}",
          schemaName,
          tableName,
          e.getMessage(),
          e);
    }
  }

  /**
   * Ensure that a given column exists for the table in Redis.
   *
   * <p>Behavior: - If the table has no entry in Redis, create it with a single column. - If the
   * table exists and the column is missing, add the column and save. - If the column already
   * exists, this is a no-op.
   *
   * @param schemaName schema name, e.g. "gtw"
   * @param tableName table name, e.g. "device_operation_reference"
   * @param columnName column to ensure is present
   */
  public void addColumnIfMissing(String schemaName, String tableName, String columnName) {
    String key = buildKey(schemaName, tableName);
    try {
      RBucket<String> bucket = redissonClient.getBucket(key);
      String value = bucket.get();

      SchemaColumns schemaColumns;

      if (value == null) {
        // No metadata yet: create with this single column
        List<String> cols = new ArrayList<>();
        cols.add(columnName);
        schemaColumns = new SchemaColumns(cols);
        log.info("Creating new schema metadata for key={} with initial column={}", key, columnName);
      } else {
        schemaColumns = objectMapper.readValue(value, SchemaColumns.class);

        if (schemaColumns.hasColumn(columnName)) {
          log.debug("Column '{}' already present for key={}, no update needed", columnName, key);
          return;
        }

        // Add column and persist
        schemaColumns.columns().add(columnName);
        log.info("Adding new column='{}' to schema metadata for key={}", columnName, key);
      }

      String updatedJson = objectMapper.writeValueAsString(schemaColumns);
      bucket.set(updatedJson);

    } catch (Exception e) {
      log.error(
          "Failed to add column '{}' for schema={}, table={} in Redis: {}",
          columnName,
          schemaName,
          tableName,
          e.getMessage(),
          e);
    }
  }
}
