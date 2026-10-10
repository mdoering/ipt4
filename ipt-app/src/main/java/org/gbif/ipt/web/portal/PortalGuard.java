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

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IPT 3's portal interceptors: {@code ResourceSessionInterceptor} (remembers the requested resource in the session)
 * and {@code PrivateDeletedResourceInterceptor} (private resources and versions only for their managers, deleted ones
 * are gone).
 */
@Component
public class PortalGuard {

  private final ResourceManager resourceManager;

  public PortalGuard(ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
  }

  /** @return a result to show instead of the page, or null to go on */
  @Nullable
  public String check(HttpServletRequest request, @Nullable User user) {
    if (request.getParameterMap().containsKey(Constants.REQ_PARAM_RESOURCE)) {
      String requested = StringUtils.trimToNull(request.getParameter(Constants.REQ_PARAM_RESOURCE));
      if (requested == null) {
        if (request.getSession(false) != null) {
          request.getSession().removeAttribute(Constants.SESSION_RESOURCE);
        }
        return ActionPage.NOT_FOUND;
      }
      request.getSession().setAttribute(Constants.SESSION_RESOURCE, requested);
      return privateOrDeleted(requested, request.getParameter(Constants.REQ_PARAM_VERSION), user);
    }
    return null;
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
      try {
        VersionHistory history = resource.findVersionHistory(new BigDecimal(version));
        if (history != null) {
          if (history.getPublicationStatus() == PublicationStatus.PRIVATE && !manager) {
            return ActionPage.NOT_ALLOWED;
          } else if (history.getPublicationStatus() == PublicationStatus.DELETED && !manager) {
            return ActionPage.GONE;
          }
        }
      } catch (NumberFormatException e) {
        return ActionPage.NOT_FOUND;
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
