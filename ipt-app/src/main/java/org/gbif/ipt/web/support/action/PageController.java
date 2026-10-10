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
package org.gbif.ipt.web.support.action;

import org.gbif.ipt.web.support.page.GlobalModel;

import java.util.Map;

import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/** Base of controllers that run a ported IPT 3 action (a page bean) and render its result. */
public abstract class PageController {

  protected final ActionRunner runner;

  protected PageController(ActionRunner runner) {
    this.runner = runner;
  }

  /**
   * Runs {@code method} of {@code page} like Struts did and renders the view the result maps to, with the page as
   * {@code action}.
   */
  protected <P extends ActionPage> ModelAndView run(P page, HttpServletRequest request, String methodName,
                                                   ActionRunner.ActionMethod method, Map<String, String> results)
      throws Exception {
    String result = runner.run(page, request, methodName, method);
    return render(page, result, results);
  }

  protected ModelAndView render(ActionPage page, String result, Map<String, String> results) {
    ModelAndView mav = ActionResults.view(result, results);
    boolean redirect = mav.getViewName() != null && mav.getViewName().startsWith("redirect:");
    if (page != null && !redirect) {
      mav.addObject(GlobalModel.PAGE, page);
    }
    return mav;
  }
}
