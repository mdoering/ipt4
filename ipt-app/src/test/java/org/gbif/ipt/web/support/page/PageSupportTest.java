package org.gbif.ipt.web.support.page;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.gbif.ipt.app.IptProperties;
import org.gbif.ipt.i18n.BundleMessages;
import org.junit.jupiter.api.Test;

class PageSupportTest {

  private static PageSupport page(Locale locale) {
    SiteConfig cfg = new SiteConfig(new IptProperties(null, null, "en", null, false), Optional.empty(), Optional.empty());
    return new PageSupport(
        new PageContext(
            new BundleMessages(), locale, cfg, "http://localhost", "http://localhost/about", false, false, false));
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
