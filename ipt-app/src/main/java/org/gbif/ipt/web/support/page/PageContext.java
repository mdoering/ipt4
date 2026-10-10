package org.gbif.ipt.web.support.page;

import java.util.Locale;
import org.gbif.ipt.i18n.Messages;

/** Per-request facts every page bean needs; built by {@link GlobalModel}. */
public record PageContext(
    Messages messages,
    Locale locale,
    SiteConfig cfg,
    String baseUrl,
    String requestUrl,
    boolean loggedIn,
    boolean adminRights,
    boolean managerRights) {}
