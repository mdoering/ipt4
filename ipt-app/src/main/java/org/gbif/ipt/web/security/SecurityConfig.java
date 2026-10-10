package org.gbif.ipt.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * Replaces the IPT 3 RequireAdmin/RequireManager interceptors with URL rules, and adds CSRF protection to every form
 * (IPT 3 only protected the login form). Per-resource authorization arrives with the manage pages (M3).
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

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
  SecurityFilterChain web(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/admin/**", "/admin-api/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/manage/**", "/manager-api/**")
                    .hasAnyRole("MANAGER", "PUBLISHER", "ADMIN")
                    .anyRequest()
                    .permitAll())
        .exceptionHandling(e -> e.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
        .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/"));
    return http.build();
  }

  /** No accounts until M2 ports the IPT users; this also stops Spring Boot generating a default user. */
  @Bean
  UserDetailsService userDetailsService() {
    return username -> {
      throw new UsernameNotFoundException(username);
    };
  }
}
