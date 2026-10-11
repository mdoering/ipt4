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
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.test.context.DynamicPropertyRegistry;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The DataCite REST API for tests ({@code /dois}, JSON:API): DOIs move between draft, registered and findable as
 * DataCite's events say; drafts can be deleted. One account (HTTP Basic). Requests are recorded; a test can make the
 * next calls fail.
 */
public final class StubDataCite {

  public record Request(String method, String path, String body, String user) {
  }

  /** A DOI as the stub holds it. */
  public record Doi(String state, String url, String xml) {
  }

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final HttpServer server;
  private final String user;
  private final String password;
  private final Map<String, Doi> dois = new ConcurrentHashMap<>();
  private final List<Request> requests = new CopyOnWriteArrayList<>();
  private volatile int failingStatus;

  private StubDataCite(String user, String password) throws IOException {
    this.user = user;
    this.password = password;
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", this::handle);
    server.start();
  }

  /** Starts a stub for this account and points the IPT's DataCite settings at it. */
  public static StubDataCite start(DynamicPropertyRegistry registry, String user, String password) throws IOException {
    StubDataCite stub = new StubDataCite(user, password);
    registry.add("ipt.defaults[dev.datacite.url]", stub::url);
    registry.add("ipt.defaults[dev.datacitedev.url]", stub::url);
    return stub;
  }

  public String url() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  public Map<String, Doi> dois() {
    return Map.copyOf(dois);
  }

  public List<Request> requests() {
    return List.copyOf(requests);
  }

  /** From now on every call answers this status (0: normal again). */
  public void failWith(int status) {
    failingStatus = status;
  }

  private void handle(HttpExchange exchange) throws IOException {
    try (exchange) {
      String method = exchange.getRequestMethod();
      String path = exchange.getRequestURI().getPath();
      String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      String authorization = exchange.getRequestHeaders().getFirst("Authorization");
      String basicUser = null;
      boolean authorised = false;
      if (authorization != null && authorization.startsWith("Basic ")) {
        String decoded = new String(Base64.getDecoder().decode(authorization.substring(6)), StandardCharsets.UTF_8);
        basicUser = decoded.substring(0, Math.max(0, decoded.indexOf(':')));
        authorised = decoded.equals(user + ":" + password);
      }
      requests.add(new Request(method, path, body, basicUser));
      if (failingStatus != 0) {
        respond(exchange, failingStatus, "text/plain", "failing");
      } else if (!authorised) {
        respond(exchange, 401, "text/plain", "unauthorized");
      } else if (path.equals("/dois") && method.equals("POST")) {
        JsonNode attributes = JSON.readTree(body).path("data").path("attributes");
        String doi = attributes.path("doi").asString().toLowerCase();
        Doi created = apply(new Doi("draft", null, null), attributes);
        dois.put(doi, created);
        respond(exchange, 201, "application/vnd.api+json", document(doi, created));
      } else if (path.startsWith("/dois/")) {
        String doi = URLDecoder.decode(path.substring("/dois/".length()), StandardCharsets.UTF_8).toLowerCase();
        Doi existing = dois.get(doi);
        if (existing == null) {
          respond(exchange, 404, "application/vnd.api+json", "{\"errors\":[{\"status\":\"404\"}]}");
        } else if (method.equals("GET")) {
          String accept = exchange.getRequestHeaders().getFirst("Accept");
          if (accept != null && accept.contains("xml")) {
            respond(exchange, 200, "application/vnd.datacite.datacite+xml", existing.xml() == null ? "" : existing.xml());
          } else {
            respond(exchange, 200, "application/vnd.api+json", document(doi, existing));
          }
        } else if (method.equals("PUT")) {
          Doi updated = apply(existing, JSON.readTree(body).path("data").path("attributes"));
          dois.put(doi, updated);
          respond(exchange, 200, "application/vnd.api+json", document(doi, updated));
        } else if (method.equals("DELETE") && existing.state().equals("draft")) {
          dois.remove(doi);
          respond(exchange, 204, "text/plain", "");
        } else {
          respond(exchange, 405, "text/plain", "not allowed");
        }
      } else {
        respond(exchange, 404, "text/plain", "not found");
      }
    }
  }

  /** DataCite's events: register makes a DOI registered, publish findable, hide registered again. */
  private static Doi apply(Doi doi, JsonNode attributes) {
    String state = switch (attributes.path("event").asString("")) {
      case "register", "hide" -> "registered";
      case "publish" -> "findable";
      default -> doi.state();
    };
    String url = attributes.hasNonNull("url") ? attributes.get("url").asString() : doi.url();
    String xml = attributes.hasNonNull("xml")
        ? new String(Base64.getDecoder().decode(attributes.get("xml").asString()), StandardCharsets.UTF_8) : doi.xml();
    return new Doi(state, url, xml);
  }

  private static String document(String doi, Doi value) {
    ObjectNode attributes = JSON.createObjectNode().put("doi", doi).put("state", value.state());
    if (value.url() != null) {
      attributes.put("url", value.url());
    }
    ObjectNode data = JSON.createObjectNode().put("id", doi).put("type", "dois");
    data.set("attributes", attributes);
    ObjectNode root = JSON.createObjectNode();
    root.set("data", data);
    return root.toString();
  }

  private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", contentType);
    exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
    exchange.getResponseBody().write(bytes);
  }
}
