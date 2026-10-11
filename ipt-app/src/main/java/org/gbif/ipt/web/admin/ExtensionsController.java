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
package org.gbif.ipt.web.admin;

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

/** IPT 3's ExtensionsAction, VocabulariesAction and DataPackageSchemaAction. */
@Controller
public class ExtensionsController extends AdminPageController {

  private static final String EXTENSIONS = "redirect:/admin/extensions.do";
  private static final String SCHEMAS = "redirect:/admin/dataPackages.do";

  private final ObjectProvider<ExtensionsPage> extensionPages;
  private final ObjectProvider<VocabulariesPage> vocabularyPages;
  private final ObjectProvider<DataPackageSchemaPage> schemaPages;

  public ExtensionsController(ActionRunner runner, ObjectProvider<ExtensionsPage> extensionPages,
                              ObjectProvider<VocabulariesPage> vocabularyPages,
                              ObjectProvider<DataPackageSchemaPage> schemaPages) {
    super(runner);
    this.extensionPages = extensionPages;
    this.vocabularyPages = vocabularyPages;
    this.schemaPages = schemaPages;
  }

  /** The list; its synchronise button posts. */
  @RequestMapping(path = "/admin/extensions", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView extensions(HttpServletRequest request) throws Exception {
    ExtensionsPage page = extensionPages.getObject();
    return admin(request, page, "list", page::list, Map.of(ActionPage.SUCCESS, "admin/extensions"));
  }

  @PostMapping("/admin/updateExtension")
  public ModelAndView updateExtension(HttpServletRequest request) throws Exception {
    ExtensionsPage page = extensionPages.getObject();
    return admin(request, page, "update", page::update, Map.of(ActionPage.SUCCESS, EXTENSIONS));
  }

  /** An extension's page; posts install ({@code url}) or remove ({@code id}, {@code deleteFlag}). */
  @RequestMapping(path = "/admin/extension", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView extension(HttpServletRequest request) throws Exception {
    ExtensionsPage page = extensionPages.getObject();
    return admin(request, page, "execute", page::execute,
        Map.of(ActionPage.INPUT, "admin/extension", ActionPage.SUCCESS, EXTENSIONS));
  }

  @GetMapping("/admin/vocabulary")
  public ModelAndView vocabulary(HttpServletRequest request) throws Exception {
    VocabulariesPage page = vocabularyPages.getObject();
    return admin(request, page, "execute", page::execute, Map.of(ActionPage.SUCCESS, "admin/vocabulary"));
  }

  @GetMapping("/admin/dataPackages")
  public ModelAndView dataPackages(HttpServletRequest request) throws Exception {
    DataPackageSchemaPage page = schemaPages.getObject();
    return admin(request, page, "list", page::list, Map.of(ActionPage.SUCCESS, "admin/dataPackageSchemas"));
  }

  @PostMapping("/admin/updateDataPackage")
  public ModelAndView updateDataPackage(HttpServletRequest request) throws Exception {
    DataPackageSchemaPage page = schemaPages.getObject();
    return admin(request, page, "update", page::update, Map.of(ActionPage.SUCCESS, SCHEMAS));
  }

  @RequestMapping(path = "/admin/dataPackage", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView dataPackage(HttpServletRequest request) throws Exception {
    DataPackageSchemaPage page = schemaPages.getObject();
    return admin(request, page, "execute", page::execute,
        Map.of(ActionPage.INPUT, "admin/dataPackageSchema", ActionPage.SUCCESS, SCHEMAS));
  }
}
