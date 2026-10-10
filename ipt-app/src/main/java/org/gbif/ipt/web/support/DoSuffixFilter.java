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

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * IPT 3 (Struts) URLs end in {@code .do}, and many are registered in the GBIF registry ({@code archive.do?r=...},
 * {@code eml.do?r=...}). This filter strips the suffix, so every controller maps clean paths while all legacy URLs keep
 * working. The query string is untouched.
 */
public class DoSuffixFilter extends OncePerRequestFilter {

  static final String SUFFIX = ".do";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String uri = request.getRequestURI();
    if (uri == null || !uri.endsWith(SUFFIX) || uri.endsWith("/" + SUFFIX)) {
      chain.doFilter(request, response);
      return;
    }
    chain.doFilter(new SuffixStrippedRequest(request), response);
  }

  private static String strip(String value) {
    return value != null && value.endsWith(SUFFIX) ? value.substring(0, value.length() - SUFFIX.length()) : value;
  }

  private static final class SuffixStrippedRequest extends HttpServletRequestWrapper {

    SuffixStrippedRequest(HttpServletRequest request) {
      super(request);
    }

    @Override
    public String getRequestURI() {
      return strip(super.getRequestURI());
    }

    @Override
    public String getServletPath() {
      return strip(super.getServletPath());
    }

    @Override
    public String getPathInfo() {
      return strip(super.getPathInfo());
    }

    @Override
    public StringBuffer getRequestURL() {
      return new StringBuffer(strip(super.getRequestURL().toString()));
    }
  }
}
