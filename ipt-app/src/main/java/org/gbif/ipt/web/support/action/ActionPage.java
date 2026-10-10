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

import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.feedback.Feedback;
import org.gbif.ipt.i18n.SimpleTextProvider;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.web.support.page.PageContext;
import org.gbif.ipt.web.support.page.PageSupport;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * What IPT 3's {@code BaseAction} gave its subclasses, without Struts: request, response and session, the result names,
 * texts, and the action's messages (which go to the request's {@link Feedback}). Ported actions extend this class and
 * keep their IPT 3 code; {@link ActionRunner} drives them like the Struts interceptor stack did.
 */
public class ActionPage extends PageSupport implements Feedback {

  public static final String SUCCESS = "success";
  public static final String INPUT = "input";
  public static final String ERROR = "error";
  public static final String NONE = "none";
  public static final String NOT_MODIFIED = "304";
  public static final String NOT_FOUND = "404";
  public static final String NOT_ALLOWED = "401";
  public static final String NOT_ALLOWED_MANAGER = "401-manager";
  public static final String LOGIN = "login";
  public static final String HOME = "home";
  public static final String LOCKED = "locked";
  public static final String GONE = "410";
  public static final String CANCEL = "cancel";

  protected final AppConfig cfg;
  protected final RegistrationManager registrationManager;
  protected final SimpleTextProvider textProvider;
  protected HttpServletRequest req;
  protected HttpServletResponse response;
  protected Map<String, Object> session;
  // a generic identifier for loading an object BEFORE parameters are bound
  public String id;
  protected boolean cancel = false;

  public ActionPage(PageContext ctx) {
    super(ctx);
    this.cfg = ctx.cfg();
    this.registrationManager = ctx.registrations();
    this.textProvider = new SimpleTextProvider(ctx.messages());
  }

  @Autowired
  public void setServletRequest(HttpServletRequest request) {
    this.req = request;
    this.session = new SessionMap(request);
  }

  @Autowired
  public void setServletResponse(HttpServletResponse response) {
    this.response = response;
  }

  /** Called before request parameters are bound (Struts' Preparable). */
  public void prepare() {
    id = StringUtils.trimToNull(req.getParameter("id"));
  }

  /** Called after binding, unless the method is exempt (input, back, cancel, list); errors then give {@link #INPUT}. */
  public void validate() {
  }

  public String execute() throws Exception {
    return SUCCESS;
  }

  public String cancel() throws Exception {
    return CANCEL;
  }

  @StrutsParameter
  public void setCancel(String cancel) {
    this.cancel = StringUtils.trimToNull(cancel) != null;
  }

  public boolean isCancel() {
    return cancel;
  }

  public String getId() {
    return id;
  }

  protected boolean isHttpPost() {
    return req != null && req.getMethod().equalsIgnoreCase("post");
  }

  public Cookie getCookie(String name) {
    if (req != null && req.getCookies() != null) {
      for (Cookie c : req.getCookies()) {
        if (name.equals(c.getName())) {
          return c;
        }
      }
    }
    return null;
  }

  public Optional<String> getRequestParameter(HttpServletRequest request, String paramName) {
    return Optional.ofNullable(request.getParameter(paramName));
  }

  /** True only if both are null or their string forms are equal. */
  protected boolean stringEquals(Object o1, Object o2) {
    if (o1 == null && o2 == null) {
      return true;
    }
    return o1 != null && o2 != null && o1.toString().equals(o2.toString());
  }

  protected void addActionExceptionWarning(Exception e) {
    if (e.getMessage() != null) {
      getFeedback().addActionWarning(e.getMessage());
    }
  }

  // Feedback, delegating to the request's feedback

  @Override
  public void addActionMessage(String message) {
    getFeedback().addActionMessage(message);
  }

  @Override
  public void addActionWarning(String warning) {
    getFeedback().addActionWarning(warning);
  }

  @Override
  public void addActionWarning(String warning, Exception e) {
    getFeedback().addActionWarning(warning, e);
  }

  @Override
  public void addActionError(String error) {
    getFeedback().addActionError(error);
  }

  @Override
  public void addFieldError(String field, String error) {
    getFeedback().addFieldError(field, error);
  }

  @Override
  public boolean hasActionErrors() {
    return getFeedback().hasActionErrors();
  }

  @Override
  public boolean hasFieldErrors() {
    return getFeedback().hasFieldErrors();
  }

  public boolean hasErrors() {
    return hasActionErrors() || hasFieldErrors();
  }

  public List<String> getWarnings() {
    return getActionWarnings();
  }

  public Map<String, List<String>> getFieldErrors() {
    return context().feedback().getFieldErrors();
  }

  public void clearErrors() {
    context().feedback().clearErrors();
  }
}
