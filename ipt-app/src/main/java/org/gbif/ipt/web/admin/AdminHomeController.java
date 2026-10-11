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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/** IPT 3's admin {@code home} action, also the admin namespace's default action. */
@Controller
public class AdminHomeController extends AdminPageController {

  private final ObjectProvider<AdminHomePage> pages;

  public AdminHomeController(ActionRunner runner, ObjectProvider<AdminHomePage> pages) {
    super(runner);
    this.pages = pages;
  }

  @RequestMapping(path = {"/admin", "/admin/", "/admin/home"}, method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView home(HttpServletRequest request) throws Exception {
    AdminHomePage page = pages.getObject();
    return admin(request, page, "execute", page::execute,
        Map.of(ActionPage.SUCCESS, "admin/home", ActionPage.INPUT, "admin/home"));
  }
}
