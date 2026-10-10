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
package org.gbif.ipt.web.support;

import org.gbif.ipt.app.IptProperties;
import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.config.ConfigWarnings;
import org.gbif.ipt.config.DataDir;
import org.gbif.ipt.service.admin.ConfigManager;
import org.gbif.ipt.web.setup.SetupGuard;
import org.gbif.ipt.web.support.feedback.DoSuffixFlashMapManager;
import org.gbif.ipt.web.support.feedback.FeedbackFlashInterceptor;
import org.gbif.ipt.web.support.feedback.RequestFeedback;
import org.gbif.ipt.web.support.feedback.StartupWarningsInterceptor;
import org.gbif.ipt.web.support.i18n.IptLocaleResolver;
import org.gbif.ipt.web.support.page.GlobalModel;
import org.gbif.ipt.web.support.page.PageContext;

import java.io.File;
import java.util.Locale;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.HttpServletRequest;

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

  /** IPT 3 request parameter for switching the UI language. */
  public static final String LOCALE_PARAMETER = "request_locale";

  private final ObjectProvider<RequestFeedback> feedback;
  private final ConfigWarnings warnings;

  public WebConfig(ObjectProvider<RequestFeedback> feedback, ConfigWarnings warnings) {
    this.feedback = feedback;
    this.warnings = warnings;
  }

  /** Replaces Spring Boot's {@code server.forward-headers-strategy=framework}, trusting only proxy addresses. */
  @Bean
  public FilterRegistrationBean<TrustedProxyForwardedHeaderFilter> forwardedHeaderFilter(
      @Value("${ipt.trusted-proxies:" + TrustedProxyForwardedHeaderFilter.DEFAULT_TRUSTED_PROXIES + "}")
          String trustedProxies) {
    FilterRegistrationBean<TrustedProxyForwardedHeaderFilter> registration =
        new FilterRegistrationBean<>(new TrustedProxyForwardedHeaderFilter(trustedProxies));
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }

  /** Runs before Spring Security, so URL authorization rules see the suffix-less path too. */
  @Bean
  public FilterRegistrationBean<DoSuffixFilter> doSuffixFilter() {
    FilterRegistrationBean<DoSuffixFilter> registration = new FilterRegistrationBean<>(new DoSuffixFilter());
    registration.addUrlPatterns("/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return registration;
  }

  /** Until setup is complete, pages lead to the setup wizard (IPT 3: SetupAndCancelInterceptor). */
  @Bean
  public FilterRegistrationBean<SetupGuard> setupGuard(ConfigManager configManager) {
    FilterRegistrationBean<SetupGuard> registration = new FilterRegistrationBean<>(new SetupGuard(configManager));
    registration.addUrlPatterns("/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
    return registration;
  }

  /** Uploads larger than the threshold are written to the data dir's tmp/uploads, next to the stored uploads. */
  @Bean
  public MultipartConfigElement multipartConfigElement(MultipartProperties properties, DataDir dataDir) {
    File dir = dataDir.tmpFile("uploads");
    if (!dir.isDirectory() && !dir.mkdirs()) {
      throw new IllegalStateException("Cannot create upload directory " + dir);
    }
    properties.setLocation(dir.getAbsolutePath());
    return properties.createMultipartConfig();
  }

  @Bean(name = DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME)
  public FlashMapManager flashMapManager() {
    return new DoSuffixFlashMapManager();
  }

  /** The default language is the one chosen in the admin UI ({@code ipt.properties}), else {@code ipt.default-locale}. */
  @Bean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
  public LocaleResolver localeResolver(IptProperties props, AppConfig cfg) {
    return new IptLocaleResolver(() -> StringUtils.hasText(cfg.getDefaultLocale()) ? cfg.getDefaultLocale()
        : props.defaultLocale());
  }

  /** The messages and errors of the current request; also injectable as core's {@code Feedback}. */
  @Bean
  @Primary
  @RequestScope
  public RequestFeedback requestFeedback(CurrentUser currentUser, HttpServletRequest request) {
    Locale locale = IptLocaleResolver.supported(LocaleContextHolder.getLocale()).orElse(Locale.UK);
    return new RequestFeedback(locale, currentUser.get(), RequestContextUtils.getInputFlashMap(request));
  }

  /** The page facts of the current request, injected into page beans (no proxy: page beans live per request). */
  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.NO)
  public PageContext pageContext(GlobalModel globalModel, HttpServletRequest request) {
    return globalModel.pageContext(request, LocaleContextHolder.getLocale());
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new FeedbackFlashInterceptor(feedback));
    LocaleChangeInterceptor localeChange =
        new LocaleChangeInterceptor() {
          /** A blank value would reset the visitor's choice; treat it as invalid, i.e. ignore it. */
          @Override
          protected Locale parseLocaleValue(String localeValue) {
            if (!StringUtils.hasText(localeValue)) {
              throw new IllegalArgumentException("Blank locale");
            }
            return super.parseLocaleValue(localeValue);
          }
        };
    localeChange.setParamName(LOCALE_PARAMETER);
    localeChange.setIgnoreInvalidLocale(true);
    registry.addInterceptor(localeChange);
    // after the locale change: it creates the request's feedback, which keeps the locale it was made with
    registry.addInterceptor(new StartupWarningsInterceptor(warnings, feedback));
  }
}
