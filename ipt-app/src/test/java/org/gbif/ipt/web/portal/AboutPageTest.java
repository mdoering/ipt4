package org.gbif.ipt.web.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class AboutPageTest {

  @Autowired MockMvc mvc;

  private String html(MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(request)
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/html;charset=UTF-8"))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Test
  void rendersTheIpt3AboutPage() throws Exception {
    String html = html(get("/about"));

    assertThat(html)
        .contains("<title>About</title>")
        .contains("About this IPT installation")
        .contains("This IPT installation has not been registered yet.")
        .contains("--color-gbif-primary: 97,168,97;")
        .contains("href=\"http://localhost/styles/main.css\"")
        .contains("Integrated Publishing Toolkit (IPT) Version")
        .contains("TEST MODE")
        .containsPattern("<meta name=\"_csrf\" content=\"[^\"]+\"/>")
        .doesNotContain("<@s.", "[@s.", "${");
  }

  @Test
  void loginFormInMenuCarriesCsrfToken() throws Exception {
    assertThat(html(get("/about")))
        .containsPattern("action=\"http://localhost/login.do\" method=\"post\"[^>]*>\\s*<input type=\"hidden\" name=\"_csrf\"");
  }

  @Test
  void legacyDoUrlWorks() throws Exception {
    assertThat(html(get("/about.do"))).contains("About this IPT installation");
  }

  @Test
  void headRequestOnDoUrl() throws Exception {
    mvc.perform(head("/about.do")).andExpect(status().isOk());
  }

  @Test
  void switchesToFrenchAndRemembersIt() throws Exception {
    MockHttpSession session = new MockHttpSession();

    assertThat(html(get("/about").param("request_locale", "fr").session(session)))
        .contains("<title>A propos</title>")
        .contains("Cette installation IPT n'a pas encore été enregistrée.");
    assertThat(html(get("/about").session(session))).contains("À propos de cette installation IPT");
  }

  @Test
  void languageMenuLinksKeepTheCurrentPage() throws Exception {
    assertThat(html(get("/about").param("request_locale", "fr")))
        .contains("href=\"http://localhost/about?request_locale=es\"")
        .doesNotContain("request_locale=fr&amp;request_locale");
  }

  @Test
  void escapesHostileQueryStringInLanguageLinks() throws Exception {
    // a client can send the query unencoded; it is echoed into the language links via requestURL
    String html =
        html(
            get("/about")
                .with(
                    request -> {
                      request.setQueryString("x=\"><script>alert(1)</script>");
                      return request;
                    }));

    assertThat(html)
        .doesNotContain("<script>alert(1)</script>")
        .contains("&lt;script&gt;alert(1)&lt;/script&gt;");
  }

  @Test
  void honoursForwardedHeaders() throws Exception {
    String html =
        html(
            get("/about")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "ipt.example.org")
                .header("X-Forwarded-Prefix", "/ipt"));

    assertThat(html).contains("href=\"https://ipt.example.org/ipt/styles/main.css\"");
  }

  @Test
  void anonymousMenuHasNoManageOrAdminLinks() throws Exception {
    assertThat(html(get("/about"))).doesNotContain("href=\"http://localhost/manage/\"", "href=\"http://localhost/admin/\"");
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminMenuHasManageAndAdminLinks() throws Exception {
    assertThat(html(get("/about")))
        .contains("href=\"http://localhost/manage/\"")
        .contains("href=\"http://localhost/admin/\"");
  }

  @Test
  void servesVendoredStaticFiles() throws Exception {
    mvc.perform(get("/styles/main.css")).andExpect(status().isOk());
    mvc.perform(get("/js/jquery/jquery-3.7.1.min.js")).andExpect(status().isOk());
  }

  @Test
  void rootRedirectsToAboutUntilThePortalHomeIsPorted() throws Exception {
    mvc.perform(get("/")).andExpect(status().isFound()).andExpect(redirectedUrl("/about"));
  }
}
