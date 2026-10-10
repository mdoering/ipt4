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
package org.gbif.ipt.web.manage;

import org.gbif.ipt.config.Constants;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.manage.ResourceAccessPolicy;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;
import org.gbif.ipt.web.support.action.ActionPage;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * IPT 3's ResourceSessionInterceptor and the per-resource part of RequireManagerInterceptor, for the resource a manage
 * page will work on: the {@code r} parameter, else the one remembered in the session. IPT 3 remembered {@code r}
 * before checking it and only checked {@code r}, so a manager could reach another manager's resource through the
 * session; here the effective resource is checked and remembered only once access is granted.
 */
@Component
public class ManageGuard {

  private final ResourceManager resourceManager;
  private final ResourcePublicationManager publications;

  public ManageGuard(ResourceManager resourceManager, ResourcePublicationManager publications) {
    this.resourceManager = resourceManager;
    this.publications = publications;
  }

  /**
   * @param lockedRedirect whether a resource being published sends the visitor to the locked page (not for the
   *                       publication's own status and cancel requests)
   * @return a result to show instead of the page, or null to go on (also when no resource is involved)
   */
  @Nullable
  public String check(HttpServletRequest request, @Nullable User user, boolean lockedRedirect) {
    boolean fromParameter = request.getParameterMap().containsKey(Constants.REQ_PARAM_RESOURCE);
    HttpSession session = request.getSession(false);
    String requested = fromParameter
        ? StringUtils.trimToNull(request.getParameter(Constants.REQ_PARAM_RESOURCE))
        : session == null ? null : (String) session.getAttribute(Constants.SESSION_RESOURCE);
    if (requested == null) {
      return fromParameter ? ActionPage.NOT_FOUND : null;
    }
    Resource resource = resourceManager.get(requested);
    if (resource == null || !ResourceAccessPolicy.canManage(user, resource)) {
      if (!fromParameter && session != null) {
        session.removeAttribute(Constants.SESSION_RESOURCE);
      }
      return resource == null ? ActionPage.NOT_FOUND : ActionPage.NOT_ALLOWED;
    }
    request.getSession().setAttribute(Constants.SESSION_RESOURCE, resource.getShortname());
    if (lockedRedirect && publications.isLocked(resource.getShortname())) {
      return ActionPage.LOCKED;
    }
    return null;
  }
}
