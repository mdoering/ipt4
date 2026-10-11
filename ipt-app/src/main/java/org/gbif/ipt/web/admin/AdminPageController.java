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
import org.gbif.ipt.web.support.action.ActionResults;
import org.gbif.ipt.web.support.action.ActionRunner;
import org.gbif.ipt.web.support.action.PageController;

import java.util.Map;

import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Runs admin pages with IPT 3's admin results. Access is the security filter chain's admin rule; IPT 3's
 * {@code bulkReport} had none.
 */
public abstract class AdminPageController extends PageController {

  protected AdminPageController(ActionRunner runner) {
    super(runner);
  }

  protected ModelAndView admin(HttpServletRequest request, ActionPage page, String methodName,
                               ActionRunner.ActionMethod method, Map<String, String> results) throws Exception {
    return run(page, request, methodName, method, ActionResults.results(ActionResults.ADMIN, results));
  }
}
