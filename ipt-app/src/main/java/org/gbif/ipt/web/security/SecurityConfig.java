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

import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.service.admin.UserAccountManager;

import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.ForwardAuthenticationFailureHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.CacheControlHeadersWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Replaces the IPT 3 RequireAdmin/RequireManager interceptors with URL rules, and adds CSRF protection to every form
 * (IPT 3 only protected the login form). Per-resource authorization arrives with the manage pages (M3).
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Downloads (conditional GET on Last-Modified, as in IPT 3) and static assets keep their caching; everything else
   * gets Spring Security's no-store.
   */
  private static final RequestMatcher CACHEABLE = new OrRequestMatcher(Stream.of("/eml", "/metadata", "/logo",
          "/rtf", "/archive", "/publicationlog", "/sourcelog", "/appLogo", "/js/**", "/styles/**", "/images/**")
      .<RequestMatcher>map(PathPatternRequestMatcher::pathPattern).toList());

  /** Reserved for a token-authenticated REST API (e.g. a later MDT integration); no handlers exist yet. */
  @Bean
  @Order(1)
  SecurityFilterChain apiV1(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/v1/**")
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(AbstractHttpConfigurer::disable);
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain web(HttpSecurity http, UserAccountManager users, AppConfig cfg,
                          IptAuthenticationProvider authenticationProvider) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/admin/**", "/admin-api/**")
                    .access(IptAuthorization.admin(users))
                    .requestMatchers("/manage/**", "/manager-api/**", "/resource/preview")
                    .access(IptAuthorization.manager(users))
                    .anyRequest()
                    .permitAll())
        .formLogin(login -> login.loginPage("/login")
            .usernameParameter("email")
            .passwordParameter("password")
            .successHandler(new LoginSuccessHandler(users, cfg))
            // re-renders login.ftl with the error and the entered email, as IPT 3 did
            .failureHandler(new ForwardAuthenticationFailureHandler("/login?error")))
        .authenticationProvider(authenticationProvider)
        // logged-in users without rights get IPT 3's 401 pages; a missing or stale CSRF token stays a 403
        .exceptionHandling(e -> e.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
            .accessDeniedHandler((request, response, denied) -> response.sendError(
                denied instanceof CsrfException ? HttpServletResponse.SC_FORBIDDEN : HttpServletResponse.SC_UNAUTHORIZED)))
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
            .cacheControl(HeadersConfigurer.CacheControlConfig::disable)
            .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(new NegatedRequestMatcher(CACHEABLE),
                new CacheControlHeadersWriter())))
        .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/"));
    return http.build();
  }
}
