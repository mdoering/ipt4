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

import org.gbif.ipt.web.portal.ResourceFilePage;
import org.gbif.ipt.web.support.CurrentUser;
import org.gbif.ipt.web.support.FileStreams;
import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.web.support.action.ActionRunner;

import java.io.InputStream;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** IPT 3's SourceAction (add, edit and analyze, peek, delete) and the raw source download. */
@Controller
public class SourceController extends ManagePageController {

  private static final String TO_OVERVIEW = "redirect:/manage/resource.do?r={r}";
  private static final View NO_BODY = (model, request, response) -> {
  };

  private final ObjectProvider<SourcePage> pages;
  private final ObjectProvider<ResourceFilePage> filePages;

  public SourceController(ActionRunner runner, ManageGuard guard, CurrentUser currentUser,
                          ObjectProvider<SourcePage> pages, ObjectProvider<ResourceFilePage> filePages) {
    super(runner, guard, currentUser);
    this.pages = pages;
    this.filePages = filePages;
  }

  @RequestMapping(path = "/manage/source", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView source(HttpServletRequest request) throws Exception {
    SourcePage page = pages.getObject();
    return manage(request, page, "execute", page::execute, Map.of(
        ActionPage.INPUT, "manage/source",
        "analyze", "redirect:/manage/source.do?r={r}&id={param:id}",
        ActionPage.SUCCESS, TO_OVERVIEW,
        ActionPage.CANCEL, TO_OVERVIEW));
  }

  /** The file upload posts with XHR and reads X-Error-Message on IPT 3's bare 400. */
  @PostMapping("/manage/addsource")
  public ModelAndView add(HttpServletRequest request) throws Exception {
    SourcePage page = pages.getObject();
    ModelAndView mav = manage(request, page, "add", page::add, Map.of(
        ActionPage.SUCCESS, "redirect:/manage/source.do?r={r}&id={id}",
        ActionPage.INPUT, TO_OVERVIEW,
        ActionPage.ERROR, "error-400"));
    if ("error-400".equals(mav.getViewName())) {
      ModelAndView bare = new ModelAndView(NO_BODY);
      bare.setStatus(HttpStatus.BAD_REQUEST);
      return bare;
    }
    return mav;
  }

  /** IPT 3 mapped this to a method its SourceAction did not have; the form only leads back to the overview. */
  @PostMapping("/manage/canceloverwrite")
  public ModelAndView cancelOverwrite(HttpServletRequest request) throws Exception {
    SourcePage page = pages.getObject();
    return manage(request, page, "cancelOverwrite", () -> ActionPage.SUCCESS,
        Map.of(ActionPage.SUCCESS, TO_OVERVIEW));
  }

  @PostMapping("/manage/delete-source")
  public ModelAndView delete(HttpServletRequest request) throws Exception {
    SourcePage page = pages.getObject();
    return manage(request, page, "delete", page::delete,
        Map.of(ActionPage.SUCCESS, TO_OVERVIEW, ActionPage.ERROR, TO_OVERVIEW));
  }

  /** IPT 3 ran this without any access check (ajaxStack). */
  @GetMapping("/manage/peek")
  public ModelAndView peek(HttpServletRequest request) throws Exception {
    SourcePage page = pages.getObject();
    return manage(request, page, "peek", page::peek, Map.of(ActionPage.SUCCESS, "manage/peek"), false);
  }

  @GetMapping("/manage/raw-source")
  public ModelAndView rawSource(HttpServletRequest request, HttpServletResponse response) throws Exception {
    ResourceFilePage page = filePages.getObject();
    ModelAndView mav = manage(request, page, "rawsource", page::rawsource, Map.of(ActionPage.SUCCESS, "stream"));
    try (InputStream in = page.getInputStream()) {
      if (!"stream".equals(mav.getViewName())) {
        return mav;
      }
      FileStreams.write(request, response, page.getData(), page.getMimeType(), page.getFilename(), in, false);
      return null;
    }
  }
}
