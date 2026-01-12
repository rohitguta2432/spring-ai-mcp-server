package com.stellantis.lwm2m.mcp.client.config;

import com.stellantis.lwm2m.mcp.client.service.embeddings.HybridEmbeddingModel;
import org.springframework.ai.bedrock.titan.BedrockTitanEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

  @Bean
  HybridEmbeddingModel hybridEmbeddingModel(
      OpenAiEmbeddingModel openai,
      BedrockTitanEmbeddingModel bedrock,
      @Value("${hybrid.embedding.primary:OPENAI}") String primary) {
    return new HybridEmbeddingModel(openai, bedrock, primary);
  }
}
