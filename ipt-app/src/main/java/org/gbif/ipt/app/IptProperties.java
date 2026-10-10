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
