package org.gbif.ipt.app;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Deployment settings (environment, {@code application.yml}). Installation settings edited in the admin UI live in the
 * data directory and arrive with M1.
 *
 * @param dataDir the IPT data directory, from {@code IPT_DATA_DIR}
 * @param baseUrl public base URL; when blank it is derived from each request (incl. X-Forwarded-* headers)
 * @param defaultLocale language used until a visitor picks another one
 * @param analyticsKey optional Google Analytics key
 * @param devMode development conveniences: template reloading and error details in pages
 */
@ConfigurationProperties("ipt")
public record IptProperties(
    String dataDir,
    String baseUrl,
    @DefaultValue("en") String defaultLocale,
    String analyticsKey,
    boolean devMode) {}
