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
package org.gbif.ipt.http;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * The IPT's HTTP client, on the JDK {@link HttpClient}. Replaces HttpClient 4 and gbif-httputils; method names follow
 * gbif-httputils so ported call sites stay recognisable.
 * <p>
 * Errors are responses, not exceptions: callers check {@link #success(Response)}. Only connection failures and
 * timeouts throw {@link IOException}.
 */
public class IptHttp {

  /** Basic auth credentials, sent preemptively. */
  public record Credentials(String username, String password) {
  }

  /** One field of a {@code application/x-www-form-urlencoded} body. */
  public record FormParam(String name, @Nullable String value) {
  }

  /** Per-request overrides: a timeout and, if not null, a proxy to use instead of the client's. */
  public record RequestOptions(Duration timeout, @Nullable HttpHost proxy) {
  }

  /** The URL could not be parsed; replaces the {@code URISyntaxException} of HttpClient 4. */
  public static class InvalidUrlException extends IOException {
    public InvalidUrlException(String url, Throwable cause) {
      super("Invalid URL " + url, cause);
    }
  }

  /** A response with its body as text. */
  public record Response(int statusCode, @Nullable String reason, @Nullable String content) {

    public int getStatusCode() {
      return statusCode;
    }

    @Nullable
    public String getContent() {
      return content;
    }

    /** Status code and reason, like HttpClient 4's status line. */
    public String getStatusLine() {
      return reason == null ? String.valueOf(statusCode) : statusCode + " " + reason;
    }
  }

  private static final DateTimeFormatter HTTP_DATE = DateTimeFormatter.RFC_1123_DATE_TIME;

  private final String userAgent;
  private final Duration timeout;
  private volatile HttpClient client;
  private volatile HttpHost proxy;

  public IptHttp(String userAgent, Duration timeout) {
    this.userAgent = userAgent;
    this.timeout = timeout;
    this.client = build(null, timeout);
  }

  public static boolean success(@Nullable Response response) {
    return response != null && success(response.statusCode());
  }

  public static boolean success(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }

  /** Sets the proxy used for all later requests. */
  public void setProxy(@Nullable HttpHost proxy) {
    this.proxy = proxy;
    this.client = build(proxy, timeout);
  }

  public void removeProxy() {
    setProxy(null);
  }

  @Nullable
  public HttpHost getProxy() {
    return proxy;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public Response get(String url) throws IOException {
    return send(request(url, null, timeout), client);
  }

  public Response get(String url, Credentials credentials) throws IOException {
    return send(request(url, credentials, timeout), client);
  }

  public Response get(String url, RequestOptions options) throws IOException {
    HttpClient c = options.proxy() == null ? client : build(options.proxy(), options.timeout());
    return send(request(url, null, options.timeout()), c);
  }

  public Response post(String url, @Nullable Credentials credentials, @Nullable List<FormParam> form)
      throws IOException {
    String body = form == null ? "" : form.stream()
        .map(p -> encode(p.name()) + "=" + encode(p.value() == null ? "" : p.value()))
        .collect(Collectors.joining("&"));
    HttpRequest req = request(url, credentials, timeout)
        .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
        .build();
    return send(req, client);
  }

  public Response postJson(String url, @Nullable Credentials credentials, String json) throws IOException {
    HttpRequest req = request(url, credentials, timeout)
        .header("Content-Type", "application/json; charset=UTF-8")
        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
        .build();
    return send(req, client);
  }

  public Response delete(String url, @Nullable Credentials credentials) throws IOException {
    return send(request(url, credentials, timeout).DELETE().build(), client);
  }

  /**
   * Downloads {@code url} into {@code file}. The file is only replaced when the server answers with success; the
   * response content is not kept.
   */
  public Response download(URL url, File file) throws IOException {
    return download(url, file, null);
  }

  /**
   * Downloads {@code url} into {@code file} unless the server says the file's copy is current (HTTP 304).
   *
   * @return true if the file was (re)written
   */
  public boolean downloadIfChanged(URL url, File file) throws IOException {
    Instant since = file.exists() ? Instant.ofEpochMilli(file.lastModified()) : null;
    Response response = download(url, file, since);
    return success(response);
  }

  private Response download(URL url, File file, @Nullable Instant ifModifiedSince) throws IOException {
    HttpRequest.Builder req = request(url.toString(), null, timeout);
    if (ifModifiedSince != null) {
      req.header("If-Modified-Since", HTTP_DATE.format(ZonedDateTime.ofInstant(ifModifiedSince, ZoneOffset.UTC)));
    }
    Path target = file.toPath().toAbsolutePath();
    Files.createDirectories(target.getParent());
    Path tmp = Files.createTempFile(target.getParent(), "." + target.getFileName(), ".download");
    try {
      HttpResponse<Path> resp = client.send(req.GET().build(),
          HttpResponse.BodyHandlers.ofFile(tmp));
      if (success(resp.statusCode())) {
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        resp.headers().firstValue("Last-Modified").ifPresent(lm -> {
          try {
            file.setLastModified(ZonedDateTime.parse(lm, HTTP_DATE).toInstant().toEpochMilli());
          } catch (RuntimeException e) {
            // keep the download time
          }
        });
      }
      return new Response(resp.statusCode(), null, null);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while downloading " + url, e);
    } finally {
      Files.deleteIfExists(tmp);
    }
  }

  private HttpRequest.Builder request(String url, @Nullable Credentials credentials, Duration requestTimeout)
      throws IOException {
    HttpRequest.Builder b;
    try {
      b = HttpRequest.newBuilder(new URI(url)).timeout(requestTimeout).header("User-Agent", userAgent);
    } catch (URISyntaxException | IllegalArgumentException e) {
      throw new InvalidUrlException(url, e);
    }
    if (credentials != null) {
      String token = Base64.getEncoder().encodeToString(
          (credentials.username() + ":" + credentials.password()).getBytes(StandardCharsets.UTF_8));
      b.header("Authorization", "Basic " + token);
    }
    return b;
  }

  private Response send(HttpRequest.Builder builder, HttpClient c) throws IOException {
    return send(builder.GET().build(), c);
  }

  private Response send(HttpRequest request, HttpClient c) throws IOException {
    try {
      HttpResponse<String> resp = c.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      return new Response(resp.statusCode(), null, resp.body());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while requesting " + request.uri(), e);
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static HttpClient build(@Nullable HttpHost proxy, Duration connectTimeout) {
    HttpClient.Builder b = HttpClient.newBuilder()
        .connectTimeout(connectTimeout)
        .followRedirects(HttpClient.Redirect.NORMAL);
    if (proxy != null) {
      b.proxy(ProxySelector.of(new InetSocketAddress(proxy.hostName(), proxy.port() < 0 ? 80 : proxy.port())));
    }
    return b.build();
  }
}
