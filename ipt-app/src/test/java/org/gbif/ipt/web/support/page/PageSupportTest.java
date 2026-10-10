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

import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.model.IptColorScheme;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.web.support.feedback.RequestFeedback;
import org.gbif.ipt.i18n.BundleMessages;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PageSupportTest {

  private static PageSupport page(Locale locale) {
    AppConfig cfg = mock(AppConfig.class);
    when(cfg.getColorSchemeConfig()).thenReturn(new IptColorScheme());
    return new PageSupport(new PageContext(new BundleMessages(), locale, cfg, mock(RegistrationManager.class),
        "http://localhost", "http://localhost/about", null, new RequestFeedback(locale, null, null)));
  }

  @Test
  void exposesBaseActionStyleGetters() {
    PageSupport page = page(Locale.UK);

    assertThat(page.getBaseURL()).isEqualTo("http://localhost");
    assertThat(page.getBase()).isEqualTo("http://localhost");
    assertThat(page.getLogoRedirectURL()).isEqualTo("http://localhost");
    assertThat(page.getRequestURL()).isEqualTo("http://localhost/about");
    assertThat(page.getLocaleLanguage()).isEqualTo("en");
    assertThat(page.getPrimaryColor()).isEqualTo("97,168,97");
    assertThat(page.getNavbarColor()).isEqualTo("120,181,120");
    assertThat(page.getRegisteredIpt()).isNull();
    assertThat(page.getActionWarnings()).isEmpty();
  }

  @Test
  void localisesTexts() {
    PageSupport page = page(Locale.FRENCH);

    assertThat(page.getText("about.notRegistered")).isEqualTo("Cette installation IPT n'a pas encore été enregistrée.");
    assertThat(page.getText("basic.createdByOn", List.of("Ann", "2020"))).isEqualTo("créé par <b>Ann</b> le 2020");
    assertThat(page.getText("no.such.key", "fallback")).isEqualTo("fallback");
  }
}
