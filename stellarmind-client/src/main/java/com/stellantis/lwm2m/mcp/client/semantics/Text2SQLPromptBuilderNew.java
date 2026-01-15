package com.stellantis.lwm2m.mcp.client.semantics;

import org.springframework.stereotype.Component;

@Component
public class Text2SQLPromptBuilderNew {
  public String buildPrompt(SchemaMCPNew schema, String userQuestion) {
    StringBuilder schemaText = new StringBuilder();

    // 1) Append tables and columns
    for (TableInfo table : schema.tables()) {
      schemaText.append("Table: ").append(table.name()).append(" ");
      schemaText.append("Columns: ");
      for (ColumnInfo col : table.columns()) {
        schemaText
            .append("  - ")
            .append(col.name())
            .append(" (")
            .append(col.type())
            .append(")")
            .append(" - ")
            .append(col.description())
            .append(" ");
      }
      schemaText.append(" ");
    }

    // 2) Append all relationships
    var rels =
        schema.tables().stream().flatMap(t -> t.relationships().stream()).distinct().toList();
    if (!rels.isEmpty()) {
      schemaText.append("Relationships: ");
      for (RelationshipInfo rel : rels) {
        schemaText
            .append("  - ")
            .append(rel.fromTable())
            .append(".")
            .append(rel.fromColumn())
            .append(" → ")
            .append(rel.toTable())
            .append(".")
            .append(rel.toColumn())
            .append(" ");
      }
      schemaText.append(" ");
    }

    // 3) Build prompt
    return String.format(
        """
						You are an expert SQL generator. Convert the following user question into a single valid PostgreSQL SELECT query.

						Rules you must follow:
						- ONLY output the SQL SELECT query — no comments, no assumptions, no explanation.
						- DO NOT use markdown formatting (no triple backticks).
						- DO NOT invent tables or columns — use ONLY the ones listed in the schema below.
						- The output MUST start with 'SELECT' and be executable as-is.
						- ALWAYS use schema-qualified table names (e.g., bs.table_name).
						- DO NOT drop or simplify schema prefixes.

						MULTI-SCHEMA HANDLING RULES:
						- When searching for entities that exist in BOTH bs and gtw schemas (like ecu, vehicle), use UNION ALL to query both schemas.
						- For operational/status queries, ALWAYS check both schemas as data might exist in either location.
						- Use this pattern: SELECT ... FROM bs.table WHERE ... UNION ALL SELECT ... FROM gtw.table WHERE ...
						- If querying related data (like ECU with vehicle), ensure UNION is applied to the main entity being searched.

						SCHEMA CONTEXT:
						- bs schema: Contains historical/archived operational data (also referred to as "bootstrap")
						- gtw schema: Contains real-time/current operational data (also referred to as "gateway")
						- Both schemas may contain the same entity types with identical structures
						- Data for the same entity might exist in one schema but not the other

						SCHEMA ALIASES:
						- When user mentions "bootstrap" → use bs schema
						- When user mentions "gateway" → use gtw schema
						- When user specifies a schema preference, prioritize that schema but still include UNION for completeness

						COMMON PATTERNS:
						- Vehicle/VIN queries: Check both bs.bs_vehicle and gtw.vehicle
						- ECU queries: Check both bs.bs_ecu and gtw.ecu
						- Status/operational queries: Always use UNION ALL approach
						- When joining related tables, apply UNION to the primary entity first

						--- DATABASE SCHEMA ---
						%s
						------------------------

						USER QUESTION:
						%s

						SQL:
						""",
        schemaText.toString().trim(), userQuestion.trim());
  }
}
