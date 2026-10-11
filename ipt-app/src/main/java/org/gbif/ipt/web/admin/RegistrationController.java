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

/** IPT 3's OrganisationsAction and RegistrationAction. A finished registration step goes to the portal home. */
@Controller
public class RegistrationController extends AdminPageController {

  private static final String ORGANISATIONS = "redirect:/admin/organisations.do";
  private static final String HOME = "redirect:/home.do";
  private static final String REGISTRATION = "admin/registration";

  private final ObjectProvider<OrganisationsPage> organisationPages;
  private final ObjectProvider<RegistrationPage> registrationPages;

  public RegistrationController(ActionRunner runner, ObjectProvider<OrganisationsPage> organisationPages,
                                ObjectProvider<RegistrationPage> registrationPages) {
    super(runner);
    this.organisationPages = organisationPages;
    this.registrationPages = registrationPages;
  }

  @GetMapping("/admin/organisations")
  public ModelAndView organisations(HttpServletRequest request) throws Exception {
    OrganisationsPage page = organisationPages.getObject();
    return admin(request, page, "list", page::list, Map.of(ActionPage.SUCCESS, "admin/organisations"));
  }

  @PostMapping("/admin/organisationsSynchronize")
  public ModelAndView synchronize(HttpServletRequest request) throws Exception {
    OrganisationsPage page = organisationPages.getObject();
    return admin(request, page, "synchronize", page::synchronize,
        Map.of(ActionPage.SUCCESS, "admin/organisations", ActionPage.INPUT, "admin/organisations"));
  }

  @RequestMapping(path = "/admin/organisation", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView organisation(HttpServletRequest request) throws Exception {
    OrganisationsPage page = organisationPages.getObject();
    return admin(request, page, "execute", page::execute, Map.of(ActionPage.INPUT, "admin/organisation",
        ActionPage.SUCCESS, ORGANISATIONS, ActionPage.CANCEL, ORGANISATIONS));
  }

  @RequestMapping(path = "/admin/registration", method = {RequestMethod.GET, RequestMethod.POST})
  public ModelAndView registration(HttpServletRequest request) throws Exception {
    RegistrationPage page = registrationPages.getObject();
    return admin(request, page, "execute", page::execute,
        Map.of(ActionPage.INPUT, REGISTRATION, ActionPage.SUCCESS, HOME));
  }

  @PostMapping("/admin/updateRegistration")
  public ModelAndView updateRegistration(HttpServletRequest request) throws Exception {
    RegistrationPage page = registrationPages.getObject();
    return admin(request, page, "update", page::update, Map.of(ActionPage.INPUT, REGISTRATION, ActionPage.SUCCESS, HOME));
  }

  @PostMapping("/admin/changeTokens")
  public ModelAndView changeTokens(HttpServletRequest request) throws Exception {
    RegistrationPage page = registrationPages.getObject();
    return admin(request, page, "changeTokens", page::changeTokens,
        Map.of(ActionPage.INPUT, REGISTRATION, ActionPage.SUCCESS, HOME));
  }

  @PostMapping("/admin/associateWithNetwork")
  public ModelAndView associateWithNetwork(HttpServletRequest request) throws Exception {
    RegistrationPage page = registrationPages.getObject();
    return admin(request, page, "associateWithNetwork", page::associateWithNetwork,
        Map.of(ActionPage.INPUT, REGISTRATION, ActionPage.SUCCESS, HOME));
  }
}
