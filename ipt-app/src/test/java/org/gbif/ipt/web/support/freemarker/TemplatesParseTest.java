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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfig;

import static org.assertj.core.api.Assertions.assertThat;

/** Every template parses with IPT4's FreeMarker settings (syntax errors otherwise only show when a page is opened). */
@SpringBootTest
class TemplatesParseTest {

  @Autowired FreeMarkerConfig freeMarker;

  @Test
  void everyTemplateParses() throws Exception {
    Path root = Paths.get("src/main/resources/templates");
    List<String> broken = new ArrayList<>();
    int count = 0;
    try (Stream<Path> files = Files.walk(root)) {
      for (Path file : files.filter(f -> f.toString().endsWith(".ftl")).toList()) {
        count++;
        String name = root.relativize(file).toString();
        try {
          freeMarker.getConfiguration().getTemplate(name);
        } catch (Exception e) {
          broken.add(name + ": " + e.getMessage());
        }
      }
    }
    assertThat(count).isGreaterThan(40);
    assertThat(broken).isEmpty();
  }
}
