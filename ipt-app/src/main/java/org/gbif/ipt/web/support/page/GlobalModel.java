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
package org.gbif.ipt.web.support.page;

import org.gbif.ipt.app.IptProperties;
import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.i18n.Messages;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.web.support.feedback.RequestFeedback;
import org.gbif.ipt.web.support.freemarker.IptFreeMarkerView;
import org.gbif.ipt.web.support.i18n.IptLocaleResolver;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Adds what every IPT 3 page had on the value stack: the default page bean, the request's messages and field errors.
 * Controllers needing page-specific getters replace {@value #PAGE} with a {@link PageSupport} subclass built from the
 * {@value #PAGE_CONTEXT} attribute.
 */
@ControllerAdvice
public class GlobalModel {

  public static final String PAGE = IptFreeMarkerView.PAGE;
  public static final String PAGE_CONTEXT = "pageCtx";

  private final AppConfig cfg;
  private final IptProperties props;
  private final Messages messages;
  private final RegistrationManager registrations;
  private final ObjectProvider<RequestFeedback> feedback;

  public GlobalModel(AppConfig cfg, IptProperties props, Messages messages, RegistrationManager registrations,
                     ObjectProvider<RequestFeedback> feedback) {
    this.cfg = cfg;
    this.props = props;
    this.messages = messages;
    this.registrations = registrations;
    this.feedback = feedback;
  }

  @ModelAttribute
  public void addPage(Model model, HttpServletRequest request, Locale locale) {
    PageContext ctx = pageContext(request, locale);
    model.addAttribute(PAGE_CONTEXT, ctx);
    model.addAttribute(PAGE, new PageSupport(ctx));
    // live views: what the handler adds later still shows
    model.addAttribute(RequestFeedback.MESSAGES, ctx.feedback().getActionMessages());
    model.addAttribute(RequestFeedback.WARNINGS, ctx.feedback().getActionWarnings());
    model.addAttribute(RequestFeedback.ERRORS, ctx.feedback().getActionErrors());
    model.addAttribute("fieldErrors", ctx.feedback().getFieldErrors());
  }

  PageContext pageContext(HttpServletRequest request, Locale locale) {
    String baseUrl = baseUrl(request);
    Locale pageLocale = IptLocaleResolver.supported(locale).orElse(Locale.UK);
    RequestFeedback f = feedback.getObject();
    return new PageContext(messages, pageLocale, cfg, registrations, baseUrl,
        RequestUrls.withoutLocaleParameter(baseUrl, request), f.getCurrentUser(), f);
  }

  /** {@code ipt.base-url}, else the base URL set during setup, else the URL the request came in on. */
  String baseUrl(HttpServletRequest request) {
    String base = StringUtils.trimToNull(props.baseUrl());
    if (base == null) {
      base = StringUtils.trimToNull(cfg.getBaseUrl());
    }
    if (base == null) {
      base = ServletUriComponentsBuilder.fromContextPath(request).build().toUriString();
    }
    return base.replaceAll("/+$", "");
  }
}
