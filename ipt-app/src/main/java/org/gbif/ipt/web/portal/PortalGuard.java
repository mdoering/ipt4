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

import org.gbif.ipt.config.Constants;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.VersionHistory;
import org.gbif.ipt.model.voc.PublicationStatus;
import org.gbif.ipt.service.manage.ResourceAccessPolicy;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.web.support.action.ActionPage;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * IPT 3's portal interceptors: {@code ResourceSessionInterceptor} (remembers the requested resource in the session)
 * and {@code PrivateDeletedResourceInterceptor} (private resources and versions only for their managers, deleted ones
 * are gone).
 */
@Component
public class PortalGuard {

  private static final Pattern VERSION = Pattern.compile("\\d{1,9}(\\.\\d{1,9})?");

  private final ResourceManager resourceManager;

  public PortalGuard(ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
  }

  /**
   * Checks the resource the page will show: the {@code r} parameter, else the one remembered in the session (IPT 3
   * remembered the requested resource before checking it, so a private one could then be read without {@code r}).
   *
   * @return a result to show instead of the page, or null to go on
   */
  @Nullable
  public String check(HttpServletRequest request, @Nullable User user) {
    boolean fromParameter = request.getParameterMap().containsKey(Constants.REQ_PARAM_RESOURCE);
    HttpSession session = request.getSession(false);
    String requested = fromParameter
        ? StringUtils.trimToNull(request.getParameter(Constants.REQ_PARAM_RESOURCE))
        : session == null ? null : (String) session.getAttribute(Constants.SESSION_RESOURCE);
    if (requested == null) {
      if (session != null) {
        session.removeAttribute(Constants.SESSION_RESOURCE);
      }
      return fromParameter ? ActionPage.NOT_FOUND : null;
    }
    String blocked = privateOrDeleted(requested, request.getParameter(Constants.REQ_PARAM_VERSION), user);
    if (blocked == null) {
      request.getSession().setAttribute(Constants.SESSION_RESOURCE, requested);
    } else if (session != null) {
      session.removeAttribute(Constants.SESSION_RESOURCE);
    }
    return blocked;
  }

  /** A version as the IPT writes them ({@code 1.0}, {@code 12.3}); anything else is not a version of a resource. */
  @Nullable
  public static BigDecimal parseVersion(@Nullable String version) {
    String v = StringUtils.trimToNull(version);
    return v != null && VERSION.matcher(v).matches() ? new BigDecimal(v) : null;
  }

  @Nullable
  private String privateOrDeleted(String shortname, @Nullable String versionParam, @Nullable User user) {
    Resource resource = resourceManager.get(shortname);
    if (resource == null) {
      return ActionPage.NOT_FOUND;
    }
    boolean manager = ResourceAccessPolicy.canManage(user, resource);
    String version = StringUtils.trimToNull(versionParam);
    if (version != null) {
      BigDecimal parsed = parseVersion(version);
      if (parsed == null) {
        // also keeps exponents like 1e100000000 from turning into huge numbers
        return ActionPage.NOT_FOUND;
      }
      VersionHistory history = resource.findVersionHistory(parsed);
      if (history != null && !manager) {
        if (history.getPublicationStatus() == PublicationStatus.PRIVATE) {
          return ActionPage.NOT_ALLOWED;
        } else if (history.getPublicationStatus() == PublicationStatus.DELETED) {
          return ActionPage.GONE;
        }
      }
      return null;
    }
    if (PublicationStatus.DELETED == resource.getStatus() && !manager) {
      return ActionPage.GONE;
    }
    PublicationStatus status = CollectionUtils.isNotEmpty(resource.getVersionHistory())
        ? resource.getVersionHistory().get(0).getPublicationStatus()
        : resource.getStatus();
    if (PublicationStatus.PRIVATE == status && !manager) {
      return ActionPage.NOT_ALLOWED;
    }
    return null;
  }
}
