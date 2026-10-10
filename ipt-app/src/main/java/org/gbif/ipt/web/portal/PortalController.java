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
import org.gbif.ipt.service.manage.ResourceAccessPolicy;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.web.support.CurrentUser;
import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.web.support.action.ActionResults;
import org.gbif.ipt.web.support.action.ActionRunner;
import org.gbif.ipt.web.support.action.PageController;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** The public pages of IPT 3's struts-portal.xml and struts.xml: home, resource, preview, fallback, about, health, rss. */
@Controller
public class PortalController extends PageController {

  private static final Map<String, String> PORTAL = ActionResults.PORTAL;

  private final ObjectProvider<HomePage> homePages;
  private final ObjectProvider<ResourcePage> resourcePages;
  private final ObjectProvider<FallbackPage> fallbackPages;
  private final ObjectProvider<AboutPage> aboutPages;
  private final ObjectProvider<HealthPage> healthPages;
  private final PortalGuard guard;
  private final CurrentUser currentUser;
  private final ResourceManager resourceManager;

  public PortalController(ActionRunner runner, ObjectProvider<HomePage> homePages,
                          ObjectProvider<ResourcePage> resourcePages, ObjectProvider<FallbackPage> fallbackPages,
                          ObjectProvider<AboutPage> aboutPages, ObjectProvider<HealthPage> healthPages,
                          PortalGuard guard, CurrentUser currentUser, ResourceManager resourceManager) {
    super(runner);
    this.homePages = homePages;
    this.resourcePages = resourcePages;
    this.fallbackPages = fallbackPages;
    this.aboutPages = aboutPages;
    this.healthPages = healthPages;
    this.guard = guard;
    this.currentUser = currentUser;
    this.resourceManager = resourceManager;
  }

  @GetMapping({"/", "/home"})
  public ModelAndView home(HttpServletRequest request) throws Exception {
    HomePage page = homePages.getObject();
    return guarded(request, page, () -> run(page, request, "execute", page::execute,
        ActionResults.results(PORTAL, Map.of(ActionPage.SUCCESS, "portal/home", ActionPage.INPUT, "portal/home"))));
  }

  @GetMapping("/resource")
  public ModelAndView resource(HttpServletRequest request) throws Exception {
    ResourcePage page = resourcePages.getObject();
    return guarded(request, page, () -> run(page, request, "detail", page::detail,
        ActionResults.results(PORTAL, Map.of(ActionPage.SUCCESS, "portal/resource"))));
  }

  /** IPT 3 ran the preview through the manager stack: only managers of the resource may see it. */
  @GetMapping("/resource/preview")
  public ModelAndView preview(HttpServletRequest request) throws Exception {
    ResourcePage page = resourcePages.getObject();
    String shortname = request.getParameter(Constants.REQ_PARAM_RESOURCE);
    Resource resource = shortname == null ? null : resourceManager.get(shortname.trim());
    if (resource == null) {
      return render(page, ActionPage.NOT_FOUND, PORTAL);
    }
    if (!ResourceAccessPolicy.canManage(currentUser.get(), resource)) {
      return render(page, ActionPage.NOT_ALLOWED, PORTAL);
    }
    request.getSession().setAttribute(Constants.SESSION_RESOURCE, resource.getShortname());
    return run(page, request, "preview", page::preview,
        ActionResults.results(PORTAL, Map.of(ActionPage.SUCCESS, "portal/resource")));
  }

  @GetMapping("/fallback")
  public ModelAndView fallback(HttpServletRequest request) throws Exception {
    FallbackPage page = fallbackPages.getObject();
    return guarded(request, page, () -> run(page, request, "execute", page::execute,
        ActionResults.results(PORTAL,
            Map.of(ActionPage.SUCCESS, "portal/fallback", ActionPage.INPUT, "portal/fallback"))));
  }

  @GetMapping("/about")
  public ModelAndView about(HttpServletRequest request) throws Exception {
    AboutPage page = aboutPages.getObject();
    return guarded(request, page, () -> run(page, request, "execute", page::execute,
        Map.of(ActionPage.SUCCESS, "about")));
  }

  @GetMapping("/health")
  public ModelAndView health(HttpServletRequest request) throws Exception {
    HealthPage page = healthPages.getObject();
    return run(page, request, "execute", page::execute, Map.of(ActionPage.SUCCESS, "health"));
  }

  @GetMapping("/rss")
  public ModelAndView rss(HttpServletRequest request, HttpServletResponse response) throws Exception {
    ResourcePage page = resourcePages.getObject();
    response.setContentType("text/xml;charset=UTF-8");
    return run(page, request, "rss", page::rss, Map.of(ActionPage.SUCCESS, "portal/rss"));
  }

  @FunctionalInterface
  private interface Render {
    ModelAndView render() throws Exception;
  }

  private ModelAndView guarded(HttpServletRequest request, ActionPage page, Render render) throws Exception {
    String blocked = guard.check(request, currentUser.get());
    return blocked != null ? render(page, blocked, PORTAL) : render.render();
  }
}
