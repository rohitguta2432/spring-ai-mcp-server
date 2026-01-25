package com.stellantis.lwm2m.mcp.server.tool;

import com.stellantis.lwm2m.mcp.server.service.TokenService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class DmOperationTool {

  private static final Logger log = LoggerFactory.getLogger(DmOperationTool.class);
  private static final String OPERATION_API_URL =
      "http://localhost:9001/nexusconnect-gtw-ops/v1/operations";

  private final TokenService tokenService;
  private final WebClient webClient;

  public DmOperationTool(TokenService tokenService, WebClient.Builder webClientBuilder) {
    this.tokenService = tokenService;
    this.webClient = webClientBuilder.baseUrl(OPERATION_API_URL).build();
  }

  @Tool(
      name = "create_dm_operation",
      description =
          "Create a Device Management (DM) operation by sending a JSON payload to the backend system.")
  public String createOperation(String operationPayload) {
    final String traceId = UUID.randomUUID().toString();
    log.info(
        "traceId={} step=START tool=create_dm_operation payload='{}'", traceId, operationPayload);

    try {
      // 1. Get Token
      String token = tokenService.getAccessToken().block();
      if (token == null) {
        throw new RuntimeException("Failed to retrieve access token");
      }

      // 2. Call API
      String response =
          webClient
              .post()
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + token)
              .header("correlator-id", UUID.randomUUID().toString())
              .bodyValue(operationPayload)
              .retrieve()
              .bodyToMono(String.class)
              .block();

      log.info("traceId={} step=SUCCESS response='{}'", traceId, response);
      return response;

    } catch (Exception e) {
      log.error("traceId={} step=ERROR msg='{}'", traceId, e.getMessage(), e);
      return "{\"error\": \"Failed to create operation: " + e.getMessage() + "\"}";
    }
  }
}
