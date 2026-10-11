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
package org.gbif.ipt.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.springframework.test.context.DynamicPropertyRegistry;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * The GBIF registry and rs.gbif.org for tests: the listings captured in {@code registry/} (their download URLs
 * pointing here), the files they list ({@code registry/files/}, else the installed vocabularies and extensions of
 * {@code installed-config/}), and answers a test sets for other registry calls. Requests are recorded.
 */
public final class StubRegistry {

  /** A recorded request; {@code user} is the HTTP Basic user name, if any. */
  public record Request(String method, String path, String body, String user) {
  }

  private record Answer(int status, String contentType, String body) {
  }

  private final HttpServer server;
  private final Map<String, Answer> answers = new ConcurrentHashMap<>();
  private final List<Request> requests = new CopyOnWriteArrayList<>();
  private final Map<String, String> organisationPasswords = new ConcurrentHashMap<>();

  private StubRegistry() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", this::handle);
    server.start();
  }

  /** Starts a stub and points the IPT's registry settings at it. */
  public static StubRegistry start(DynamicPropertyRegistry registry) throws IOException {
    StubRegistry stub = new StubRegistry();
    registry.add("ipt.defaults[dev.registry.url]", stub::url);
    registry.add("ipt.defaults[dev.registrydev.url]", stub::url);
    return stub;
  }

  public String url() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  /** Answers {@code method path} (path without query) with this status and body from now on. */
  public void answer(String method, String path, int status, String contentType, String body) {
    answers.put(method + " " + path, new Answer(status, contentType, body));
  }

  /** The password the registry's organisation login accepts for this key. */
  public void organisationPassword(String key, String password) {
    organisationPasswords.put(key, password);
  }

  public List<Request> requests() {
    return List.copyOf(requests);
  }

  private void handle(HttpExchange exchange) throws IOException {
    try (exchange) {
      String method = exchange.getRequestMethod();
      String path = exchange.getRequestURI().getPath();
      String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      requests.add(new Request(method, exchange.getRequestURI().toString(), body,
          basicUser(exchange.getRequestHeaders().getFirst("Authorization"))));
      Answer answer = answers.get(method + " " + path);
      if (answer == null && "GET".equals(method)) {
        answer = login(path, exchange.getRequestHeaders().getFirst("Authorization"));
      }
      if (answer == null && "GET".equals(method)) {
        answer = listingOrFile(path);
      }
      if (answer == null) {
        answer = new Answer(404, "text/plain", "not found");
      }
      byte[] bytes = answer.body().getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", answer.contentType());
      exchange.sendResponseHeaders(answer.status(), bytes.length == 0 ? -1 : bytes.length);
      exchange.getResponseBody().write(bytes);
    }
  }

  /** The organisation login ({@code /registry/organisation/<key>?op=login}) with HTTP Basic credentials. */
  private Answer login(String path, String authorization) {
    java.util.regex.Matcher login = java.util.regex.Pattern.compile("/registry/organisation/([0-9a-f-]{36})")
        .matcher(path);
    if (!login.matches()) {
      return null;
    }
    String password = organisationPasswords.get(login.group(1));
    String expected = password == null ? null : "Basic " + java.util.Base64.getEncoder()
        .encodeToString((login.group(1) + ":" + password).getBytes(StandardCharsets.UTF_8));
    return expected != null && expected.equals(authorization)
        ? new Answer(200, "application/json", "{}") : new Answer(401, "text/plain", "unauthorized");
  }

  private Answer listingOrFile(String path) throws IOException {
    if (path.equals("/registry/organisation.json") || path.equals("/registry/network.json")) {
      URL listing = StubRegistry.class.getResource("/registry" + path.substring("/registry".length()));
      return new Answer(200, "application/json", listing == null ? "[]" : resource("/registry" + path.substring(9)));
    }
    java.util.regex.Matcher organisation = java.util.regex.Pattern
        .compile("/registry/organisation/([0-9a-f-]{36})\\.json").matcher(path);
    if (organisation.matches()) {
      URL detail = StubRegistry.class.getResource("/registry/organisation/" + organisation.group(1) + ".json");
      return detail == null ? null
          : new Answer(200, "application/json", resource("/registry/organisation/" + organisation.group(1) + ".json"));
    }
    if (path.matches("/registry/(extensions|thesauri|dataPackages)\\.json")) {
      String listing = resource("/registry" + path.substring("/registry".length()));
      // only the download URLs come here; identifiers stay as registered
      return new Answer(200, "application/json",
          listing.replaceAll("\"url\": \"https?://rs\\.gbif\\.org/", "\"url\": \"" + url() + "/rs/"));
    }
    java.util.regex.Matcher version = java.util.regex.Pattern
        .compile("/registry/dataPackages/[^/]+/([^/]+)/version\\.json").matcher(path);
    if (version.matches()) {
      // the installed schema versions are the latest compatible ones
      return new Answer(200, "application/json", "{\"latestCompatibleVersion\": \"" + version.group(1) + "\"}");
    }
    if (path.startsWith("/rs/")) {
      String name = path.substring(path.lastIndexOf('/') + 1);
      URL file = StubRegistry.class.getResource("/registry/files/" + name);
      if (file != null) {
        return new Answer(200, "application/xml", resource("/registry/files/" + name));
      }
      String wanted = normalized("http://rs.gbif.org/" + path.substring(4));
      for (String dir : List.of(".vocabularies", ".extensions")) {
        Path installed = installedConfig().resolve(dir);
        try (Stream<Path> files = Files.list(installed)) {
          Path match = files.filter(f -> normalized(f.getFileName().toString().replaceAll("\\.[a-z]+$", ""))
              .equals(wanted)).findFirst().orElse(null);
          if (match != null) {
            return new Answer(200, "application/xml", Files.readString(match));
          }
        }
      }
    }
    return null;
  }

  private static String basicUser(String authorization) {
    if (authorization == null || !authorization.startsWith("Basic ")) {
      return null;
    }
    String decoded = new String(java.util.Base64.getDecoder().decode(authorization.substring(6)), StandardCharsets.UTF_8);
    return decoded.substring(0, Math.max(0, decoded.indexOf(':')));
  }

  private static String normalized(String s) {
    return s.replaceAll("[^A-Za-z0-9]+", "_");
  }

  private static Path installedConfig() {
    try {
      return Path.of(StubRegistry.class.getResource("/installed-config").toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String resource(String name) {
    try (InputStream in = StubRegistry.class.getResourceAsStream(name)) {
      if (in == null) {
        throw new IllegalArgumentException(name);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
