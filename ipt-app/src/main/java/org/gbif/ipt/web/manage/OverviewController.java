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

import org.gbif.ipt.web.support.CurrentUser;
import org.gbif.ipt.web.support.FileStreams;
import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.web.support.action.ActionRunner;

import java.io.InputStream;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * IPT 3's resource overview ({@code OverviewAction}): the overview page, publishing and its status, visibility,
 * deletion, co-managers, metadata replacement and the registry/DOI actions of the {@code resource-*} wildcard.
 */
@Controller
public class OverviewController extends ManagePageController {

  private static final String OVERVIEW = "manage/overview";
  private static final String TO_OVERVIEW = "redirect:/manage/resource.do?r={r}";
  private static final String PUBLISHING = "publishing";

  private final ObjectProvider<OverviewPage> pages;
  private final ObjectProvider<ManageResourceFilePage> filePages;
  private final ObjectProvider<UrlMetadataPage> urlMetadataPages;

  public OverviewController(ActionRunner runner, ManageGuard guard, CurrentUser currentUser,
                            ObjectProvider<OverviewPage> pages, ObjectProvider<ManageResourceFilePage> filePages,
                            ObjectProvider<UrlMetadataPage> urlMetadataPages) {
    super(runner, guard, currentUser);
    this.pages = pages;
    this.filePages = filePages;
    this.urlMetadataPages = urlMetadataPages;
  }

  @RequestMapping(path = "/manage/resource", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView overview(HttpServletRequest request) throws Exception {
    OverviewPage page = pages.getObject();
    return manage(request, page, "execute", page::execute,
        Map.of(ActionPage.SUCCESS, OVERVIEW, ActionPage.INPUT, OVERVIEW));
  }

  @PostMapping("/manage/publish")
  public ModelAndView publish(HttpServletRequest request) throws Exception {
    OverviewPage page = pages.getObject();
    return manage(request, page, "publish", page::publish, Map.of(PUBLISHING, "manage/locked",
        ActionPage.SUCCESS, OVERVIEW, ActionPage.INPUT, OVERVIEW, ActionPage.ERROR, OVERVIEW));
  }

  /** IPT 3 ran this without any access check (ajaxStack), and on GET; it stops a publication, so it is a POST. */
  @PostMapping("/manage/cancel")
  public ModelAndView cancel(HttpServletRequest request) throws Exception {
    OverviewPage page = pages.getObject();
    return manage(request, page, "cancel", page::cancel,
        Map.of(ActionPage.SUCCESS, TO_OVERVIEW, ActionPage.ERROR, "manage/locked"), false);
  }

  @GetMapping("/manage/locked")
  public ModelAndView locked(HttpServletRequest request) throws Exception {
    OverviewPage page = pages.getObject();
    return manage(request, page, "locked", page::locked, Map.of(ActionPage.SUCCESS, "manage/locked"), false);
  }

  /** The publication status the locked page polls; IPT 3 ran it without any access check (ajaxStack). */
  @GetMapping("/manage/report")
  public ModelAndView report(HttpServletRequest request) throws Exception {
    OverviewPage page = pages.getObject();
    return manage(request, page, "locked", page::locked,
        Map.of(ActionPage.SUCCESS, "manage/report", ActionPage.CANCEL, "manage/report"), false);
  }

  /** IPT 3 ran this without any access check (ajaxStack). */
  @GetMapping("/manage/mappingPeek")
  public ModelAndView mappingPeek(HttpServletRequest request) throws Exception {
    OverviewPage page = pages.getObject();
    return manage(request, page, "peek", page::peek, Map.of(ActionPage.SUCCESS, "manage/peek"), false);
  }

  /**
   * IPT 3's {@code resource-*} wildcard with its allowed methods (its {@code changePublishingOrganization} had no
   * method in the baseline); state changes are POSTs.
   */
  @PostMapping("/manage/resource-{method}")
  public ModelAndView resourceAction(@PathVariable String method, HttpServletRequest request) throws Exception {
    OverviewPage page = pages.getObject();
    ActionRunner.ActionMethod action = switch (method) {
      case "addNetwork" -> page::addNetwork;
      case "addManager" -> page::addManager;
      case "delete" -> page::delete;
      case "deleteFromIpt" -> page::deleteFromIpt;
      case "deleteDoi" -> page::deleteDoi;
      case "deleteNetwork" -> page::deleteNetwork;
      case "deleteManager" -> page::deleteManager;
      case "makePrivate" -> page::makePrivate;
      case "makePublic" -> page::makePublic;
      case "cancelVisibilityChange" -> page::cancelVisibilityChange;
      case "registerResource" -> page::registerResource;
      case "updateDwcDpDatasetType" -> page::updateDwcDpDatasetType;
      case "reserveDoi" -> page::reserveDoi;
      case "undelete" -> page::undelete;
      default -> null;
    };
    if (action == null) {
      return render(page, ActionPage.NOT_FOUND, Map.of(ActionPage.NOT_FOUND, "manage/404"));
    }
    return manage(request, page, method, action,
        Map.of(ActionPage.INPUT, OVERVIEW, ActionPage.SUCCESS, TO_OVERVIEW, ActionPage.ERROR, TO_OVERVIEW));
  }

  @PostMapping("/manage/replace-eml")
  public ModelAndView replaceEml(HttpServletRequest request) throws Exception {
    OverviewPage page = pages.getObject();
    return manage(request, page, "replaceEml", page::replaceEml, Map.of(
        ActionPage.INPUT, OVERVIEW, ActionPage.SUCCESS, TO_OVERVIEW, ActionPage.ERROR, TO_OVERVIEW));
  }

  @PostMapping("/manage/replace-datapackage-metadata")
  public ModelAndView replaceDatapackageMetadata(HttpServletRequest request) throws Exception {
    OverviewPage page = pages.getObject();
    return manage(request, page, "replaceDatapackageMetadata", page::replaceDatapackageMetadata, Map.of(
        ActionPage.INPUT, OVERVIEW, ActionPage.SUCCESS, TO_OVERVIEW, ActionPage.ERROR, TO_OVERVIEW));
  }

  /** The resource's current (unpublished) metadata file. */
  @GetMapping("/manage/eml")
  public ModelAndView eml(HttpServletRequest request, HttpServletResponse response) throws Exception {
    ManageResourceFilePage page = filePages.getObject();
    ModelAndView mav = manage(request, page, "metadata", page::metadata, Map.of(ActionPage.SUCCESS, "stream"));
    try (InputStream in = page.getInputStream()) {
      if (!"stream".equals(mav.getViewName())) {
        return mav;
      }
      FileStreams.write(request, response, page.getData(), page.getMimeType(), page.getFilename(), in, true);
      return null;
    }
  }

  /** Header facts about a URL source's URL, for the add source form. */
  @GetMapping("/manage/urlMetadata")
  public void urlMetadata(HttpServletRequest request, HttpServletResponse response) {
    UrlMetadataPage page = urlMetadataPages.getObject();
    page.setUrl(request.getParameter("url"));
    page.execute(response);
  }
}
