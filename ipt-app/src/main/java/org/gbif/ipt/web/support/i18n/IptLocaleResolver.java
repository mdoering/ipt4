package org.gbif.ipt.web.support.i18n;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * IPT 3 locale rules: a visitor's choice ({@code ?request_locale=xx}) is kept in the session, only the enabled UI
 * languages are accepted, and the default is the configured IPT language, never the browser's Accept-Language.
 */
public class IptLocaleResolver extends SessionLocaleResolver {

  /** Enabled UI languages; English is en_GB for day-month-year dates, as in IPT 3. */
  public static final List<Locale> SUPPORTED =
      List.of(
          Locale.UK,
          Locale.FRENCH,
          Locale.of("es"),
          Locale.CHINESE,
          Locale.of("pt"),
          Locale.JAPANESE,
          Locale.of("ru"));

  public IptLocaleResolver(Supplier<String> configuredDefault) {
    setDefaultLocaleFunction(request -> supported(parse(configuredDefault.get())).orElse(Locale.UK));
  }

  @Override
  public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
    if (locale == null) {
      super.setLocale(request, response, null);
      return;
    }
    supported(locale).ifPresent(l -> super.setLocale(request, response, l));
  }

  /** The enabled UI locale with the same language as {@code locale}, if any. */
  public static Optional<Locale> supported(Locale locale) {
    if (locale == null) {
      return Optional.empty();
    }
    return SUPPORTED.stream().filter(s -> s.getLanguage().equals(locale.getLanguage())).findFirst();
  }

  private static Locale parse(String value) {
    return value == null || value.isBlank() ? null : StringUtils.parseLocale(value);
  }
}
