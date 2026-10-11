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

import org.gbif.ipt.model.datatable.DatatableResult;
import org.gbif.ipt.web.support.CurrentUser;
import org.gbif.ipt.web.support.StrutsJson;
import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.web.support.action.ActionResults;
import org.gbif.ipt.web.support.action.ActionRunner;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/** IPT 3's manage home, resource creation and the manager API (JSON). */
@Controller
public class ManageHomeController extends ManagePageController {

  private final ObjectProvider<ManageHomePage> homePages;
  private final ObjectProvider<CreateResourcePage> createPages;
  private final ObjectProvider<MetadataAgentSuggesterPage> suggesterPages;

  public ManageHomeController(ActionRunner runner, ManageGuard guard, CurrentUser currentUser,
                              ObjectProvider<ManageHomePage> homePages,
                              ObjectProvider<CreateResourcePage> createPages,
                              ObjectProvider<MetadataAgentSuggesterPage> suggesterPages) {
    super(runner, guard, currentUser);
    this.homePages = homePages;
    this.createPages = createPages;
    this.suggesterPages = suggesterPages;
  }

  @GetMapping({"/manage", "/manage/", "/manage/home"})
  public ModelAndView home(HttpServletRequest request) throws Exception {
    ManageHomePage page = homePages.getObject();
    return manage(request, page, "execute", page::execute, Map.of(ActionPage.SUCCESS, "manage/home"));
  }

  @RequestMapping(path = "/manage/create", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView create(HttpServletRequest request) throws Exception {
    CreateResourcePage page = createPages.getObject();
    return manage(request, page, "execute", page::execute, Map.of(
        ActionPage.INPUT, "manage/create_input",
        ActionPage.SUCCESS, "redirect:/manage/resource.do?r={shortname}"));
  }

  @GetMapping("/manager-api/resources")
  @ResponseBody
  public ResponseEntity<byte[]> resources(HttpServletRequest request) throws Exception {
    ManageHomePage page = homePages.getObject();
    String result;
    try {
      result = guarded(request, page, "execute", page::execute);
    } catch (NumberFormatException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
    if (!ActionPage.SUCCESS.equals(result)) {
      return status(result);
    }
    DatatableResult table = page.getResources();
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("aaData", table.getData());
    root.put("iTotalDisplayRecords", table.getTotalDisplayRecords());
    root.put("iTotalRecords", table.getTotalRecords());
    return StrutsJson.json(root);
  }

  @GetMapping("/manager-api/suggest-resources")
  @ResponseBody
  public ResponseEntity<byte[]> suggestResources(HttpServletRequest request) throws Exception {
    MetadataAgentSuggesterPage page = suggesterPages.getObject();
    String result = guarded(request, page, "execute", page::execute);
    return ActionPage.SUCCESS.equals(result) ? StrutsJson.json(page.getSuggestedResources()) : status(result);
  }

  @GetMapping("/manager-api/suggest-agents")
  @ResponseBody
  public ResponseEntity<byte[]> suggestAgents(HttpServletRequest request) throws Exception {
    MetadataAgentSuggesterPage page = suggesterPages.getObject();
    String result = guarded(request, page, "execute", page::execute);
    return ActionPage.SUCCESS.equals(result) ? StrutsJson.json(page.getSuggestedAgents()) : status(result);
  }

  /** The manager stack of IPT 3's JSON package: the guard's result, else the page's. */
  private String guarded(HttpServletRequest request, ActionPage page, String methodName,
                         ActionRunner.ActionMethod method) throws Exception {
    String blocked = guard.check(request, currentUser.get(), false, false);
    return blocked != null ? blocked : runner.run(page, request, methodName, method);
  }

  private static ResponseEntity<byte[]> status(String result) {
    HttpStatus status = switch (result) {
      case ActionPage.NOT_FOUND -> HttpStatus.NOT_FOUND;
      case ActionPage.NOT_ALLOWED -> HttpStatus.UNAUTHORIZED;
      default -> HttpStatus.BAD_REQUEST;
    };
    return ResponseEntity.status(status).build();
  }
}
