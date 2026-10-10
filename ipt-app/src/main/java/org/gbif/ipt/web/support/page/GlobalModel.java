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

import org.gbif.ipt.i18n.Messages;
import org.gbif.ipt.web.support.freemarker.IptFreeMarkerView;
import org.gbif.ipt.web.support.i18n.IptLocaleResolver;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Adds the default page bean to every controller model. Controllers needing page-specific getters replace
 * {@value #PAGE} with a {@link PageSupport} subclass built from the {@value #PAGE_CONTEXT} attribute.
 */
@ControllerAdvice
public class GlobalModel {

  public static final String PAGE = IptFreeMarkerView.PAGE;
  public static final String PAGE_CONTEXT = "pageCtx";

  private final SiteConfig cfg;
  private final Messages messages;

  public GlobalModel(SiteConfig cfg, Messages messages) {
    this.cfg = cfg;
    this.messages = messages;
  }

  @ModelAttribute
  public void addPage(Model model, HttpServletRequest request, Locale locale) {
    PageContext ctx = pageContext(request, locale);
    model.addAttribute(PAGE_CONTEXT, ctx);
    model.addAttribute(PAGE, new PageSupport(ctx));
  }

  PageContext pageContext(HttpServletRequest request, Locale locale) {
    String baseUrl =
        Optional.ofNullable(cfg.getBaseUrl())
            .orElseGet(() -> ServletUriComponentsBuilder.fromContextPath(request).build().toUriString());
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    boolean loggedIn = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    Set<String> roles = loggedIn ? AuthorityUtils.authorityListToSet(auth.getAuthorities()) : Set.of();
    boolean admin = roles.contains("ROLE_ADMIN");
    boolean manager = admin || roles.contains("ROLE_MANAGER") || roles.contains("ROLE_PUBLISHER");
    Locale pageLocale = IptLocaleResolver.supported(locale).orElse(Locale.UK);
    return new PageContext(
        messages,
        pageLocale,
        cfg,
        baseUrl,
        RequestUrls.withoutLocaleParameter(baseUrl, request),
        loggedIn,
        admin,
        manager);
  }
}
