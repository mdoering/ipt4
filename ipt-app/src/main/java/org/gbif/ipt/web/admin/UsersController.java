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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IPT 3's UserAccountsAction: the user list and the user form. IPT 3 declared {@code user} twice; Struts used the
 * second declaration (the form's save, delete and cancel).
 */
@Controller
public class UsersController extends AdminPageController {

  private static final String USERS = "redirect:/admin/users.do";

  private final ObjectProvider<UserAccountsPage> pages;

  public UsersController(ActionRunner runner, ObjectProvider<UserAccountsPage> pages) {
    super(runner);
    this.pages = pages;
  }

  @GetMapping("/admin/users")
  public ModelAndView users(HttpServletRequest request) throws Exception {
    UserAccountsPage page = pages.getObject();
    return admin(request, page, "list", page::list, Map.of(ActionPage.SUCCESS, "admin/users"));
  }

  @RequestMapping(path = "/admin/user", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView user(HttpServletRequest request) throws Exception {
    UserAccountsPage page = pages.getObject();
    return admin(request, page, "execute", page::execute,
        Map.of(ActionPage.INPUT, "admin/user", ActionPage.SUCCESS, USERS, ActionPage.CANCEL, USERS));
  }
}
