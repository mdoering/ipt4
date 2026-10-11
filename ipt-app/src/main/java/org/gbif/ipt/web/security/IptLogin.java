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

import org.gbif.ipt.model.User;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Logs a user in without a password (IPT 3: putting the user into the session), with a new session id. */
@Component
public class IptLogin {

  private final SecurityContextRepository repository = new HttpSessionSecurityContextRepository();

  public void login(User user, HttpServletRequest request, HttpServletResponse response) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(IptAuthenticationProvider.authenticated(user));
    SecurityContextHolder.setContext(context);
    request.getSession(true);
    request.changeSessionId();
    repository.saveContext(context, request, response);
  }

  /** Logs the current user out but keeps the session (and so the messages for the next page). */
  public void logout(HttpServletRequest request) {
    SecurityContextHolder.clearContext();
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    }
  }
}
