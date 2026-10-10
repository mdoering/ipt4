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
import java.util.regex.Pattern;

import org.springframework.web.filter.ForwardedHeaderFilter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * Applies {@code Forwarded}/{@code X-Forwarded-*} headers (scheme, host, port, prefix, client address) only when the
 * request comes from a trusted reverse proxy, and strips them otherwise. Without this, any client could make the IPT
 * generate links to another host, and a shared cache would serve those pages to everyone.
 */
public class TrustedProxyForwardedHeaderFilter implements Filter {

  /** Tomcat's default {@code RemoteIpValve} internal proxies: loopback, private, link-local and CGNAT addresses. */
  public static final String DEFAULT_TRUSTED_PROXIES =
      "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3}|169\\.254\\.\\d{1,3}\\.\\d{1,3}"
          + "|127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|100\\.6[4-9]\\.\\d{1,3}\\.\\d{1,3}|100\\.[7-9]\\d\\.\\d{1,3}\\.\\d{1,3}"
          + "|100\\.1[0-1]\\d\\.\\d{1,3}\\.\\d{1,3}|100\\.12[0-7]\\.\\d{1,3}\\.\\d{1,3}|172\\.1[6-9]\\.\\d{1,3}\\.\\d{1,3}"
          + "|172\\.2\\d\\.\\d{1,3}\\.\\d{1,3}|172\\.3[0-1]\\.\\d{1,3}\\.\\d{1,3}|0:0:0:0:0:0:0:1|::1"
          + "|fe[89ab]\\p{XDigit}:.*|f[cd]\\p{XDigit}{2}+:.*";

  private final Pattern trustedProxies;
  private final ForwardedHeaderFilter apply = new ForwardedHeaderFilter();
  private final ForwardedHeaderFilter strip = new ForwardedHeaderFilter();

  public TrustedProxyForwardedHeaderFilter(String trustedProxies) {
    this.trustedProxies = Pattern.compile(trustedProxies);
    strip.setRemoveOnly(true);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    String remote = request.getRemoteAddr();
    boolean trusted = remote != null && trustedProxies.matcher(remote).matches();
    (trusted ? apply : strip).doFilter(request, response, chain);
  }
}
