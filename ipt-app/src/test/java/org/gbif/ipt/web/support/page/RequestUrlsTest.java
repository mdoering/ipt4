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
package org.gbif.ipt.web.support.page;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RequestUrlsTest {

  @Test
  void dropsOnlyTheLocaleParameter() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/manage/resource");
    request.setServletPath("/manage/resource");
    request.setQueryString("r=abc&request_locale=fr&v=1.0");

    assertThat(RequestUrls.withoutLocaleParameter("https://ipt.example.org", request))
        .isEqualTo("https://ipt.example.org/manage/resource?r=abc&v=1.0");
  }

  @Test
  void noQuestionMarkWhenOnlyTheLocaleParameterWasPresent() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/about");
    request.setServletPath("/about");
    request.setQueryString("request_locale=fr");

    assertThat(RequestUrls.withoutLocaleParameter("http://localhost", request)).isEqualTo("http://localhost/about");
  }

  @Test
  void usesPathInfoWhenTheServletPathIsEmpty() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/about");
    request.setServletPath("");
    request.setPathInfo("/about");

    assertThat(RequestUrls.withoutLocaleParameter("http://localhost", request)).isEqualTo("http://localhost/about");
  }
}
