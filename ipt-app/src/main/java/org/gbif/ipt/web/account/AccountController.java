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
package org.gbif.ipt.web.account;

import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.web.support.action.ActionRunner;
import org.gbif.ipt.web.support.action.PageController;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IPT 3's login, account and change-password actions. Spring Security handles the posted login form and logout;
 * a failed login is forwarded here to show the form again.
 */
@Controller
public class AccountController extends PageController {

  private static final Map<String, String> ACCOUNT = Map.of(
      ActionPage.INPUT, "account",
      ActionPage.CANCEL, "redirect:/home.do",
      ActionPage.SUCCESS, "redirect:/account.do");

  private final ObjectProvider<LoginPage> loginPages;
  private final ObjectProvider<AccountPage> accountPages;

  public AccountController(ActionRunner runner, ObjectProvider<LoginPage> loginPages,
                           ObjectProvider<AccountPage> accountPages) {
    super(runner);
    this.loginPages = loginPages;
    this.accountPages = accountPages;
  }

  @RequestMapping(path = "/login", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView login(HttpServletRequest request) throws Exception {
    LoginPage page = loginPages.getObject();
    String result = runner.run(page, request, "login", page::login);
    if (ActionPage.SUCCESS.equals(result)) {
      return new ModelAndView("redirect:" + page.getRedirectUrl());
    }
    return render(page, result, Map.of(ActionPage.INPUT, "login"));
  }

  @RequestMapping(path = "/account", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView account(HttpServletRequest request) throws Exception {
    AccountPage page = accountPages.getObject();
    return run(page, request, "execute", page::execute, ACCOUNT);
  }

  @PostMapping("/change-password")
  public ModelAndView changePassword(HttpServletRequest request) throws Exception {
    AccountPage page = accountPages.getObject();
    return run(page, request, "changePassword", page::changePassword, ACCOUNT);
  }
}
