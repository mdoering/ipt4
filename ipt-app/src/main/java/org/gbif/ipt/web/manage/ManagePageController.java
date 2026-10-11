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

import org.gbif.ipt.model.Resource;
import org.gbif.ipt.web.portal.PortalBasePage;
import org.gbif.ipt.web.support.CurrentUser;
import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.web.support.action.ActionResults;
import org.gbif.ipt.web.support.action.ActionRunner;
import org.gbif.ipt.web.support.action.PageController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Runs manage pages behind {@link ManageGuard} with IPT 3's manage results. Redirect targets may hold placeholders,
 * filled in URL-encoded after the page ran: {@code {r}} the page's resource (else the {@code r} parameter),
 * {@code {param:x}} the request parameter x, {@code {x}} the page property x (IPT 3's {@code %{x}}).
 */
public abstract class ManagePageController extends PageController {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{(param:)?([A-Za-z]+)}");

  protected final ManageGuard guard;
  protected final CurrentUser currentUser;

  protected ManagePageController(ActionRunner runner, ManageGuard guard, CurrentUser currentUser) {
    super(runner);
    this.guard = guard;
    this.currentUser = currentUser;
  }

  protected ModelAndView manage(HttpServletRequest request, ActionPage page, String methodName,
                                ActionRunner.ActionMethod method, Map<String, String> results) throws Exception {
    return manage(request, page, methodName, method, results, true);
  }

  protected ModelAndView manage(HttpServletRequest request, ActionPage page, String methodName,
                                ActionRunner.ActionMethod method, Map<String, String> results,
                                boolean lockedRedirect) throws Exception {
    boolean resourcePage = page instanceof ManagerBasePage || page instanceof PortalBasePage;
    String blocked = guard.check(request, currentUser.get(), lockedRedirect, resourcePage);
    // a page without a form of its own goes back to the overview with its errors (Struts had no result: 500)
    Map<String, String> all = ActionResults.results(ActionResults.MANAGE,
        Map.of(ActionPage.INPUT, "redirect:/manage/resource.do?r={r}"), results);
    String result;
    try {
      result = blocked != null ? blocked : runner.run(page, request, methodName, method);
    } catch (IndexOutOfBoundsException | NumberFormatException e) {
      // a mapping or source index (mid, id) that does not exist (any more), e.g. a link from another tab
      result = ActionPage.NOT_FOUND;
    }
    ModelAndView mav = render(page, result, all);
    if (mav.getViewName() != null && mav.getViewName().startsWith("redirect:")) {
      mav.setViewName(fill(mav.getViewName(), page, request));
    }
    return mav;
  }

  static String fill(String target, ActionPage page, HttpServletRequest request) {
    Matcher m = PLACEHOLDER.matcher(target);
    StringBuilder sb = new StringBuilder();
    while (m.find()) {
      String value = m.group(1) != null ? request.getParameter(m.group(2)) : property(page, m.group(2), request);
      m.appendReplacement(sb, Matcher.quoteReplacement(
          value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8)));
    }
    m.appendTail(sb);
    return sb.toString();
  }

  private static String property(ActionPage page, String name, HttpServletRequest request) {
    if ("r".equals(name)) {
      Resource resource = page instanceof ManagerBasePage manager ? manager.getResource() : null;
      return resource != null ? resource.getShortname() : request.getParameter("r");
    }
    BeanWrapperImpl wrapper = new BeanWrapperImpl(page);
    Object value = wrapper.isReadableProperty(name) ? wrapper.getPropertyValue(name) : null;
    return value == null ? null : value.toString();
  }
}
