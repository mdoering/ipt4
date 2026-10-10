package org.gbif.ipt.web.support;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

  /** Runs before Spring Security, so URL authorization rules see the suffix-less path too. */
  @Bean
  public FilterRegistrationBean<DoSuffixFilter> doSuffixFilter() {
    FilterRegistrationBean<DoSuffixFilter> registration = new FilterRegistrationBean<>(new DoSuffixFilter());
    registration.addUrlPatterns("/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return registration;
  }
}
