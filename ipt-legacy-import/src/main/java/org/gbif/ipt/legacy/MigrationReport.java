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
package org.gbif.ipt.legacy;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** What a migration did, written to {@code migration-report.json} and {@code migration-report.txt} in the new data dir. */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MigrationReport {

  /** One resource: its counts as read from IPT 3, or the error that stopped it. */
  public record ResourceResult(String shortname, int sources, int mappings, int dataPackageMappings,
                               int versions, String error) {
  }

  private final String from;
  private final String to;
  private final boolean dryRun;
  private final String started = Instant.now().toString();
  private int users;
  private int usersRehashed;
  private int organisations;
  private int filesCopied;
  private int filesLinked;
  private final List<ResourceResult> resources = new ArrayList<>();
  private final List<String> warnings = new ArrayList<>();
  private final List<String> errors = new ArrayList<>();

  MigrationReport(Path from, Path to, boolean dryRun) {
    this.from = from.toString();
    this.to = to.toString();
    this.dryRun = dryRun;
  }

  public List<ResourceResult> resources() {
    return resources;
  }

  public List<String> warnings() {
    return warnings;
  }

  public List<String> errors() {
    return errors;
  }

  public int users() {
    return users;
  }

  public int usersRehashed() {
    return usersRehashed;
  }

  public int organisations() {
    return organisations;
  }

  public int filesCopied() {
    return filesCopied;
  }

  public int filesLinked() {
    return filesLinked;
  }

  void users(int count, int rehashed) {
    this.users = count;
    this.usersRehashed = rehashed;
  }

  void organisations(int count) {
    this.organisations = count;
  }

  void copied() {
    filesCopied++;
  }

  void linked() {
    filesLinked++;
  }

  public String toJson() {
    return JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build().writeValueAsString(this);
  }

  public String toText() {
    StringBuilder sb = new StringBuilder();
    sb.append("IPT 3 to IPT4 migration").append(dryRun ? " (dry run)" : "").append(System.lineSeparator());
    sb.append("from: ").append(from).append(System.lineSeparator());
    sb.append("to:   ").append(to).append(System.lineSeparator());
    sb.append(users).append(" users (").append(usersRehashed).append(" old passwords re-hashed), ")
        .append(organisations).append(" organisations, ").append(resources.size()).append(" resources, ")
        .append(filesCopied).append(" files copied, ").append(filesLinked).append(" linked")
        .append(System.lineSeparator());
    for (ResourceResult r : resources) {
      sb.append("  ").append(r.shortname()).append(": ");
      if (r.error() != null) {
        sb.append("ERROR ").append(r.error());
      } else {
        sb.append(r.sources()).append(" sources, ").append(r.mappings()).append(" mappings, ")
            .append(r.dataPackageMappings()).append(" data package mappings, ").append(r.versions()).append(" versions");
      }
      sb.append(System.lineSeparator());
    }
    warnings.forEach(w -> sb.append("WARNING ").append(w).append(System.lineSeparator()));
    errors.forEach(e -> sb.append("ERROR ").append(e).append(System.lineSeparator()));
    return sb.toString();
  }
}
