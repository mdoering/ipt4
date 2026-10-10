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

import java.util.ArrayList;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Carries the request's messages over a {@code redirect:} (IPT 3: RedirectMessageInterceptor). */
public class FeedbackFlashInterceptor implements HandlerInterceptor {

  private final ObjectProvider<RequestFeedback> feedback;

  public FeedbackFlashInterceptor(ObjectProvider<RequestFeedback> feedback) {
    this.feedback = feedback;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                         ModelAndView mav) {
    if (mav == null || mav.getViewName() == null || !mav.getViewName().startsWith("redirect:")) {
      return;
    }
    RequestFeedback f = feedback.getObject();
    FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
    if (!f.getActionMessages().isEmpty()) {
      flash.put(RequestFeedback.MESSAGES, new ArrayList<>(f.getActionMessages()));
    }
    if (!f.getActionWarnings().isEmpty()) {
      flash.put(RequestFeedback.WARNINGS, new ArrayList<>(f.getActionWarnings()));
    }
    if (!f.getActionErrors().isEmpty()) {
      flash.put(RequestFeedback.ERRORS, new ArrayList<>(f.getActionErrors()));
    }
  }
}
