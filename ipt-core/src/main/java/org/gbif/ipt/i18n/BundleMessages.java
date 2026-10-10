package org.gbif.ipt.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Port of the IPT 3 {@code SimpleTextProvider}: English is the fallback for any locale without a bundle, independent of
 * the JVM default locale. Unlike IPT 3, messages are run through {@link MessageFormat} once only; IPT 3 formatted twice,
 * which dropped apostrophes from translations such as French {@code n''a}.
 */
public final class BundleMessages implements Messages {

  public static final String DEFAULT_BUNDLE = "ApplicationResources";

  private static final ResourceBundle.Control ENGLISH_FALLBACK =
      new ResourceBundle.Control() {
        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
          return Locale.ENGLISH.equals(locale) ? null : Locale.ENGLISH;
        }
      };

  private final String baseName;

  public BundleMessages() {
    this(DEFAULT_BUNDLE);
  }

  public BundleMessages(String baseName) {
    this.baseName = baseName;
  }

  @Override
  public ResourceBundle bundle(Locale locale) {
    Locale requested = locale == null ? Locale.ENGLISH : locale;
    ClassLoader loader = BundleMessages.class.getClassLoader();
    try {
      return ResourceBundle.getBundle(baseName, requested, loader, ENGLISH_FALLBACK);
    } catch (MissingResourceException e) {
      return ResourceBundle.getBundle(baseName, Locale.ENGLISH, loader, ENGLISH_FALLBACK);
    }
  }

  @Override
  public String get(Locale locale, String key, Object... args) {
    return getOrDefault(locale, key, null, args);
  }

  @Override
  public String getOrDefault(Locale locale, String key, String defaultValue, Object... args) {
    String pattern = find(bundle(locale), key);
    if (pattern == null) {
      // there is no base bundle, so a translation's parent chain never reaches English
      pattern = find(bundle(Locale.ENGLISH), key);
    }
    if (pattern == null) {
      pattern = defaultValue != null ? defaultValue : key;
    }
    return format(pattern, args);
  }

  private static String find(ResourceBundle bundle, String key) {
    try {
      return bundle.getString(key);
    } catch (MissingResourceException e) {
      return null;
    }
  }

  private static String format(String pattern, Object[] args) {
    try {
      return MessageFormat.format(pattern, args == null ? new Object[0] : args);
    } catch (IllegalArgumentException e) {
      return pattern;
    }
  }
}
