package org.gbif.ipt.web.support.i18n;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("locale-probe")
class LocaleChangeTest {

  @Autowired MockMvc mvc;

  @Test
  void defaultIsBritishEnglish() throws Exception {
    mvc.perform(get("/_probe/locale")).andExpect(content().string("en_GB"));
  }

  @Test
  void requestLocaleParameterSwitchesAndIsRemembered() throws Exception {
    MockHttpSession session = new MockHttpSession();
    mvc.perform(get("/_probe/locale").param("request_locale", "ja").session(session))
        .andExpect(content().string("ja"));
    mvc.perform(get("/_probe/locale").session(session)).andExpect(content().string("ja"));
  }

  @Test
  void worksOnDoUrls() throws Exception {
    mvc.perform(get("/_probe/locale.do").param("request_locale", "fr")).andExpect(content().string("fr"));
  }

  @Test
  void ignoresInvalidLocaleValues() throws Exception {
    for (String value : new String[] {"xx", "", "<script>alert(1)</script>", "de_DE"}) {
      mvc.perform(get("/_probe/locale").param("request_locale", value))
          .andExpect(status().isOk())
          .andExpect(content().string("en_GB"));
    }
  }
}
