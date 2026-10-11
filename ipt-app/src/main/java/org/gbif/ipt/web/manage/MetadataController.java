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

import org.gbif.ipt.model.voc.CamtrapMetadataSection;
import org.gbif.ipt.model.voc.FrictionlessMetadataSection;
import org.gbif.ipt.model.voc.MetadataSection;
import org.gbif.ipt.web.support.CurrentUser;
import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.web.support.action.ActionRunner;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IPT 3's metadata editors: the EML sections ({@code metadata-*}), the Camtrap DP sections
 * ({@code camtrap-metadata-*}), the Frictionless/ColDP form ({@code datapackage-metadata-*}) and the logo upload.
 * Only known sections are served (the section names a template).
 */
@Controller
public class MetadataController extends ManagePageController {

  private static final String TO_OVERVIEW = "redirect:/manage/resource.do?r={r}";

  private final ObjectProvider<MetadataPage> emlPages;
  private final ObjectProvider<CamtrapMetadataPage> camtrapPages;
  private final ObjectProvider<DataPackageMetadataPage> dataPackagePages;

  public MetadataController(ActionRunner runner, ManageGuard guard, CurrentUser currentUser,
                            ObjectProvider<MetadataPage> emlPages, ObjectProvider<CamtrapMetadataPage> camtrapPages,
                            ObjectProvider<DataPackageMetadataPage> dataPackagePages) {
    super(runner, guard, currentUser);
    this.emlPages = emlPages;
    this.camtrapPages = camtrapPages;
    this.dataPackagePages = dataPackagePages;
  }

  @RequestMapping(path = "/manage/metadata-{section}", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView eml(@PathVariable String section, HttpServletRequest request) throws Exception {
    MetadataPage page = emlPages.getObject();
    if (MetadataSection.fromName(section) == null) {
      return render(page, ActionPage.NOT_FOUND, Map.of(ActionPage.NOT_FOUND, "manage/404"));
    }
    return manage(request, page, "execute", page::execute, Map.of(
        ActionPage.INPUT, "manage/eml/" + MetadataSection.fromName(section).getName(),
        ActionPage.SUCCESS, "redirect:/manage/metadata-{next}.do?r={r}"));
  }

  @RequestMapping(path = "/manage/camtrap-metadata-{section}", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView camtrap(@PathVariable String section, HttpServletRequest request) throws Exception {
    CamtrapMetadataPage page = camtrapPages.getObject();
    if (CamtrapMetadataSection.fromName(section) == null) {
      return render(page, ActionPage.NOT_FOUND, Map.of(ActionPage.NOT_FOUND, "manage/404"));
    }
    return manage(request, page, "execute", page::execute, Map.of(
        ActionPage.INPUT, "manage/dp-metadata/camtrap/" + CamtrapMetadataSection.fromName(section).getName(),
        ActionPage.SUCCESS, "redirect:/manage/camtrap-metadata-{next}.do?r={r}"));
  }

  @RequestMapping(path = "/manage/datapackage-metadata-{section}", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView dataPackage(@PathVariable String section, HttpServletRequest request) throws Exception {
    DataPackageMetadataPage page = dataPackagePages.getObject();
    if (FrictionlessMetadataSection.fromName(section) == null) {
      return render(page, ActionPage.NOT_FOUND, Map.of(ActionPage.NOT_FOUND, "manage/404"));
    }
    return manage(request, page, "execute", page::execute, Map.of(
        ActionPage.INPUT, "manage/dp-metadata/frictionless/" + FrictionlessMetadataSection.fromName(section).getName(),
        ActionPage.SUCCESS, TO_OVERVIEW));
  }

  /** The logo upload of the additional metadata section; the form comes back with the logo or the error. */
  @PostMapping("/manage/uploadlogo")
  public ModelAndView uploadLogo(HttpServletRequest request) throws Exception {
    MetadataPage page = emlPages.getObject();
    return manage(request, page, "uploadLogo", page::uploadLogo, Map.of(
        ActionPage.INPUT, "redirect:/manage/metadata-additional.do?r={r}", ActionPage.ERROR, TO_OVERVIEW));
  }
}
