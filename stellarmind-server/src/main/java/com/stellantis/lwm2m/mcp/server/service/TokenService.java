package com.stellantis.lwm2m.mcp.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TokenService {

  private static final Logger log = LoggerFactory.getLogger(TokenService.class);
  private static final String TOKEN_URL = "https://idfed.mpsa.com/as/token.oauth2";

  // The token provided by the user to be used as a Bearer token for the request
  @org.springframework.beans.factory.annotation.Value("${nexusconnect.auth.token:}")
  private String authToken;

  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  public TokenService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
    this.webClient = webClientBuilder.baseUrl(TOKEN_URL).build();
    this.objectMapper = objectMapper;
  }

  public Mono<String> getAccessToken() {
    log.info("Fetching access token from {}", TOKEN_URL);

    return webClient
        .post()
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .header("Cookie", "PSACountry=IN; PF=EHQONI0I4jJzVc5e32JLFk")
        .header("Authorization", "Bearer " + authToken)
        .bodyValue(new LinkedMultiValueMap<>()) // Empty body
        .retrieve()
        .bodyToMono(String.class)
        .map(this::extractToken)
        .doOnError(e -> log.error("Failed to fetch access token", e));
  }

  private String extractToken(String responseBody) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      if (root.has("access_token")) {
        return root.get("access_token").asText();
      } else {
        throw new RuntimeException("access_token not found in response");
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse token response", e);
    }
  }
}
