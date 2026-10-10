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
package org.gbif.ipt.web.support.feedback;

import org.gbif.ipt.config.ConfigWarnings;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** IPT 3's SetupAndCancelInterceptor: problems found at start-up are listed as warnings on every page. */
public class StartupWarningsInterceptor implements HandlerInterceptor {

  private final ConfigWarnings warnings;
  private final ObjectProvider<RequestFeedback> feedback;

  public StartupWarningsInterceptor(ConfigWarnings warnings, ObjectProvider<RequestFeedback> feedback) {
    this.warnings = warnings;
    this.feedback = feedback;
  }

  /** Added when a page renders (not before a redirect, which would carry them on), after the page's own warnings. */
  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                         @Nullable ModelAndView mav) {
    if (mav == null || (mav.getViewName() != null && mav.getViewName().startsWith("redirect:"))
        || !warnings.hasStartupErrors()) {
      return;
    }
    // feedback lists each warning once
    RequestFeedback f = feedback.getObject();
    f.addActionWarning(f.getText("admin.startup.warnings"));
    for (String msg : warnings.getStartupErrors()) {
      f.addActionWarning(StringUtils.trimToEmpty(msg));
    }
  }
}
