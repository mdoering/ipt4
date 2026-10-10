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
package org.gbif.ipt.web.support.i18n;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

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
