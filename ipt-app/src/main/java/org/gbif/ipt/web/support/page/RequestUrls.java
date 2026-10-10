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

import org.gbif.ipt.web.support.WebConfig;

import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

/** Port of IPT 3 {@code BaseAction.getRequestURL()}. */
public final class RequestUrls {

  private RequestUrls() {}

  /** {@code baseUrl} + request path + query string without {@code request_locale}. */
  public static String withoutLocaleParameter(String baseUrl, HttpServletRequest request) {
    StringBuilder url = new StringBuilder(baseUrl);
    String servletPath = trimToEmpty(request.getServletPath());
    if (!servletPath.isEmpty()) {
      if (!baseUrl.endsWith("/") && !servletPath.startsWith("/")) {
        url.append('/');
      }
      url.append(servletPath);
    }
    String pathInfo = trimToEmpty(request.getPathInfo());
    if (!pathInfo.isEmpty()) {
      if (!pathInfo.startsWith("/")) {
        url.append('/');
      }
      url.append(pathInfo);
    }
    String query = request.getQueryString();
    if (query != null && !query.isBlank()) {
      String parameter = WebConfig.LOCALE_PARAMETER;
      String filtered =
          Arrays.stream(query.split("&"))
              .filter(p -> !p.startsWith(parameter + "=") && !p.equals(parameter))
              .collect(Collectors.joining("&"));
      if (!filtered.isEmpty()) {
        url.append('?').append(filtered);
      }
    }
    return url.toString();
  }

  private static String trimToEmpty(String value) {
    return value == null ? "" : value.trim();
  }
}
