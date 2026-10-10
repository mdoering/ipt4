package org.gbif.ipt.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class BundleMessagesTest {

  private final Messages messages = new BundleMessages();

  @Test
  void englishText() {
    assertThat(messages.get(Locale.UK, "menu.about")).isEqualTo("About");
  }

  @Test
  void frenchText() {
    assertThat(messages.get(Locale.FRENCH, "menu.home")).isEqualTo("Accueil");
  }

  @Test
  void formatsArguments() {
    assertThat(messages.get(Locale.ENGLISH, "basic.createdByOn", "Ann", "2020"))
        .isEqualTo("created by <b>Ann</b> on 2020");
  }

  @Test
  void unescapesDoubledApostrophesExactlyOnce() {
    assertThat(messages.get(Locale.FRENCH, "about.notRegistered"))
        .isEqualTo("Cette installation IPT n'a pas encore été enregistrée.");
    assertThat(messages.get(Locale.FRENCH, "button.updateRegistration"))
        .isEqualTo("Mettre à jour l'enregistrement");
  }

  @Test
  void missingKeyReturnsKey() {
    assertThat(messages.get(Locale.UK, "no.such.key")).isEqualTo("no.such.key");
  }

  @Test
  void missingKeyReturnsFormattedDefault() {
    assertThat(messages.getOrDefault(Locale.UK, "no.such.key", "Hello {0}", "Ann")).isEqualTo("Hello Ann");
  }

  @Test
  void keyMissingInLocaleFallsBackToEnglish() {
    // ApplicationResources_tr has no about.installation
    assertThat(messages.get(Locale.of("tr"), "about.installation")).isEqualTo("This is a default IPT hosted by");
  }

  @Test
  void unknownLocaleFallsBackToEnglishRegardlessOfJvmDefault() {
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(Locale.FRENCH);
      assertThat(messages.get(Locale.GERMAN, "menu.home")).isEqualTo("Home");
    } finally {
      Locale.setDefault(previous);
    }
  }

  @Test
  void nullLocaleMeansEnglish() {
    assertThat(messages.get(null, "menu.home")).isEqualTo("Home");
  }

  @Test
  void britishEnglishUsesTheEnglishBundle() {
    assertThat(messages.bundle(Locale.UK).getLocale().getLanguage()).isEqualTo("en");
  }
}
