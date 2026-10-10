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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("fm-probe")
class TemplateShimTest {

  @Autowired MockMvc mvc;

  private String render(String path) throws Exception {
    return mvc.perform(get(path)).andReturn().getResponse().getContentAsString();
  }

  @Test
  void textTagBehavesLikeStruts() throws Exception {
    String html = render("/_probe/shim");

    assertThat(html)
        .contains("A:About")
        .contains("B:created by <b>Ann</b> on 2020")
        .contains("C:no.such.key")
        .contains("D:Fallback")
        .contains("E:[Home]")
        .contains("F:created by &lt;b&gt;Ann&lt;/b&gt; on 2020")
        .contains("G:This IPT installation has not been registered yet.");
  }

  @Test
  void textTagUsesTheRequestLocale() throws Exception {
    String html = render("/_probe/shim?request_locale=fr");

    assertThat(html).contains("A:A propos").contains("G:Cette installation IPT n'a pas encore été enregistrée.");
  }

  @Test
  void squareBracketSyntaxWorks() throws Exception {
    assertThat(render("/_probe/shim_square")).contains("S:created by <b>Bo</b> on 1999");
  }

  @Test
  void csrfFieldWritesTheToken() throws Exception {
    assertThat(render("/_probe/shim")).containsPattern("H:<input type=\"hidden\" name=\"_csrf\" value=\"[^\"]+\"/>");
  }

  @Test
  void unsupportedStrutsTagsFailLoudly() {
    assertThatThrownBy(() -> mvc.perform(get("/_probe/unsupported")))
        .rootCause()
        .hasMessageContaining("s.submit is not supported by IPT4 yet");
  }
}
