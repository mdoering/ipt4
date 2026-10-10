package org.gbif.ipt.web.support.page;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.gbif.ipt.web.support.WebConfig;

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
