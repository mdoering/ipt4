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
import org.gbif.ipt.web.support.i18n.IptLocaleResolver;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import jakarta.servlet.DispatcherType;

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

  /** IPT 3 request parameter for switching the UI language. */
  public static final String LOCALE_PARAMETER = "request_locale";

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

  /** M1 switches the default to the language configured in the data directory. */
  @Bean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
  public LocaleResolver localeResolver(IptProperties props) {
    return new IptLocaleResolver(props::defaultLocale);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
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
  }
}
