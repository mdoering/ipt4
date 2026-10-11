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
package org.gbif.ipt.web.support;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/** IPT 3's SanitizeHtmlFilter: every parameter loses its tags, the EML rich-text fields keep the allowed ones. */
class SanitizeHtmlFilterTest {

  private static HttpServletRequest filtered(MockHttpServletRequest request) throws Exception {
    MockFilterChain chain = new MockFilterChain();
    new SanitizeHtmlFilter().doFilter(request, new MockHttpServletResponse(), chain);
    return (HttpServletRequest) chain.getRequest();
  }

  @Test
  void plainTextLosesAllTags() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/account");
    request.addParameter("user.firstname", "<script>alert(1)</script>Ann<br>Lee");
    request.addParameter("user.lastname", "O'Brien & Sons");
    HttpServletRequest out = filtered(request);
    assertThat(out.getParameter("user.firstname")).isEqualTo("Ann\n\nLee");
    assertThat(out.getParameter("user.lastname")).isEqualTo("O'Brien & Sons");
    @SuppressWarnings("unchecked")
    Map<String, String> flagged = (Map<String, String>) out.getAttribute(SanitizeHtmlFilter.XSS_FLAGGED_FIELDS_ATTR);
    assertThat(flagged).containsOnlyKeys("user.firstname");
  }

  @Test
  void richTextKeepsTheAllowedTags() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/manage/metadata-basic.do");
    request.addParameter("eml.description", "<p>A <b>bold</b> <a href=\"https://gbif.org\">link</a></p>");
    request.addParameter("eml.abstract", "<p>ok</p><script>x()</script><img src=x onerror=y>");
    HttpServletRequest out = filtered(request);
    assertThat(out.getParameter("eml.description"))
        .isEqualTo("<p>A <b>bold</b> <a href=\"https://gbif.org\">link</a></p>");
    assertThat(out.getParameter("eml.abstract")).isEqualTo("<p>ok</p>");
    @SuppressWarnings("unchecked")
    Map<String, String> flagged = (Map<String, String>) out.getAttribute(SanitizeHtmlFilter.XSS_FLAGGED_FIELDS_ATTR);
    assertThat(flagged).containsOnlyKeys("eml.abstract");
  }
}
