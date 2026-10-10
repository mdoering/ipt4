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

import org.gbif.ipt.web.admin.VocabulariesPage;
import org.gbif.ipt.web.support.CurrentUser;
import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.web.support.action.ActionRunner;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/** IPT 3's MappingAction, TranslationAction and the vocabulary popup of the manage area. */
@Controller
public class MappingController extends ManagePageController {

  private static final String TO_OVERVIEW = "redirect:/manage/resource.do?r={r}";
  private static final String TRANSLATION = "manage/translation";

  private final ObjectProvider<MappingPage> mappingPages;
  private final ObjectProvider<TranslationPage> translationPages;
  private final ObjectProvider<VocabulariesPage> vocabularyPages;

  public MappingController(ActionRunner runner, ManageGuard guard, CurrentUser currentUser,
                           ObjectProvider<MappingPage> mappingPages,
                           ObjectProvider<TranslationPage> translationPages,
                           ObjectProvider<VocabulariesPage> vocabularyPages) {
    super(runner, guard, currentUser);
    this.mappingPages = mappingPages;
    this.translationPages = translationPages;
    this.vocabularyPages = vocabularyPages;
  }

  @RequestMapping(path = "/manage/mapping", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView mapping(HttpServletRequest request) throws Exception {
    MappingPage page = mappingPages.getObject();
    return manage(request, page, "execute", page::execute, Map.of(
        ActionPage.INPUT, "manage/mapping",
        "source", "manage/mappingSource",
        "save", "redirect:/manage/mapping.do?r={r}&id={param:id}&mid={mid}",
        ActionPage.SUCCESS, TO_OVERVIEW,
        ActionPage.CANCEL, TO_OVERVIEW,
        ActionPage.ERROR, TO_OVERVIEW));
  }

  @PostMapping("/manage/delete-mapping")
  public ModelAndView deleteMapping(HttpServletRequest request) throws Exception {
    MappingPage page = mappingPages.getObject();
    return manage(request, page, "delete", page::delete,
        Map.of(ActionPage.SUCCESS, TO_OVERVIEW, ActionPage.ERROR, TO_OVERVIEW));
  }

  @RequestMapping(path = "/manage/translation", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView translation(HttpServletRequest request) throws Exception {
    TranslationPage page = translationPages.getObject();
    return manage(request, page, "execute", page::execute, Map.of(
        ActionPage.SUCCESS, TRANSLATION,
        ActionPage.INPUT, TRANSLATION,
        ActionPage.NONE, "redirect:/manage/mapping.do?r={r}&id={id}&mid={mid}"));
  }

  @RequestMapping(path = "/manage/translationReload", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView translationReload(HttpServletRequest request) throws Exception {
    TranslationPage page = translationPages.getObject();
    return manage(request, page, "reload", page::reload, Map.of(ActionPage.SUCCESS, TRANSLATION));
  }

  @RequestMapping(path = "/manage/translationAutomap", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView translationAutomap(HttpServletRequest request) throws Exception {
    TranslationPage page = translationPages.getObject();
    return manage(request, page, "automap", page::automap, Map.of(ActionPage.SUCCESS, TRANSLATION));
  }

  /** IPT 3 ran the admin's VocabulariesAction here, for the vocabulary popup of the mapping page. */
  @GetMapping("/manage/vocabulary")
  public ModelAndView vocabulary(HttpServletRequest request) throws Exception {
    VocabulariesPage page = vocabularyPages.getObject();
    return manage(request, page, "execute", page::execute, Map.of(ActionPage.SUCCESS, "manage/vocabulary"));
  }
}
