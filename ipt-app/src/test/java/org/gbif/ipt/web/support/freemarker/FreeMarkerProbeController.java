package org.gbif.ipt.web.support.freemarker;

import java.util.Map;
import java.util.TreeMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Test-only controller rendering templates from src/test/resources/templates/probe. */
@Controller
@Profile("fm-probe")
public class FreeMarkerProbeController {

  @GetMapping("/_probe/{view}")
  public String render(@PathVariable String view, Model model) {
    model.addAttribute("html", "<b>bold</b>");
    model.addAttribute("stringKeyed", new TreeMap<>(Map.of("a", "x", "b", "y")));
    model.addAttribute("emptyMap", Map.of());
    model.addAttribute("modelValue", "from model");
    model.addAttribute(IptFreeMarkerView.PAGE, new ProbePage());
    return "probe/" + view;
  }

  public static class ProbePage {
    public String getTitle() {
      return "From page";
    }

    public String getBaseURL() {
      return "http://probe";
    }

    public String getModelValue() {
      return "from page";
    }

    public String getText(String key) {
      return "T:" + key;
    }
  }
}
