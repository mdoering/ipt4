package org.gbif.ipt.web.support;

import org.gbif.ipt.app.IptProperties;
import org.gbif.ipt.web.support.i18n.IptLocaleResolver;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

  /** IPT 3 request parameter for switching the UI language. */
  public static final String LOCALE_PARAMETER = "request_locale";

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
    LocaleChangeInterceptor localeChange = new LocaleChangeInterceptor();
    localeChange.setParamName(LOCALE_PARAMETER);
    localeChange.setIgnoreInvalidLocale(true);
    registry.addInterceptor(localeChange);
  }
}
