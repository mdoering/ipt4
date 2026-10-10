package org.gbif.ipt.web.support.page;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

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
