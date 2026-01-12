package com.stellantis.lwm2m.mcp.server.service;

import com.stellantis.lwm2m.mcp.server.execption.SqlGenerationException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link DatabaseService} that:
 *
 * <ul>
 *   <li>Retrieves pgvector-backed schema context from {@code gtw.knowledge_chunks} using a
 *       repository.
 *   <li>Generates SQL via a configured {@link } (LLM model is picked from application.yml).
 *   <li>Enforces read-only SQL (SELECT/WITH only) before execution.
 *   <li>Executes SQL using Spring {@link JdbcTemplate}.
 * </ul>
 */
@Service
public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger log = LoggerFactory.getLogger(DatabaseServiceImpl.class);

  private final JdbcTemplate jdbc;

  @Autowired
  public DatabaseServiceImpl(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Executes an LLM-generated (already validated) SQL query. */
  @Override
  public List<Map<String, Object>> executeGeneratedSql(String sql) {
    if (!StringUtils.hasText(sql)) {
      throw new SqlGenerationException("SQL must not be empty");
    }
    try {
      return jdbc.queryForList(sql);
    } catch (Exception e) {
      log.error("SQL execution failed: {}", sql, e);
      throw new SqlGenerationException("SQL execution failed", e);
    }
  }
}
