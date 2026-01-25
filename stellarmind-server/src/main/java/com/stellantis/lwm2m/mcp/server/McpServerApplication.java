package com.stellantis.lwm2m.mcp.server;

import com.stellantis.lwm2m.mcp.server.tool.DacDbTool;
import java.util.List;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "com.stellantis.nexusconnect.mcp.server")
public class McpServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(McpServerApplication.class, args);
  }

  @Bean
  public List<ToolCallback> dacTools(DacDbTool dacDbTool) {
    return List.of(ToolCallbacks.from(dacDbTool));
  }
}
