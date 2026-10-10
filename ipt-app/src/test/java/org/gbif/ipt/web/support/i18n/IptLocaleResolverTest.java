package org.gbif.ipt.web.support.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class IptLocaleResolverTest {

  private final MockHttpServletRequest request = new MockHttpServletRequest();
  private final MockHttpServletResponse response = new MockHttpServletResponse();

  @Test
  void defaultsToConfiguredLanguage() {
    assertThat(new IptLocaleResolver(() -> "fr").resolveLocale(request)).isEqualTo(Locale.FRENCH);
  }

  @Test
  void englishMeansBritishEnglish() {
    assertThat(new IptLocaleResolver(() -> "en").resolveLocale(request)).isEqualTo(Locale.UK);
  }

  @Test
  void unsupportedOrBlankDefaultFallsBackToBritishEnglish() {
    assertThat(new IptLocaleResolver(() -> "de").resolveLocale(request)).isEqualTo(Locale.UK);
    assertThat(new IptLocaleResolver(() -> "").resolveLocale(request)).isEqualTo(Locale.UK);
    assertThat(new IptLocaleResolver(() -> null).resolveLocale(request)).isEqualTo(Locale.UK);
  }

  @Test
  void ignoresAcceptLanguageHeader() {
    request.addPreferredLocale(Locale.JAPANESE);
    assertThat(new IptLocaleResolver(() -> "en").resolveLocale(request)).isEqualTo(Locale.UK);
  }

  @Test
  void storesSupportedLocaleInSession() {
    IptLocaleResolver resolver = new IptLocaleResolver(() -> "en");
    resolver.setLocale(request, response, Locale.of("pt", "BR"));
    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.of("pt"));
  }

  @Test
  void ignoresUnsupportedLocale() {
    IptLocaleResolver resolver = new IptLocaleResolver(() -> "en");
    resolver.setLocale(request, response, Locale.FRENCH);
    resolver.setLocale(request, response, Locale.GERMAN);
    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.FRENCH);
  }

  @Test
  void nullResetsToDefault() {
    IptLocaleResolver resolver = new IptLocaleResolver(() -> "en");
    resolver.setLocale(request, response, Locale.FRENCH);
    resolver.setLocale(request, response, null);
    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.UK);
  }
}
