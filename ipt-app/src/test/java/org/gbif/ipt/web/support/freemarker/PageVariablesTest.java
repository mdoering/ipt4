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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Template variables: the page's properties and fields, and the {@code csrfField} directive. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("fm-probe")
class PageVariablesTest {

  @Autowired MockMvc mvc;

  private String render(String path) throws Exception {
    return mvc.perform(get(path)).andReturn().getResponse().getContentAsString();
  }

  @Test
  void topLevelNamesReadPublicFieldsOfThePage() throws Exception {
    assertThat(render("/_probe/field")).contains("FIELD:from a field");
  }

  /** Struts' value stack had the action before request and session attributes of the same name. */
  @Test
  void pagePropertiesComeBeforeSessionAttributes() throws Exception {
    String html = mvc.perform(get("/_probe/precedence").sessionAttr("title", "from the session")).andReturn()
        .getResponse().getContentAsString();
    assertThat(html).contains("T:From page");
  }

  @Test
  void topLevelNamesAreNotActionMethods() throws Exception {
    assertThat(render("/_probe/method")).contains("METHOD:none");
  }

  @Test
  void csrfFieldWritesTheToken() throws Exception {
    assertThat(render("/_probe/csrf")).containsPattern("H:<input type=\"hidden\" name=\"_csrf\" value=\"[^\"]+\"/>");
  }
}
