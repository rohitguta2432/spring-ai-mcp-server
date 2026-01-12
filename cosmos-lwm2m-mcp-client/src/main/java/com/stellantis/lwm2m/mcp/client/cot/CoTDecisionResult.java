package com.stellantis.lwm2m.mcp.client.cot;

import java.util.List;

/**
 * Result of a CoT decision for schema/table selection.
 *
 * @param needsUserChoice whether the user must disambiguate
 * @param selectedSchemaTable the chosen schema.table (if auto-picked)
 * @param options list of candidate schema.tables
 * @param decisionTrace reasoning or explanation of the decision
 * @param columns list of parsed columns for the selected table
 * @param relationships list of parsed relationships (FKs, joins, cross-schema) for the selected
 *     table
 */
public record CoTDecisionResult(
    boolean needsUserChoice,
    String selectedSchemaTable,
    List<String> options,
    String decisionTrace,
    List<String> columns,
    List<String> relationships,
    List<String> fullSchemaContext) {}
