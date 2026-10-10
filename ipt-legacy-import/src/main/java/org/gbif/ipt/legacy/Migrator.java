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

import org.gbif.ipt.config.JdbcSupport;
import org.gbif.ipt.model.DataPackageMapping;
import org.gbif.ipt.model.ExtensionMapping;
import org.gbif.ipt.model.InferredMetadata;
import org.gbif.ipt.model.Registration;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.Source;
import org.gbif.ipt.model.UrlSource;
import org.gbif.ipt.persistence.AtomicFiles;
import org.gbif.ipt.persistence.DataDirFormat;
import org.gbif.ipt.persistence.JsonResourcePersister;
import org.gbif.ipt.persistence.RegistrationStore;
import org.gbif.ipt.persistence.SecretCipher;
import org.gbif.ipt.persistence.UserStore;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts an IPT 3 data dir into a new IPT4 data dir. The source is only read. In the target, everything is copied
 * as it is, except the XML files IPT 3 wrote with XStream:
 * <ul>
 *   <li>{@code config/users.xml} becomes {@code users.json}; old PBE-encrypted passwords are re-hashed with bcrypt,</li>
 *   <li>{@code config/registration2.xml} (or the older {@code registration.xml}) becomes {@code registration.json},</li>
 *   <li>each {@code resources/<name>/resource.xml} becomes {@code resource.json} and {@code inferredMetadata.xml}
 *   becomes {@code inferred-metadata.json}.</li>
 * </ul>
 * Stored secrets are re-encrypted with the new installation's key. Every converted resource is read back and compared
 * with the original. The data dir marker is written last, so an interrupted migration never looks complete.
 */
public class Migrator {

  private static final Logger LOG = LoggerFactory.getLogger(Migrator.class);

  /** Published files that never change again: hard-linking them is safe. */
  private static final Pattern VERSIONED = Pattern.compile(".+-\\d+(\\.\\d+)*\\.(xml|json|ya?ml|zip|rtf)");
  private static final Set<String> CONVERTED_CONFIG = Set.of("users.xml", "registration.xml", "registration2.xml");
  private static final String LEGACY_RESOURCE = "resource.xml";
  private static final String LEGACY_INFERRED = "inferredMetadata.xml";

  private final Map<String, String> env;

  public Migrator(Map<String, String> env) {
    this.env = env;
  }

  public MigrationReport migrate(Path from, Path to, MigrationOptions options) throws MigrationException {
    Path source = from.toAbsolutePath().normalize();
    Path target = to.toAbsolutePath().normalize();
    MigrationReport report = new MigrationReport(source, target, options.dryRun());
    try {
      if (DataDirFormat.detect(source) != DataDirFormat.Kind.IPT3) {
        throw new MigrationException(source + " is not an IPT 3 data directory (no config/ipt.properties found)");
      }
      if (DataDirFormat.detect(target) != DataDirFormat.Kind.EMPTY) {
        throw new MigrationException("The target " + target + " is not empty. Migrate into a new, empty directory.");
      }
      if (target.startsWith(source) || source.startsWith(target)) {
        throw new MigrationException("Source and target must not contain each other");
      }

      SecretCipher cipher;
      if (options.dryRun()) {
        cipher = new SecretCipher(new byte[32]);
      } else {
        Files.createDirectories(target.resolve("config"));
        cipher = SecretCipher.create(target.resolve("config"), env);
      }
      LegacyXmlReader reader = new LegacyXmlReader(JdbcSupport.fromClasspath());
      JsonResourcePersister persister = new JsonResourcePersister(cipher, JdbcSupport.fromClasspath(), new KeysOnly());

      migrateConfig(source, target, options, reader, cipher, report);
      Installed installed = Installed.in(source.resolve("config"));
      migrateResources(source, target, options, reader, persister, installed, report);
      if (options.includeLogs()) {
        copyTree(source.resolve("logs"), target.resolve("logs"), options, report);
      }
      for (String problem : reader.takeProblems()) {
        report.warnings().add(problem);
      }

      if (!options.dryRun()) {
        AtomicFiles.writeString(target.resolve("migration-report.json"), report.toJson());
        AtomicFiles.writeString(target.resolve("migration-report.txt"), report.toText());
        Map<String, Object> migratedFrom = new LinkedHashMap<>();
        migratedFrom.put("path", source.toString());
        migratedFrom.put("migrated", Instant.now().toString());
        migratedFrom.put("errors", report.errors().size());
        DataDirFormat.writeMarker(target, version(), migratedFrom, cipher.keyCheck());
      }
      return report;
    } catch (IOException e) {
      throw new MigrationException("Migration failed: " + e.getMessage(), e);
    }
  }

  private void migrateConfig(Path source, Path target, MigrationOptions options, LegacyXmlReader reader,
                             SecretCipher cipher, MigrationReport report) throws IOException {
    Path config = source.resolve("config");
    try (Stream<Path> files = Files.walk(config)) {
      for (Path file : files.filter(Files::isRegularFile).toList()) {
        Path relative = config.relativize(file);
        if (relative.getNameCount() == 1 && CONVERTED_CONFIG.contains(relative.toString())) {
          continue;
        }
        copy(file, target.resolve("config").resolve(relative.toString()), options, report);
      }
    }

    Path usersXml = config.resolve("users.xml");
    if (Files.exists(usersXml)) {
      try (InputStream in = Files.newInputStream(usersXml)) {
        LegacyXmlReader.Users users = reader.readUsers(in);
        report.users(users.users().size(), users.rehashed());
        if (!options.dryRun()) {
          new UserStore(target.resolve("config").resolve(UserStore.FILENAME)).save(users.users());
        }
      }
    } else {
      report.warnings().add("No config/users.xml: the migrated IPT has no user accounts");
    }

    Path v2 = config.resolve("registration2.xml");
    Path v1 = config.resolve("registration.xml");
    Registration registration = null;
    if (Files.exists(v2)) {
      try (InputStream in = Files.newInputStream(v2)) {
        registration = reader.readRegistration(in);
      }
    } else if (Files.exists(v1)) {
      try (InputStream in = Files.newInputStream(v1)) {
        registration = reader.readLegacyRegistration(in);
      }
    }
    if (registration != null) {
      report.organisations(registration.getAssociatedOrganisations().size());
      if (!options.dryRun()) {
        new RegistrationStore(target.resolve("config").resolve(RegistrationStore.FILENAME), cipher).save(registration);
      }
    }
  }

  private void migrateResources(Path source, Path target, MigrationOptions options, LegacyXmlReader reader,
                                JsonResourcePersister persister, Installed installed, MigrationReport report)
      throws IOException {
    Path resources = source.resolve("resources");
    if (!Files.isDirectory(resources)) {
      report.warnings().add("No resources folder");
      return;
    }
    List<Path> dirs;
    try (Stream<Path> list = Files.list(resources)) {
      dirs = list.filter(Files::isDirectory).sorted().toList();
    }
    for (Path dir : dirs) {
      String shortname = dir.getFileName().toString();
      Path out = target.resolve("resources").resolve(shortname);
      MigrationReport.ResourceResult result;
      try {
        result = migrateResource(source, target, dir, out, shortname, options, reader, persister, report);
        checkInstalled(shortname, dir, installed, reader, report);
      } catch (IOException | RuntimeException e) {
        LOG.error("Resource {} could not be migrated", shortname, e);
        result = new MigrationReport.ResourceResult(shortname, 0, 0, 0, 0, e.getMessage());
        report.errors().add("Resource " + shortname + ": " + e.getMessage()
            + " (its files were copied; resource.xml is kept as resource.xml.ipt3)");
        copyResourceFiles(dir, out, options, report, true);
      }
      report.resources().add(result);
    }
  }

  private MigrationReport.ResourceResult migrateResource(Path source, Path target, Path dir, Path out, String shortname,
                                                        MigrationOptions options, LegacyXmlReader reader,
                                                        JsonResourcePersister persister, MigrationReport report)
      throws IOException {
    Path xml = dir.resolve(LEGACY_RESOURCE);
    if (!Files.exists(xml)) {
      throw new IOException("no resource.xml");
    }
    Resource resource;
    try (InputStream in = Files.newInputStream(xml)) {
      resource = reader.readResource(in);
    }
    for (String sql : reader.takeDroppedSecrets()) {
      report.warnings().add("Resource " + shortname + ": the password of SQL source " + sql + " could not be"
          + " decrypted and was dropped; enter it again in the source's settings");
    }
    resource.setShortname(shortname);
    rewriteUrlSourceFiles(resource, source, target);

    StringWriter json = new StringWriter();
    persister.save(resource, json);
    Resource check = persister.load(new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)));
    compare(resource, check);

    String inferredJson = null;
    Path inferredXml = dir.resolve(LEGACY_INFERRED);
    if (Files.exists(inferredXml)) {
      try (InputStream in = Files.newInputStream(inferredXml)) {
        InferredMetadata inferred = reader.readInferredMetadata(in);
        StringWriter w = new StringWriter();
        persister.saveInferredMetadata(inferred, w);
        inferredJson = w.toString();
      } catch (IOException e) {
        report.warnings().add("Resource " + shortname + ": inferred metadata dropped, it is computed again ("
            + e.getMessage() + ")");
      }
    }

    copyResourceFiles(dir, out, options, report, false);
    if (!options.dryRun()) {
      AtomicFiles.writeString(out.resolve("resource.json"), json.toString());
      if (inferredJson != null) {
        AtomicFiles.writeString(out.resolve("inferred-metadata.json"), inferredJson);
      }
    }
    return new MigrationReport.ResourceResult(shortname, resource.getSources().size(), resource.getMappings().size(),
        resource.getAllDataPackageMappings().size(), resource.getVersionHistory().size(), null);
  }

  /** Extensions and data package schemas installed in the IPT 3 data dir (copied along with it). */
  private record Installed(Set<String> rowTypes, Set<String> schemas) {

    private static final Pattern ROW_TYPE = Pattern.compile("rowType\\s*=\\s*[\"']([^\"']+)[\"']");
    private static final Pattern IDENTIFIER = Pattern.compile("\"identifier\"\\s*:\\s*\"([^\"]+)\"");

    static Installed in(Path config) throws IOException {
      return new Installed(scan(config.resolve(".extensions"), ".xml", ROW_TYPE),
          scan(config.resolve(".dataPackages"), ".json", IDENTIFIER));
    }

    private static Set<String> scan(Path dir, String suffix, Pattern pattern) throws IOException {
      Set<String> found = new java.util.HashSet<>();
      if (Files.isDirectory(dir)) {
        try (Stream<Path> files = Files.walk(dir)) {
          for (Path f : files.filter(p -> p.toString().endsWith(suffix)).toList()) {
            java.util.regex.Matcher m = pattern.matcher(Files.readString(f));
            if (m.find()) {
              found.add(m.group(1));
            }
          }
        }
      }
      return found;
    }
  }

  /**
   * IPT4 (like IPT 3) does not load a resource whose extension or data package schema is not installed; say so now
   * rather than letting the resource vanish at start-up.
   */
  private static void checkInstalled(String shortname, Path dir, Installed installed, LegacyXmlReader reader,
                                     MigrationReport report) throws IOException {
    Resource resource;
    try (InputStream in = Files.newInputStream(dir.resolve(LEGACY_RESOURCE))) {
      resource = reader.readResource(in);
    }
    reader.takeDroppedSecrets();
    Set<String> missing = new java.util.TreeSet<>();
    for (ExtensionMapping m : resource.getMappings()) {
      if (m.getExtension() != null && !installed.rowTypes().contains(m.getExtension().getRowType())) {
        missing.add(m.getExtension().getRowType());
      }
    }
    for (DataPackageMapping m : resource.getAllDataPackageMappings()) {
      if (m.getDataPackageSchema() != null && !installed.schemas().contains(m.getDataPackageSchema().getIdentifier())) {
        missing.add(m.getDataPackageSchema().getIdentifier());
      }
    }
    if (!missing.isEmpty()) {
      report.warnings().add("Resource " + shortname + " uses " + String.join(", ", missing) + ", not installed in the"
          + " IPT 3 data dir; IPT4 will not load this resource until they are installed");
    }
  }

  /** IPT 3 stored the absolute path of URL sources' downloads; they move with the data dir. */
  private static void rewriteUrlSourceFiles(Resource resource, Path source, Path target) {
    for (Source s : resource.getSources()) {
      if (s instanceof UrlSource url && url.getFile() != null) {
        Path file = url.getFile().toPath().toAbsolutePath().normalize();
        if (file.startsWith(source)) {
          url.setFile(target.resolve(source.relativize(file).toString()).toFile());
        }
      }
    }
  }

  /** The JSON read back must hold what the XML held. */
  private static void compare(Resource xml, Resource json) throws IOException {
    check("sources", xml.getSources().size(), json.getSources().size());
    check("mappings", xml.getMappings().size(), json.getMappings().size());
    for (int i = 0; i < xml.getMappings().size(); i++) {
      ExtensionMapping a = xml.getMappings().get(i);
      ExtensionMapping b = json.getMappings().get(i);
      check("mapped fields", a.getFields().size(), b.getFields().size());
    }
    check("data package mappings", xml.getAllDataPackageMappings().size(), json.getAllDataPackageMappings().size());
    check("versions", xml.getVersionHistory().size(), json.getVersionHistory().size());
    check("managers", xml.getManagers().size(), json.getManagers().size());
  }

  private static void check(String what, int expected, int actual) throws IOException {
    if (expected != actual) {
      throw new IOException("verification failed: " + expected + " " + what + " in resource.xml, " + actual
          + " in resource.json");
    }
  }

  private void copyResourceFiles(Path dir, Path out, MigrationOptions options, MigrationReport report,
                                 boolean keepLegacyXml) throws IOException {
    try (Stream<Path> files = Files.walk(dir)) {
      for (Path file : files.filter(Files::isRegularFile).toList()) {
        Path relative = dir.relativize(file);
        String name = relative.toString();
        if (name.equals(LEGACY_RESOURCE)) {
          if (keepLegacyXml) {
            copy(file, out.resolve(LEGACY_RESOURCE + ".ipt3"), options, report);
          }
          continue;
        }
        if (name.equals(LEGACY_INFERRED)) {
          continue;
        }
        Path dest = out.resolve(name);
        if (options.linkArtifacts() && relative.getNameCount() == 1 && VERSIONED.matcher(name).matches()) {
          link(file, dest, options, report);
        } else {
          copy(file, dest, options, report);
        }
      }
    }
  }

  private void copyTree(Path from, Path to, MigrationOptions options, MigrationReport report) throws IOException {
    if (!Files.isDirectory(from)) {
      return;
    }
    try (Stream<Path> files = Files.walk(from)) {
      for (Path file : files.filter(Files::isRegularFile).toList()) {
        copy(file, to.resolve(from.relativize(file).toString()), options, report);
      }
    }
  }

  private static void copy(Path file, Path dest, MigrationOptions options, MigrationReport report) throws IOException {
    if (!options.dryRun()) {
      Files.createDirectories(dest.getParent());
      Files.copy(file, dest, StandardCopyOption.COPY_ATTRIBUTES);
    }
    report.copied();
  }

  private static void link(Path file, Path dest, MigrationOptions options, MigrationReport report) throws IOException {
    if (options.dryRun()) {
      report.linked();
      return;
    }
    Files.createDirectories(dest.getParent());
    try {
      Files.createLink(dest, file);
      report.linked();
    } catch (UnsupportedOperationException | IOException e) {
      // different file systems or no hard links: copy
      Files.copy(file, dest, StandardCopyOption.COPY_ATTRIBUTES);
      report.copied();
    }
  }

  private static String version() {
    String v = Migrator.class.getPackage().getImplementationVersion();
    return v == null ? "4" : v;
  }
}
