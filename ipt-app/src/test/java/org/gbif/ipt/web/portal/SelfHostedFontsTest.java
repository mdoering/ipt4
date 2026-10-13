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
package org.gbif.ipt.web.portal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Roboto is served by the IPT itself, so pages make no requests to Google Fonts. */
@SpringBootTest
@AutoConfigureMockMvc
class SelfHostedFontsTest {

  private static final Path CSS = Path.of("src/main/resources/static/styles/fonts/roboto/roboto.css");

  @Autowired MockMvc mvc;

  @Test
  void pagesLoadRobotoFromTheIpt() throws Exception {
    String html = mvc.perform(get("/about")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    assertThat(html)
        .contains("href=\"http://localhost/styles/fonts/roboto/roboto.css\"")
        .doesNotContain("fonts.googleapis.com", "fonts.gstatic.com");
  }

  /** The faces Google Fonts served: 9 subsets, upright and italic, all weights from one variable font. */
  @Test
  void everyFaceIsRobotoAndItsFileIsServed() throws Exception {
    String css = Files.readString(CSS);
    assertThat(css.split("@font-face", -1)).hasSize(19);
    assertThat(Pattern.compile("font-family: 'Roboto';").matcher(css).results().count()).isEqualTo(18);
    assertThat(Pattern.compile("font-style: italic;").matcher(css).results().count()).isEqualTo(9);
    assertThat(Pattern.compile("font-weight: 100 900;").matcher(css).results().count()).isEqualTo(18);

    List<String> files = new ArrayList<>();
    Matcher url = Pattern.compile("url\\(\\./(files/[^)]+\\.woff2)\\)").matcher(css);
    while (url.find()) {
      files.add(url.group(1));
    }
    assertThat(files).hasSize(18).doesNotHaveDuplicates();
    for (String file : files) {
      mvc.perform(get("/styles/fonts/roboto/" + file))
          .andExpect(status().isOk())
          .andExpect(content().contentType("font/woff2"));
    }
  }
}
