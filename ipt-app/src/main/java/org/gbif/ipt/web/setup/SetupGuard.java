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
package org.gbif.ipt.web.setup;

import org.gbif.ipt.service.admin.ConfigManager;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Until setup is complete every page except the setup steps, static files, login and the health checks redirects to
 * the setup wizard, whose steps pass a visitor on to the first one not done yet. Runs after DoSuffixFilter.
 */
public class SetupGuard extends OncePerRequestFilter {

  public static final String FIRST_STEP = "/setupDefaultAdministrator.do";

  private static final Set<String> OPEN = Set.of("/setupDefaultAdministrator", "/setupMode", "/setupPublicUrl",
      "/setupInstallationComplete", "/setupComplete", "/login", "/health", "/api/health", "/favicon.ico", "/error");
  private static final List<String> OPEN_PREFIXES = List.of("/js/", "/styles/", "/images/", "/actuator/");

  private final ConfigManager configManager;
  // setup cannot be undone: once complete, stop asking
  private volatile boolean complete;

  public SetupGuard(ConfigManager configManager) {
    this.configManager = configManager;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!complete && !(complete = configManager.setupComplete()) && !open(path(request))) {
      response.sendRedirect(request.getContextPath() + FIRST_STEP);
      return;
    }
    chain.doFilter(request, response);
  }

  private static String path(HttpServletRequest request) {
    return request.getRequestURI().substring(request.getContextPath().length());
  }

  private static boolean open(String path) {
    return OPEN.contains(path) || OPEN_PREFIXES.stream().anyMatch(path::startsWith);
  }
}
