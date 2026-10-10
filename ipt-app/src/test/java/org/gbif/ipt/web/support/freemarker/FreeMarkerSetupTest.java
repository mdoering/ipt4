package org.gbif.ipt.web.support.freemarker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import freemarker.core.InvalidReferenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
