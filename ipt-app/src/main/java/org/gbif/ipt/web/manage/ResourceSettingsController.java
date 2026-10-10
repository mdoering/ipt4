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

import org.gbif.ipt.web.portal.ResourcePage;
import org.gbif.ipt.web.support.CurrentUser;
import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.web.support.action.ActionRunner;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IPT 3's PublicationSettingsAction, AutoPublishAction, VersionHistoryAction and the managers' view of the resource
 * page ({@code manage/detail}, also for private and unpublished versions).
 */
@Controller
public class ResourceSettingsController extends ManagePageController {

  private static final String TO_OVERVIEW = "redirect:/manage/resource.do?r={r}";

  private final ObjectProvider<PublicationSettingsPage> settingsPages;
  private final ObjectProvider<AutoPublishPage> autoPublishPages;
  private final ObjectProvider<VersionHistoryPage> historyPages;
  private final ObjectProvider<ResourcePage> resourcePages;

  public ResourceSettingsController(ActionRunner runner, ManageGuard guard, CurrentUser currentUser,
                                    ObjectProvider<PublicationSettingsPage> settingsPages,
                                    ObjectProvider<AutoPublishPage> autoPublishPages,
                                    ObjectProvider<VersionHistoryPage> historyPages,
                                    ObjectProvider<ResourcePage> resourcePages) {
    super(runner, guard, currentUser);
    this.settingsPages = settingsPages;
    this.autoPublishPages = autoPublishPages;
    this.historyPages = historyPages;
    this.resourcePages = resourcePages;
  }

  @RequestMapping(path = "/manage/publication-settings", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView publicationSettings(HttpServletRequest request) throws Exception {
    PublicationSettingsPage page = settingsPages.getObject();
    return manage(request, page, "execute", page::execute, Map.of(
        ActionPage.INPUT, "manage/publicationSettings",
        ActionPage.SUCCESS, TO_OVERVIEW, ActionPage.CANCEL, TO_OVERVIEW, ActionPage.ERROR, TO_OVERVIEW));
  }

  @RequestMapping(path = "/manage/auto-publish", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView autoPublish(HttpServletRequest request) throws Exception {
    AutoPublishPage page = autoPublishPages.getObject();
    return manage(request, page, "execute", page::execute, Map.of(
        ActionPage.INPUT, "manage/autopublish",
        ActionPage.SUCCESS, TO_OVERVIEW, ActionPage.CANCEL, TO_OVERVIEW, ActionPage.ERROR, TO_OVERVIEW));
  }

  @RequestMapping(path = "/manage/history", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView history(HttpServletRequest request) throws Exception {
    VersionHistoryPage page = historyPages.getObject();
    return manage(request, page, "execute", page::execute, Map.of(
        ActionPage.INPUT, "manage/version_history",
        ActionPage.SUCCESS, "redirect:/manage/detail.do?r={r}&v={versionString}",
        "back", "redirect:/manage/detail.do?r={r}"));
  }

  @GetMapping("/manage/detail")
  public ModelAndView detail(HttpServletRequest request) throws Exception {
    ResourcePage page = resourcePages.getObject();
    return manage(request, page, "detail", page::detail, Map.of(ActionPage.SUCCESS, "portal/resource_new"));
  }
}
