package com.stellantis.lwm2m.mcp.client.semantics;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SchemaServiceNew {
  @Autowired private JdbcTemplate jdbcTemplate;

  /** Extracts and enriches schema information for the given list of schemas. */
  public SchemaMCPNew getStructuredSchema(List<String> schemas) {
    List<TableInfo> tables = new ArrayList<>();

    // 1) Fetch tables
    String tableSql =
        "SELECT table_schema, table_name FROM information_schema.tables "
            + "WHERE table_schema = ? AND table_type = 'BASE TABLE'";

    for (String schema : schemas) {
      List<Map<String, Object>> tableRows = jdbcTemplate.queryForList(tableSql, schema);
      for (Map<String, Object> row : tableRows) {
        String schemaName = (String) row.get("table_schema");
        String tableName = (String) row.get("table_name");
        TableInfo t =
            new TableInfo(
                schemaName + "." + tableName,
                toFriendlyName(tableName),
                fetchTableDescription(schemaName, tableName),
                fetchColumns(schemaName, tableName),
                List.of() // relationships
                // populated
                // below
                );
        tables.add(t);
      }
    }

    // 2) Fetch relationships and attach to tables
    List<RelationshipInfo> rels = fetchRelationships(schemas);
    var relsByTable = rels.stream().collect(Collectors.groupingBy(RelationshipInfo::fromTable));
    tables.replaceAll(t -> t.withRelationships(relsByTable.getOrDefault(t.name(), List.of())));

    return new SchemaMCPNew(tables);
  }

  private List<ColumnInfo> fetchColumns(String schema, String table) {
    String colSql =
        "SELECT c.column_name, c.data_type, pgd.description "
            + "FROM information_schema.columns c "
            + "LEFT JOIN pg_catalog.pg_statio_all_tables st "
            + "  ON c.table_schema = st.schemaname AND c.table_name = st.relname "
            + "LEFT JOIN pg_catalog.pg_description pgd "
            + "  ON pgd.objoid = st.relid AND pgd.objsubid = c.ordinal_position "
            + "WHERE c.table_schema = ? AND c.table_name = ?";
    return jdbcTemplate.query(
        colSql,
        new Object[] {schema, table},
        (ResultSet rs, int rowNum) ->
            new ColumnInfo(
                rs.getString("column_name"),
                toFriendlyName(rs.getString("column_name")),
                rs.getString("data_type"),
                rs.getString("description") != null ? rs.getString("description") : ""));
  }

  private String fetchTableDescription(String schema, String table) {
    String sql = "SELECT obj_description((?||'.'||?)::regclass)";
    String desc = jdbcTemplate.queryForObject(sql, new Object[] {schema, table}, String.class);
    return desc != null ? desc : "";
  }

  private List<RelationshipInfo> fetchRelationships(List<String> schemas) {
    String fkSql =
        "SELECT "
            + "  tc.table_schema AS from_schema, tc.table_name AS from_table, kcu.column_name AS from_column, "
            + "  ccu.table_schema AS to_schema, ccu.table_name AS to_table, ccu.column_name AS to_column "
            + "FROM information_schema.table_constraints tc "
            + "JOIN information_schema.key_column_usage kcu "
            + "  ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema "
            + "JOIN information_schema.constraint_column_usage ccu "
            + "  ON ccu.constraint_name = tc.constraint_name AND ccu.table_schema = tc.table_schema "
            + "WHERE tc.constraint_type = 'FOREIGN KEY' "
            + "  AND tc.table_schema = ANY (?)";
    return jdbcTemplate.query(
        fkSql,
        ps -> ps.setArray(1, ps.getConnection().createArrayOf("varchar", schemas.toArray())),
        (rs, rowNum) ->
            new RelationshipInfo(
                rs.getString("from_schema") + "." + rs.getString("from_table"),
                rs.getString("from_column"),
                rs.getString("to_schema") + "." + rs.getString("to_table"),
                rs.getString("to_column")));
  }

  private String toFriendlyName(String snake) {
    return String.join(
        " ",
        java.util.Arrays.stream(snake.split("_"))
            .map(s -> s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1))
            .toList());
  }
}
