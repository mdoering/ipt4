package org.gbif.ipt.web.support;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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
