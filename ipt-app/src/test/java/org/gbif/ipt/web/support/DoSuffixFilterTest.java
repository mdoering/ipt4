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

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class DoSuffixFilterTest {

  private HttpServletRequest filter(MockHttpServletRequest request) throws Exception {
    MockFilterChain chain = new MockFilterChain();
    new DoSuffixFilter().doFilter(request, new MockHttpServletResponse(), chain);
    return (HttpServletRequest) chain.getRequest();
  }

  @Test
  void stripsDoSuffix() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/manage/resource.do");
    request.setServletPath("/manage/resource.do");

    HttpServletRequest seen = filter(request);

    assertThat(seen.getRequestURI()).isEqualTo("/manage/resource");
    assertThat(seen.getServletPath()).isEqualTo("/manage/resource");
    assertThat(seen.getRequestURL().toString()).isEqualTo("http://localhost/manage/resource");
  }

  @Test
  void keepsQueryStringAndContextPath() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ipt/archive.do");
    request.setContextPath("/ipt");
    request.setServletPath("/archive.do");
    request.setQueryString("r=a%20b&v=1.2");

    HttpServletRequest seen = filter(request);

    assertThat(seen.getRequestURI()).isEqualTo("/ipt/archive");
    assertThat(seen.getContextPath()).isEqualTo("/ipt");
    assertThat(seen.getServletPath()).isEqualTo("/archive");
    assertThat(seen.getQueryString()).isEqualTo("r=a%20b&v=1.2");
  }

  @Test
  void stripsPathInfoWhenTheServletPathIsEmpty() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/about.do");
    request.setServletPath("");
    request.setPathInfo("/about.do");

    assertThat(filter(request).getPathInfo()).isEqualTo("/about");
  }

  @Test
  void leavesOtherPathsAlone() throws Exception {
    for (String uri : new String[] {"/styles/main.css", "/a.do/b", "/x.dox", "/.do", "/"}) {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
      assertThat(filter(request)).as(uri).isSameAs(request);
    }
  }
}
