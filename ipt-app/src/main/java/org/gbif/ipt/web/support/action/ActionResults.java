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

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

/**
 * Maps result names to views, like the {@code result} elements of IPT 3's struts.xml: the controller passes the
 * results of one action, which override the global ones (404, 401, 401-manager, 304, error, login, home).
 */
public final class ActionResults {

  private static final View NO_BODY = (model, request, response) -> {
  };

  private record Target(String view, HttpStatus status) {
  }

  private static final Map<String, Target> GLOBAL = Map.of(
      ActionPage.NOT_FOUND, new Target("error/404", HttpStatus.NOT_FOUND),
      ActionPage.NOT_ALLOWED, new Target("error/401", HttpStatus.UNAUTHORIZED),
      ActionPage.NOT_ALLOWED_MANAGER, new Target("error/401-manager", HttpStatus.UNAUTHORIZED),
      ActionPage.ERROR, new Target("error/error", HttpStatus.INTERNAL_SERVER_ERROR),
      ActionPage.GONE, new Target("error/404", HttpStatus.GONE),
      ActionPage.LOGIN, new Target("redirect:/login", null),
      ActionPage.HOME, new Target("redirect:/home", null));

  /** The portal package's overrides in IPT 3. */
  public static final Map<String, String> PORTAL = Map.of(
      ActionPage.NOT_FOUND, "portal/404",
      ActionPage.GONE, "portal/410");

  private ActionResults() {
  }

  /**
   * @param results result name to view name (or {@code redirect:...}); for status results the view keeps the status
   */
  public static ModelAndView view(String result, Map<String, String> results) {
    if (ActionPage.NOT_MODIFIED.equals(result)) {
      ModelAndView mav = new ModelAndView(NO_BODY);
      mav.setStatus(HttpStatus.NOT_MODIFIED);
      return mav;
    }
    Target global = GLOBAL.get(result);
    String view = results.get(result);
    if (view == null && global != null) {
      view = global.view();
    }
    if (view == null) {
      throw new IllegalStateException("No view for result " + result);
    }
    ModelAndView mav = new ModelAndView(view);
    if (global != null && global.status() != null && !view.startsWith("redirect:")) {
      mav.setStatus(global.status());
    }
    return mav;
  }

  /** Several result maps merged, later ones winning. */
  @SafeVarargs
  public static Map<String, String> results(Map<String, String>... maps) {
    Map<String, String> merged = new HashMap<>();
    for (Map<String, String> m : maps) {
      merged.putAll(m);
    }
    return merged;
  }
}
