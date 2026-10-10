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
package org.gbif.ipt.web.setup;

import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.web.support.action.ActionRunner;
import org.gbif.ipt.web.support.action.PageController;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IPT 3's setup steps 2 to 5. Each step passes a visitor on once its own part is done, and back when an earlier step
 * is missing, so the wizard cannot be used to change an installed IPT.
 */
@Controller
@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
public class SetupController extends PageController {

  private final ObjectProvider<SetupPage> setupPages;

  public SetupController(ActionRunner runner, ObjectProvider<SetupPage> setupPages) {
    super(runner);
    this.setupPages = setupPages;
  }

  @RequestMapping("/setupDefaultAdministrator")
  public ModelAndView defaultAdministrator(HttpServletRequest request) throws Exception {
    SetupPage page = setupPages.getObject();
    return run(page, request, "setupDefaultAdministrator", page::setupDefaultAdministrator, Map.of(
        ActionPage.INPUT, "admin/setup_2_default_administrator",
        ActionPage.SUCCESS, "redirect:/setupMode.do"));
  }

  @RequestMapping("/setupMode")
  public ModelAndView mode(HttpServletRequest request) throws Exception {
    SetupPage page = setupPages.getObject();
    return run(page, request, "setupMode", page::setupMode, Map.of(
        ActionPage.INPUT, "admin/setup_3_mode",
        ActionPage.ERROR, "redirect:/setupDefaultAdministrator.do",
        ActionPage.SUCCESS, "redirect:/setupPublicUrl.do"));
  }

  @RequestMapping("/setupPublicUrl")
  public ModelAndView publicUrl(HttpServletRequest request) throws Exception {
    SetupPage page = setupPages.getObject();
    return run(page, request, "setupPublicUrl", page::setupPublicUrl, Map.of(
        ActionPage.INPUT, "admin/setup_4_public_url",
        ActionPage.ERROR, "redirect:/setupMode.do",
        ActionPage.SUCCESS, "redirect:/setupInstallationComplete.do"));
  }

  @RequestMapping("/setupInstallationComplete")
  public ModelAndView installationComplete(HttpServletRequest request) throws Exception {
    SetupPage page = setupPages.getObject();
    return run(page, request, "setupInstallationComplete", page::setupInstallationComplete, Map.of(
        ActionPage.INPUT, "admin/setup_5_complete",
        ActionPage.ERROR, "redirect:/setupPublicUrl.do"));
  }

  @RequestMapping("/setupComplete")
  public String complete() {
    return "redirect:/admin/";
  }
}
