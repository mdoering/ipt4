package org.gbif.ipt.web.portal;

import java.util.UUID;
import org.gbif.ipt.web.support.page.PageContext;
import org.gbif.ipt.web.support.page.PageSupport;

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
