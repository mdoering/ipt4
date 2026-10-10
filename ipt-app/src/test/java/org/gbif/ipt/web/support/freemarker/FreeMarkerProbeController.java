/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.ipt.web.support.freemarker;

import java.util.List;
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
    model.addAttribute("fieldErrors", Map.of("email", List.of("Email is <required>")));
    model.addAttribute("options", new TreeMap<>(Map.of("a", "Alpha", "b", "Beta & Co")));
    model.addAttribute("beans", List.of(new Lang("en", "English"), new Lang("fr", "French")));
    model.addAttribute("page", new ProbePage());
    model.addAttribute(IptFreeMarkerView.PAGE, new ProbePage());
    return "probe/" + view;
  }

  public record Lang(String code, String label) {
    public String getCode() {
      return code;
    }

    public String getLabel() {
      return label;
    }
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
