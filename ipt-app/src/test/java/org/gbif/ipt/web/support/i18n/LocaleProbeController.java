package org.gbif.ipt.web.support.i18n;

import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoint echoing the resolved locale. */
@RestController
@Profile("locale-probe")
public class LocaleProbeController {

  @GetMapping("/_probe/locale")
  public String locale(Locale locale) {
    return locale.toString();
  }
}
