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

import org.gbif.ipt.web.support.FileStreams;
import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.web.support.action.ActionRunner;

import java.io.InputStream;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** IPT 3's ConfigAction, UIManagementAction and LogsAction. */
@Controller
public class SettingsController extends AdminPageController {

  private static final String UI = "redirect:/admin/uiManagement.do";

  private final ObjectProvider<ConfigPage> configPages;
  private final ObjectProvider<UIManagementPage> uiPages;
  private final ObjectProvider<LogsPage> logsPages;

  public SettingsController(ActionRunner runner, ObjectProvider<ConfigPage> configPages,
                            ObjectProvider<UIManagementPage> uiPages, ObjectProvider<LogsPage> logsPages) {
    super(runner);
    this.configPages = configPages;
    this.uiPages = uiPages;
    this.logsPages = logsPages;
  }

  /** IPT 3 redirected a saved configuration to {@code /config.do}, which its portal answered with the home page. */
  @RequestMapping(path = "/admin/config", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView config(HttpServletRequest request) throws Exception {
    ConfigPage page = configPages.getObject();
    return admin(request, page, "execute", page::execute, Map.of(ActionPage.INPUT, "admin/config",
        ActionPage.SUCCESS, "redirect:/admin/config.do", ActionPage.HOME, "redirect:/home.do"));
  }

  @RequestMapping(path = "/admin/uiManagement", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView uiManagement(HttpServletRequest request) throws Exception {
    UIManagementPage page = uiPages.getObject();
    return admin(request, page, "execute", page::execute,
        Map.of(ActionPage.INPUT, "admin/ui_management", ActionPage.SUCCESS, UI));
  }

  /**
   * The logo upload of the UI management page (an iframe upload whose answer the page ignores). IPT 3's results
   * pointed at the manage area.
   */
  @PostMapping("/admin/appLogoUpload")
  public ModelAndView appLogoUpload(HttpServletRequest request) throws Exception {
    UIManagementPage page = uiPages.getObject();
    return admin(request, page, "uploadLogo", page::uploadLogo,
        Map.of(ActionPage.INPUT, UI, ActionPage.SUCCESS, UI, ActionPage.ERROR, UI));
  }

  @GetMapping("/admin/logs")
  public ModelAndView logs(HttpServletRequest request) throws Exception {
    LogsPage page = logsPages.getObject();
    return admin(request, page, "execute", page::execute, Map.of(ActionPage.SUCCESS, "admin/logs"));
  }

  @GetMapping("/admin/logfile")
  public ModelAndView logfile(HttpServletRequest request, HttpServletResponse response) throws Exception {
    LogsPage page = logsPages.getObject();
    ModelAndView mav = admin(request, page, "logfile", page::logfile, Map.of(ActionPage.SUCCESS, "stream"));
    try (InputStream in = page.getInputStream()) {
      if (!"stream".equals(mav.getViewName())) {
        return mav;
      }
      FileStreams.write(request, response, page.getLogFile(), "text/plain", page.getLogFile().getName(), in, true);
      return null;
    }
  }
}
