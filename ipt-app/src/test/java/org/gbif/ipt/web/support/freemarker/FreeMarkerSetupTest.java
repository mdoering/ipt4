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

import freemarker.core.InvalidReferenceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("fm-probe")
class FreeMarkerSetupTest {

  @Autowired MockMvc mvc;

  private String render(String view) throws Exception {
    return mvc.perform(get("/_probe/" + view)).andReturn().getResponse().getContentAsString().trim();
  }

  @Test
  void escapesHtmlByDefaultLikeStruts7() throws Exception {
    assertThat(render("escaping")).isEqualTo("&lt;b&gt;bold&lt;/b&gt;|<b>bold</b>");
  }

  @Test
  void supportsSquareBracketSyntax() throws Exception {
    assertThat(render("square")).isEqualTo("square:&lt;b&gt;bold&lt;/b&gt;");
  }

  @Test
  void wrapsMapsLikeStrutsBeanWrapper() throws Exception {
    assertThat(render("maps")).isEqualTo("x|y|a=x;b=y;|empty");
  }

  @Test
  void resolvesMissingVariablesFromPageBean() throws Exception {
    assertThat(render("fallback")).isEqualTo("From page|From page|http://probe|T:k|none|from model");
  }

  @Test
  void undefinedVariablesFailTheRequest() {
    assertThatThrownBy(() -> mvc.perform(get("/_probe/missing")))
        .hasRootCauseInstanceOf(InvalidReferenceException.class);
  }

  @Test
  void servesHtmlAsUtf8() throws Exception {
    assertThat(mvc.perform(get("/_probe/escaping")).andReturn().getResponse().getContentType())
        .isEqualTo("text/html;charset=UTF-8");
  }
}
