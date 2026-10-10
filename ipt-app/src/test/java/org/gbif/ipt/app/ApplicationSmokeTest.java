package org.gbif.ipt.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;
import org.gbif.ipt.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationSmokeTest {

  @Autowired MockMvc mvc;
  @Autowired IptProperties props;
  @Autowired Messages messages;

  @Test
  void healthIsUp() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void bindsDefaults() {
    assertThat(props.defaultLocale()).isEqualTo("en");
    assertThat(props.devMode()).isFalse();
  }

  @Test
  void exposesMessages() {
    assertThat(messages.get(Locale.UK, "menu.about")).isEqualTo("About");
  }
}
