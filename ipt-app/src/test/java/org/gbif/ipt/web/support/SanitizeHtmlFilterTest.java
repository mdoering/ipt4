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
    request.addParameter("eml.additionalInfo", "<p>A <b>bold</b> <a href=\"https://gbif.org\">link</a></p>");
    request.addParameter("eml.abstract", "<p>ok</p><script>x()</script><img src=x onerror=y>");
    HttpServletRequest out = filtered(request);
    assertThat(out.getParameter("eml.additionalInfo"))
        .isEqualTo("<p>A <b>bold</b> <a href=\"https://gbif.org\">link</a></p>");
    assertThat(out.getParameter("eml.abstract")).isEqualTo("<p>ok</p>");
    @SuppressWarnings("unchecked")
    Map<String, String> flagged = (Map<String, String>) out.getAttribute(SanitizeHtmlFilter.XSS_FLAGGED_FIELDS_ATTR);
    assertThat(flagged).containsOnlyKeys("eml.abstract");
  }

  /** The editors post DocBook for these fields; EmlValidator checks it against the EML schema. */
  @Test
  void docBookFieldsPassUnchangedUnlessALinkIsNotAWebLink() throws Exception {
    String docBook = "<para>Benthem &amp; Hooker\u2019s <emphasis>flora</emphasis> <ulink url=\"https://gbif.org\">"
        + "<citetitle>GBIF</citetitle></ulink></para><itemizedlist><listitem><para>a</para></listitem></itemizedlist>"
        + "<literalLayout>x</literalLayout>";
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/manage/metadata-basic.do");
    request.addParameter("eml.description", docBook);
    request.addParameter("eml.purpose", "<para><ulink url=\"javascript:alert(1)\">x</ulink></para>");
    HttpServletRequest out = filtered(request);
    assertThat(out.getParameter("eml.description")).isEqualTo(docBook);
    @SuppressWarnings("unchecked")
    Map<String, String> flagged = (Map<String, String>) out.getAttribute(SanitizeHtmlFilter.XSS_FLAGGED_FIELDS_ATTR);
    assertThat(flagged).containsOnlyKeys("eml.purpose");
  }

  /** Only DocBook elements, only the url attribute, only web or mail links, whatever section posts the field. */
  @Test
  void docBookFieldsWithOtherMarkupAreFlagged() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/manage/metadata-keywords.do");
    request.addParameter("eml.description", "<img src=x onerror=alert(1)>");
    request.addParameter("eml.purpose", "<ulink url=\"http://x&quot; onmouseover=&quot;alert(1)&quot; x=&quot;\">"
        + "<citetitle>a</citetitle></ulink>");
    request.addParameter("eml.introduction", "<para onclick=\"alert(1)\">x</para>");
    request.addParameter("eml.gettingStarted", "<para>Plain text with A & B and 1 < 2</para>");
    request.addParameter("eml.acknowledgements", "Plain text, no markup & no tags");
    HttpServletRequest out = filtered(request);
    @SuppressWarnings("unchecked")
    Map<String, String> flagged = (Map<String, String>) out.getAttribute(SanitizeHtmlFilter.XSS_FLAGGED_FIELDS_ATTR);
    assertThat(flagged).containsOnlyKeys("eml.description", "eml.purpose", "eml.introduction");
  }

  /** Entity-encoded markup in a plain-text field decodes to markup: flagged, so it is not saved. */
  @Test
  void encodedMarkupInPlainTextIsFlagged() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/manage/metadata-geocoverage.do");
    request.addParameter("eml.geospatialCoverages[0].description", "&lt;img src=x onerror=alert(1)&gt;");
    request.addParameter("eml.title", "Fish &amp; chips, 1 &lt; 2");
    HttpServletRequest out = filtered(request);
    @SuppressWarnings("unchecked")
    Map<String, String> flagged = (Map<String, String>) out.getAttribute(SanitizeHtmlFilter.XSS_FLAGGED_FIELDS_ATTR);
    assertThat(flagged).containsOnlyKeys("eml.geospatialCoverages[0].description");
    assertThat(out.getParameter("eml.title")).isEqualTo("Fish & chips, 1 < 2");
  }

  /** Passwords are what the user typed: never changed (IPT 3 altered and logged them). */
  @Test
  void passwordsAreLeftAlone() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
    request.addParameter("password", "a<b>c&amp;d");
    request.addParameter("sqlSourcePassword", "<x>");
    request.addParameter("user.password", "p<q");
    HttpServletRequest out = filtered(request);
    assertThat(out.getParameter("password")).isEqualTo("a<b>c&amp;d");
    assertThat(out.getParameter("sqlSourcePassword")).isEqualTo("<x>");
    assertThat(out.getParameter("user.password")).isEqualTo("p<q");
    assertThat(out.getAttribute(SanitizeHtmlFilter.XSS_FLAGGED_FIELDS_ATTR)).isNull();
  }
}
