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

import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.web.support.CurrentUser;
import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.web.support.action.ActionRunner;
import org.gbif.ipt.web.support.action.PageBean;
import org.gbif.ipt.web.support.page.PageContext;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/** Test-only manage page: redirects to /_manage/ok with the resource it worked on. */
@Controller
@Profile("manage-probe")
public class ManageProbeController extends ManagePageController {

  /** A manage page with nothing of its own. */
  @PageBean
  @Profile("manage-probe")
  public static class ProbePage extends ManagerBasePage {
    public ProbePage(PageContext ctx, ResourceManager resourceManager) {
      super(ctx, resourceManager);
    }
  }

  private final ObjectProvider<ProbePage> pages;

  public ManageProbeController(ActionRunner runner, ManageGuard guard, CurrentUser currentUser,
                               ObjectProvider<ProbePage> pages) {
    super(runner, guard, currentUser);
    this.pages = pages;
  }

  @RequestMapping("/manage/_probe")
  public ModelAndView probe(HttpServletRequest request) throws Exception {
    ProbePage page = pages.getObject();
    return manage(request, page, "execute", () -> page.getResource() == null ? ActionPage.NOT_FOUND
        : ActionPage.SUCCESS, Map.of(ActionPage.SUCCESS, "redirect:/manage/_ok?r={r}&x={param:x}"));
  }
}
