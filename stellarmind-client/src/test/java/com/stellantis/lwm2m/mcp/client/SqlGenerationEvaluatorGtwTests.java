package com.stellantis.lwm2m.mcp.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.stellantis.lwm2m.mcp.client.evaluators.fake.FakeFactCheckingEvaluator;
import com.stellantis.lwm2m.mcp.client.evaluators.fake.FakeFaithfulnessEvaluator;
import com.stellantis.lwm2m.mcp.client.evaluators.fake.FakeRelevancyEvaluator;
import com.stellantis.lwm2m.mcp.client.evaluators.fake.FakeResponsibleAiEvaluator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;

/**
 * Unit tests for SQL generation (GTW schema). Demonstrates evaluation framework dimensions: -
 * Relevance - Correctness - Faithfulness - Responsible AI
 */
class SqlGenerationEvaluatorGtwTests {

  // ----------------------------
  // Test Data
  // ----------------------------

  record TestCase(String id, String nlq, String expectedSql, List<Document> context) {}

  static Stream<TestCase> sqlTestCases() {
    return Stream.of(
        new TestCase(
            "GTW-009",
            "Vehicles by region counts",
            "SELECT v.vehicle_region, COUNT(*) FROM gtw.vehicle v GROUP BY v.vehicle_region",
            List.of(
                new Document(
                    "Table gtw.vehicle (id, vin, vehicle_region, update_date, vehicle_architecture, vehicle_program_code, creation_date)"))),
        new TestCase(
            "GTW-010",
            "Latest updated vehicle",
            "SELECT * FROM gtw.vehicle v ORDER BY v.update_date DESC LIMIT 1",
            List.of(
                new Document(
                    "Table gtw.vehicle (id, vin, vehicle_region, update_date, vehicle_architecture, vehicle_program_code, creation_date)"))),
        new TestCase(
            "GTW-011",
            "Vehicles with architecture NEA and region NA",
            "SELECT * FROM gtw.vehicle v WHERE v.vehicle_architecture = 'NEA' AND v.vehicle_region = 'NA'",
            List.of(
                new Document(
                    "Table gtw.vehicle (id, vin, vehicle_region, vehicle_architecture, vehicle_program_code, creation_date)"))),
        new TestCase(
            "GTW-012",
            "Distinct vehicle program codes",
            "SELECT DISTINCT v.vehicle_program_code FROM gtw.vehicle v",
            List.of(new Document("Table gtw.vehicle (id, vin, vehicle_program_code)"))),
        new TestCase(
            "GTW-013",
            "Vehicles created today",
            "SELECT * FROM gtw.vehicle v WHERE DATE(v.creation_date) = CURRENT_DATE",
            List.of(new Document("Table gtw.vehicle (id, vin, creation_date)"))),
        new TestCase(
            "GTW-014",
            "Vehicles with VIN starting with '1HG'",
            "SELECT * FROM gtw.vehicle v WHERE v.vin LIKE '1HG%'",
            List.of(new Document("Table gtw.vehicle (id, vin)"))),
        new TestCase(
            "GTW-015",
            "Total vehicles",
            "SELECT COUNT(*) FROM gtw.vehicle",
            List.of(new Document("Table gtw.vehicle (id, vin)"))),
        new TestCase(
            "GTW-016",
            "List all ECUs",
            "SELECT * FROM gtw.ecu e",
            List.of(
                new Document(
                    "Table gtw.ecu (id, vin, active, device_type, device_variant_type, creation_date, update_date, serial_number)"))),
        new TestCase(
            "GTW-017",
            "Show active ECUs",
            "SELECT * FROM gtw.ecu e WHERE e.active = true",
            List.of(new Document("Table gtw.ecu (id, vin, active, device_type)"))),
        new TestCase(
            "GTW-018",
            "Find ECUs of type RTCU",
            "SELECT * FROM gtw.ecu e WHERE e.device_type = 'RTCU'",
            List.of(new Document("Table gtw.ecu (id, vin, device_type)"))),
        new TestCase(
            "GTW-019",
            "ECUs by variant counts",
            "SELECT e.device_variant_type, COUNT(*) FROM gtw.ecu e GROUP BY e.device_variant_type",
            List.of(new Document("Table gtw.ecu (id, vin, device_variant_type)"))),
        new TestCase(
            "GTW-020",
            "ECUs created in July 2025",
            "SELECT * FROM gtw.ecu e WHERE DATE(e.creation_date) BETWEEN '2025-07-01' AND '2025-07-31'",
            List.of(new Document("Table gtw.ecu (id, vin, creation_date)"))),
        new TestCase(
            "GTW-021",
            "ECUs linked to VIN 1HGTY826333123458",
            "SELECT * FROM gtw.ecu e WHERE e.vin = '1HGTY826333123458'",
            List.of(new Document("Table gtw.ecu (id, vin)"))),
        new TestCase(
            "GTW-022",
            "Count ECUs per VIN",
            "SELECT e.vin, COUNT(*) FROM gtw.ecu e GROUP BY e.vin",
            List.of(new Document("Table gtw.ecu (id, vin)"))),
        new TestCase(
            "GTW-023",
            "ECUs updated after creation",
            "SELECT * FROM gtw.ecu e WHERE e.update_date > e.creation_date",
            List.of(new Document("Table gtw.ecu (id, vin, creation_date, update_date)"))),
        new TestCase(
            "GTW-024",
            "Latest ECU update",
            "SELECT * FROM gtw.ecu e ORDER BY e.update_date DESC LIMIT 1",
            List.of(new Document("Table gtw.ecu (id, vin, update_date)"))),
        new TestCase(
            "GTW-025",
            "Inactive ECUs list",
            "SELECT * FROM gtw.ecu e WHERE e.active = false",
            List.of(new Document("Table gtw.ecu (id, vin, active)"))),
        new TestCase(
            "GTW-026",
            "ECUs with long variant names (len > 10)",
            "SELECT * FROM gtw.ecu e WHERE length(e.device_variant_type) > 10",
            List.of(new Document("Table gtw.ecu (id, vin, device_variant_type)"))),
        new TestCase(
            "GTW-027",
            "ECU by serial number T1175006474064815725",
            "SELECT * FROM gtw.ecu e WHERE e.serial_number = 'T1175006474064815725'",
            List.of(new Document("Table gtw.ecu (id, vin, serial_number)"))),
        new TestCase(
            "GTW-028",
            "ECUs per device type and activity",
            "SELECT e.device_type, e.active, COUNT(*) FROM gtw.ecu e GROUP BY e.device_type, e.active",
            List.of(new Document("Table gtw.ecu (id, vin, device_type, active)"))),
        new TestCase(
            "GTW-029",
            "VINs having more than one ECU",
            "SELECT e.vin, COUNT(*) FROM gtw.ecu e GROUP BY e.vin HAVING COUNT(*) > 1",
            List.of(new Document("Table gtw.ecu (id, vin)"))),
        new TestCase(
            "GTW-030",
            "ECUs updated on a different day than creation",
            "SELECT * FROM gtw.ecu e WHERE DATE(e.update_date) <> DATE(e.creation_date)",
            List.of(new Document("Table gtw.ecu (id, vin, creation_date, update_date)"))));
  }

  // ----------------------------
  // Evaluators under test
  // ----------------------------

  private final FakeRelevancyEvaluator relevancyEvaluator = new FakeRelevancyEvaluator();
  private final FakeFactCheckingEvaluator factCheckingEvaluator = new FakeFactCheckingEvaluator();
  private final FakeFaithfulnessEvaluator faithfulnessEvaluator = new FakeFaithfulnessEvaluator();
  private final FakeResponsibleAiEvaluator responsibleAiEvaluator =
      new FakeResponsibleAiEvaluator();

  // ----------------------------
  // Tests
  // ----------------------------

  @ParameterizedTest(name = "{0} - {1}")
  @MethodSource("sqlTestCases")
  void testFrameworkDimensions(TestCase testCase) {
    EvaluationRequest req =
        new EvaluationRequest(testCase.nlq(), testCase.context(), testCase.expectedSql());

    // Relevance
    assertThat(relevancyEvaluator.evaluate(req).isPass()).as(testCase.id() + " relevancy").isTrue();

    // Correctness
    assertThat(factCheckingEvaluator.evaluate(req).isPass())
        .as(testCase.id() + " correctness")
        .isTrue();

    // Faithfulness
    assertThat(faithfulnessEvaluator.evaluate(req).isPass())
        .as(testCase.id() + " faithfulness")
        .isTrue();

    // Safety
    assertThat(responsibleAiEvaluator.evaluate(req).isPass())
        .as(testCase.id() + " safety")
        .isTrue();
  }
}
