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

/** IPT 3's DataPackageMappingAction, DataPackageFieldTranslationAction and ValidationReportAction. */
@Controller
public class DataPackageMappingController extends ManagePageController {

  private static final String TO_OVERVIEW = "redirect:/manage/resource.do?r={r}";
  private static final String MAPPING = "manage/dataPackageMapping";
  private static final String SOURCE_SELECTOR = "manage/dataPackageMappingSourceSelector";
  private static final String TRANSLATION = "manage/dataPackageTranslation";

  private final ObjectProvider<DataPackageMappingPage> mappingPages;
  private final ObjectProvider<DataPackageFieldTranslationPage> translationPages;
  private final ObjectProvider<ValidationReportPage> reportPages;

  public DataPackageMappingController(ActionRunner runner, ManageGuard guard, CurrentUser currentUser,
                                      ObjectProvider<DataPackageMappingPage> mappingPages,
                                      ObjectProvider<DataPackageFieldTranslationPage> translationPages,
                                      ObjectProvider<ValidationReportPage> reportPages) {
    super(runner, guard, currentUser);
    this.mappingPages = mappingPages;
    this.translationPages = translationPages;
    this.reportPages = reportPages;
  }

  @RequestMapping(path = "/manage/dataPackageMapping", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView mapping(HttpServletRequest request) throws Exception {
    DataPackageMappingPage page = mappingPages.getObject();
    return manage(request, page, "execute", page::execute, Map.of(
        ActionPage.INPUT, MAPPING,
        "source", SOURCE_SELECTOR,
        ActionPage.SUCCESS, TO_OVERVIEW,
        ActionPage.CANCEL, TO_OVERVIEW,
        ActionPage.ERROR, TO_OVERVIEW));
  }

  @RequestMapping(path = "/manage/dataPackageMappingSourceNew", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView sourceNew(HttpServletRequest request) throws Exception {
    DataPackageMappingPage page = mappingPages.getObject();
    return manage(request, page, "source", page::source,
        Map.of(ActionPage.INPUT, SOURCE_SELECTOR, ActionPage.CANCEL, TO_OVERVIEW));
  }

  @RequestMapping(path = "/manage/dataPackageMappingSourceCreate", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView sourceCreate(HttpServletRequest request) throws Exception {
    DataPackageMappingPage page = mappingPages.getObject();
    return manage(request, page, "create", page::create, Map.of(
        ActionPage.INPUT, MAPPING, ActionPage.SUCCESS, TO_OVERVIEW, ActionPage.CANCEL, TO_OVERVIEW));
  }

  @PostMapping("/manage/deleteDataPackageMapping")
  public ModelAndView delete(HttpServletRequest request) throws Exception {
    DataPackageMappingPage page = mappingPages.getObject();
    return manage(request, page, "delete", page::delete,
        Map.of(ActionPage.SUCCESS, TO_OVERVIEW, ActionPage.ERROR, TO_OVERVIEW));
  }

  @GetMapping("/manage/validationReport")
  public ModelAndView validationReport(HttpServletRequest request) throws Exception {
    ValidationReportPage page = reportPages.getObject();
    return manage(request, page, "execute", page::execute,
        Map.of(ActionPage.SUCCESS, "manage/dp_validation_report"));
  }

  @RequestMapping(path = "/manage/dataPackageFieldTranslation", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView translation(HttpServletRequest request) throws Exception {
    DataPackageFieldTranslationPage page = translationPages.getObject();
    return manage(request, page, "execute", page::execute, Map.of(
        ActionPage.SUCCESS, TRANSLATION,
        ActionPage.INPUT, TRANSLATION,
        ActionPage.NONE, "redirect:/manage/dataPackageMapping.do?r={r}&id={param:id}&mid={mid}"));
  }

  @RequestMapping(path = "/manage/dataPackageFieldTranslationReload",
      method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView translationReload(HttpServletRequest request) throws Exception {
    DataPackageFieldTranslationPage page = translationPages.getObject();
    return manage(request, page, "reload", page::reload, Map.of(ActionPage.SUCCESS, TRANSLATION));
  }

  @RequestMapping(path = "/manage/dataPackageFieldTranslationAutomap",
      method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView translationAutomap(HttpServletRequest request) throws Exception {
    DataPackageFieldTranslationPage page = translationPages.getObject();
    return manage(request, page, "automap", page::automap, Map.of(ActionPage.SUCCESS, TRANSLATION));
  }
}
