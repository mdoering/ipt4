package org.gbif.ipt.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

/** Localised UI texts from the {@code ApplicationResources} bundles. */
public interface Messages {

  /** The text for {@code key} formatted with {@code args}, or the key itself if no bundle has it. */
  String get(Locale locale, String key, Object... args);

  /** The text for {@code key} formatted with {@code args}, or {@code defaultValue} (formatted) if no bundle has it. */
  String getOrDefault(Locale locale, String key, String defaultValue, Object... args);

  /** The bundle for {@code locale}, falling back to English. */
  ResourceBundle bundle(Locale locale);
}
