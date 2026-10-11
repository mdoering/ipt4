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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** The template function {@code t(key, args...)} that replaced Struts' {@code s.text}. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("fm-probe")
class TextFunctionTest {

  @Autowired MockMvc mvc;

  private String render(String path) throws Exception {
    return mvc.perform(get(path)).andReturn().getResponse().getContentAsString();
  }

  @Test
  void messagesAreMarkup() throws Exception {
    assertThat(render("/_probe/text")).contains("A:About").contains("B:created by <b>Ann</b> on 2020")
        .contains("G:This IPT installation has not been registered yet.").contains("K:Resource Title");
  }

  @Test
  void stringArgumentsAreEscaped() throws Exception {
    assertThat(render("/_probe/text")).contains("X:created by <b>&lt;i&gt;Ann&lt;/i&gt;</b> on 2020");
  }

  @Test
  void markupArgumentsAreInserted() throws Exception {
    assertThat(render("/_probe/text"))
        .contains("M:created by <b><a href=\"x?a=1&amp;b=2\">&lt;Ann&gt;</a></b> on Home");
  }

  @Test
  void numbersAreFormattedLikeInterpolations() throws Exception {
    assertThat(render("/_probe/text")).contains("N:created by <b>1,234</b> on 0.5|1,234");
  }

  @Test
  void missingKeysGiveTheKey() throws Exception {
    assertThat(render("/_probe/text")).contains("C:no.such.key");
  }

  @Test
  void markupStringIsEscapedAsPlainText() throws Exception {
    assertThat(render("/_probe/text")).contains("F:created by &lt;b&gt;Ann&lt;/b&gt; on 2020");
  }

  @Test
  void requestLocale() throws Exception {
    assertThat(render("/_probe/text?request_locale=fr")).contains("A:A propos")
        .contains("G:Cette installation IPT n'a pas encore été enregistrée.");
  }
}
