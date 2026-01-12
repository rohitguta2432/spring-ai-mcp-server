package com.stellantis.lwm2m.mcp.client.cot.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiController {

  @GetMapping("/cot")
  public String showCotPage() {
    return "cot";
  }
}
