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
package org.gbif.ipt.web.portal;

import org.gbif.ipt.web.support.page.PageContext;
import org.gbif.ipt.web.support.page.PageSupport;

import java.util.UUID;

/** View getters of the IPT 3 {@code AboutAction}. Registration details arrive with the registration manager (M1). */
public class AboutPage extends PageSupport {

  public AboutPage(PageContext ctx) {
    super(ctx);
  }

  /** Registered name, else {@code about.title} from ipt.properties, else the default text. */
  public String getTitle() {
    String configured = getCfg().getProperty("about.title");
    return configured != null ? configured : getText("about.title");
  }

  public UUID getIptKey() {
    return null;
  }

  public String getIptDescription() {
    return null;
  }

  public String getHostingOrganisationName() {
    return null;
  }

  public String getPortalUrl() {
    return getCfg().getPortalUrl();
  }
}
