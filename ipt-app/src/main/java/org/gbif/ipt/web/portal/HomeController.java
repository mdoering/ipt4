package org.gbif.ipt.web.portal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Until the portal home page is ported (M2), the root shows the about page. */
@Controller
public class HomeController {

  @GetMapping("/")
  public String home() {
    return "redirect:/about";
  }
}
