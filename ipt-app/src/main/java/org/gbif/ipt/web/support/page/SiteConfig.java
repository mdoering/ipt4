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
package org.gbif.ipt.web.support.page;

import org.gbif.ipt.app.IptProperties;

import java.util.Optional;

import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Component;

/**
 * The configuration the page layout reads, exposed to templates as {@code cfg} with the method names of the IPT 3
 * {@code AppConfig}. M1 replaces this M0 stand-in with the core AppConfig backed by the data directory.
 */
@Component
public class SiteConfig {

  static final int CSRF_PAGE_REFRESH_DELAY_MS = 10 * 60 * 1000;
  private static final String GBIF_UAT_PORTAL = "https://www.gbif-uat.org";

  private final IptProperties props;
  private final String version;
  private final String shortVersion;

  public SiteConfig(IptProperties props, Optional<BuildProperties> build, Optional<GitProperties> git) {
    this.props = props;
    this.shortVersion = build.map(BuildProperties::getVersion).orElse("dev");
    this.version =
        git.map(GitProperties::getShortCommitId).map(id -> shortVersion + "-r" + id).orElse(shortVersion);
  }

  public String getBaseUrl() {
    String base = props.baseUrl();
    if (base == null || base.isBlank()) {
      return null;
    }
    return base.strip().replaceAll("/+$", "");
  }

  /** Configurable in the admin UI from M5; until then the logo links to the base URL. */
  public String getLogoRedirectUrl() {
    return null;
  }

  public String getVersion() {
    return version;
  }

  public String getShortVersion() {
    return shortVersion;
  }

  public String getAnalyticsKey() {
    return props.analyticsKey();
  }

  public boolean devMode() {
    return props.devMode();
  }

  /** Chosen during setup (M2); until then the IPT runs in test mode like an unconfigured IPT 3. */
  public String getRegistryType() {
    return null;
  }

  public String getPortalUrl() {
    return GBIF_UAT_PORTAL;
  }

  public int getCsrfPageRefreshDelay() {
    return CSRF_PAGE_REFRESH_DELAY_MS;
  }

  public String getDefaultLocale() {
    return props.defaultLocale();
  }

  /** Free-form installation properties ({@code ipt.properties}) arrive with the data directory in M1. */
  public String getProperty(String key) {
    return null;
  }

  public ColorScheme getColorSchemeConfig() {
    return ColorScheme.DEFAULT;
  }
}
