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
package org.gbif.ipt.web.security;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Back to the login form with IPT 3's error and the entered email. A redirect, not a forward: the forwarded POST
 * would be taken for another login attempt.
 */
public class LoginFailureHandler implements AuthenticationFailureHandler {

  /** Session attribute with the email of the failed attempt, read once by the login page. */
  public static final String FAILED_EMAIL = LoginFailureHandler.class.getName() + ".email";

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException {
    request.getSession().setAttribute(FAILED_EMAIL, StringUtils.trimToEmpty(request.getParameter("email")));
    response.sendRedirect(request.getContextPath() + "/login?error");
  }
}
