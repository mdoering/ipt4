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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IptHttpTest {

  private HttpServer server;
  private String base;
  private final Map<String, String> seen = new ConcurrentHashMap<>();
  private IptHttp http;

  @TempDir
  Path tmp;

  @BeforeEach
  void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/hello", ex -> {
      seen.put("ua", String.valueOf(ex.getRequestHeaders().getFirst("User-Agent")));
      seen.put("auth", String.valueOf(ex.getRequestHeaders().getFirst("Authorization")));
      respond(ex, 200, "hello " + ex.getRequestMethod());
    });
    server.createContext("/moved", ex -> {
      ex.getResponseHeaders().add("Location", "/hello");
      respond(ex, 302, "");
    });
    server.createContext("/form", ex -> {
      seen.put("ctype", ex.getRequestHeaders().getFirst("Content-Type"));
      seen.put("auth", String.valueOf(ex.getRequestHeaders().getFirst("Authorization")));
      respond(ex, 201, new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
    });
    server.createContext("/missing", ex -> respond(ex, 404, "nope"));
    server.createContext("/file", ex -> {
      String ims = ex.getRequestHeaders().getFirst("If-Modified-Since");
      if (ims != null) {
        respond(ex, 304, "");
      } else {
        ex.getResponseHeaders().add("Last-Modified", "Wed, 21 Oct 2015 07:28:00 GMT");
        respond(ex, 200, "content");
      }
    });
    server.createContext("/slow", ex -> {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      respond(ex, 200, "late");
    });
    server.start();
    base = "http://127.0.0.1:" + server.getAddress().getPort();
    http = new IptHttp("GBIF-IPT/test", Duration.ofSeconds(5));
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  private static void respond(HttpExchange ex, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    ex.sendResponseHeaders(status, status == 304 || bytes.length == 0 ? -1 : bytes.length);
    if (bytes.length > 0 && status != 304) {
      ex.getResponseBody().write(bytes);
    }
    ex.close();
  }

  @Test
  void getsBodyAndSendsUserAgent() throws Exception {
    IptHttp.Response resp = http.get(base + "/hello");
    assertThat(resp.getStatusCode()).isEqualTo(200);
    assertThat(resp.getContent()).isEqualTo("hello GET");
    assertThat(IptHttp.success(resp)).isTrue();
    assertThat(seen.get("ua")).isEqualTo("GBIF-IPT/test");
    assertThat(seen.get("auth")).isEqualTo("null");
  }

  @Test
  void followsRedirects() throws Exception {
    assertThat(http.get(base + "/moved").getContent()).isEqualTo("hello GET");
  }

  @Test
  void reportsErrorsAsUnsuccessfulResponses() throws Exception {
    IptHttp.Response resp = http.get(base + "/missing");
    assertThat(resp.getStatusCode()).isEqualTo(404);
    assertThat(IptHttp.success(resp)).isFalse();
    assertThat(resp.getStatusLine()).contains("404");
  }

  @Test
  void sendsPreemptiveBasicAuth() throws Exception {
    http.get(base + "/hello", new IptHttp.Credentials("org-key", "secret"));
    assertThat(seen.get("auth")).isEqualTo("Basic b3JnLWtleTpzZWNyZXQ=");
  }

  @Test
  void postsUrlEncodedForms() throws Exception {
    IptHttp.Response resp = http.post(base + "/form", new IptHttp.Credentials("u", "p"),
        List.of(new IptHttp.FormParam("name", "Ä b&c"), new IptHttp.FormParam("x", "1")));
    assertThat(resp.getStatusCode()).isEqualTo(201);
    assertThat(resp.getContent()).isEqualTo("name=%C3%84+b%26c&x=1");
    assertThat(seen.get("ctype")).startsWith("application/x-www-form-urlencoded");
    assertThat(seen.get("auth")).startsWith("Basic ");
  }

  @Test
  void deletes() throws Exception {
    assertThat(http.delete(base + "/hello", new IptHttp.Credentials("u", "p")).getContent()).isEqualTo("hello DELETE");
  }

  @Test
  void downloadsToFile() throws Exception {
    File target = tmp.resolve("out.txt").toFile();
    IptHttp.Response resp = http.download(URI.create(base + "/file").toURL(), target);
    assertThat(IptHttp.success(resp)).isTrue();
    assertThat(Files.readString(target.toPath())).isEqualTo("content");
  }

  @Test
  void failedDownloadLeavesNoFile() throws Exception {
    File target = tmp.resolve("missing.txt").toFile();
    IptHttp.Response resp = http.download(URI.create(base + "/missing").toURL(), target);
    assertThat(IptHttp.success(resp)).isFalse();
    assertThat(target).doesNotExist();
  }

  @Test
  void downloadIfChangedSkipsUnmodifiedFiles() throws Exception {
    File target = tmp.resolve("cached.txt").toFile();
    assertThat(http.downloadIfChanged(URI.create(base + "/file").toURL(), target)).isTrue();
    assertThat(Files.readString(target.toPath())).isEqualTo("content");
    assertThat(http.downloadIfChanged(URI.create(base + "/file").toURL(), target)).isFalse();
  }

  @Test
  void perRequestTimeout() {
    assertThatThrownBy(() -> http.get(base + "/slow", new IptHttp.RequestOptions(Duration.ofMillis(300), null)))
        .isInstanceOf(IOException.class);
  }

  @Test
  void proxyCanBeSetAndRemoved() throws Exception {
    // a proxy that is this very server: the absolute-form request still reaches /hello
    http.setProxy(new HttpHost("127.0.0.1", server.getAddress().getPort()));
    assertThat(http.getProxy()).isEqualTo(new HttpHost("127.0.0.1", server.getAddress().getPort()));
    assertThat(http.get("http://example.invalid/hello").getContent()).isEqualTo("hello GET");
    http.removeProxy();
    assertThat(http.getProxy()).isNull();
  }
}
