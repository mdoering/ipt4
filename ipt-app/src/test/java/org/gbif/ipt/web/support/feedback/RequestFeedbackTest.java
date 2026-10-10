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
package org.gbif.ipt.web.support.feedback;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.FlashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("feedback-probe")
class RequestFeedbackTest {

  @Autowired MockMvc mvc;

  @Test
  void messagesSurviveOneRedirect() throws Exception {
    MvcResult posted = mvc.perform(post("/_feedback/redirect").with(csrf())).andReturn();
    assertThat(posted.getResponse().getRedirectedUrl()).isEqualTo("/_feedback/show");
    FlashMap flash = posted.getFlashMap();

    String shown = mvc.perform(get("/_feedback/show").flashAttrs(flash)).andReturn().getResponse().getContentAsString();
    assertThat(shown).contains("Saved <b>it</b>").contains("Careful").contains("alert-success").contains("alert-warning");

    String again = mvc.perform(get("/_feedback/show")).andReturn().getResponse().getContentAsString();
    assertThat(again).doesNotContain("Saved").doesNotContain("Careful");
  }

  @Test
  void messagesSurviveARedirectToADoUrl() throws Exception {
    MockHttpSession session = new MockHttpSession();
    MvcResult posted = mvc.perform(post("/_feedback/redirect-do").session(session).with(csrf())).andReturn();
    assertThat(posted.getResponse().getRedirectedUrl()).isEqualTo("/_feedback/show.do");
    String shown = mvc.perform(get("/_feedback/show.do").session(session)).andReturn().getResponse()
        .getContentAsString();
    assertThat(shown).contains("Saved <b>it</b>");
  }

  @Test
  void fieldAndActionErrorsReachTheTemplate() throws Exception {
    String html = mvc.perform(get("/_feedback/field")).andReturn().getResponse().getContentAsString();
    assertThat(html).contains("Failed").contains("<li><span>Email is required</span></li>");
  }

  @Test
  void messagesAreNotEvaluatedAsTemplates() throws Exception {
    // IPT 3 rendered messages with ?interpret, so FreeMarker code inside a message ran
    String html = mvc.perform(get("/_feedback/show").flashAttr("actionMessages", java.util.List.of("${1+1}")))
        .andReturn().getResponse().getContentAsString();
    assertThat(html).contains("${1+1}").doesNotContain(">2<");
  }
}
