# IPT4 M0 (Skeleton) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable Spring Boot 4 / Java 25 skeleton of IPT4. It renders the unchanged IPT 3 header, menu, footer and about page through a Struts-compatible FreeMarker setup. It ships as a jar and a Docker image, with the build guard rails and porting tools that later milestones need.

**Architecture:** A Maven multi-module build, `ipt-core` plus `ipt-app`, under GBIF's `motherpom`, importing the Spring Boot BOM.
- `ipt-core` holds web-free code. In M0 that is only i18n `Messages` and the guard rails.
- `ipt-app` is the Boot application:
  - FreeMarker is configured exactly like Struts 7's `FreemarkerManager`.
  - A view resolves missing template variables from a page bean, the way the Struts value stack did.
  - A small `s` tag shim covers `s.text` and `s.param`.
  - Spring Security, `request_locale` handling and a `.do` suffix filter are included.

The IPT 3 templates are converted by a re-runnable script plus a few manual edits.

**Tech Stack:** Java 25, Maven 3.9, Spring Boot 4.1.1 (Spring Framework 7.0.9, Spring Security 7.1.1, FreeMarker 2.3.34, Jackson 3.1.5 with a Jackson 2.21.5 BOM, Logback), JUnit Jupiter 6, AssertJ, ArchUnit 1.5.1, Docker (`eclipse-temurin:25-jre`).

**Spec:** `docs/design.md`, a copy of the approved design `~/.claude/plans/migrate-the-existing-gbif-dynamic-bengio.md`. This plan implements its milestone **M0**.

## Global Constraints

- **Coordinates:** GroupId `org.gbif.ipt`, version `4.0.0-SNAPSHOT`. Parent `org.gbif:motherpom:61`. Spring Boot BOM `4.1.1`, imported, not used as parent.
- **Java:** `java.version` = `25`.
- **Port baseline:** IPT 3 `dwc-dp` at `03582bcf20daa3c27262c2f5e50f0d308db2c2db`; the legacy repo is `~/code/gbif/ipt`.
  - Read legacy files with `git -C ~/code/gbif/ipt show "03582bcf20daa3c27262c2f5e50f0d308db2c2db:<path>"`.
  - In zsh, always write `"${B}:path"`. An unquoted `$B:s...` is a zsh history modifier.
- **`ipt-core` dependencies:** never on `jakarta.servlet:*`, `org.springframework:spring-webmvc`, `org.springframework.boot:*`, `org.apache.struts:*` or `org.apache.tomcat.embed:*`. Enforced by build and test.
- **Templates:**
  - Suffix stays `.ftl`. Location: `ipt-app/src/main/resources/templates/`.
  - FreeMarker settings mirror Struts 7.3 `FreemarkerManager`: incompatible improvements `2.3.34`, HTML output format with `ENABLE_IF_DEFAULT` auto-escaping, `BeansWrapper` with a friendly map model, localized lookup off, whitespace stripping on, `SAFER_RESOLVER`. `?api` stays disabled; the templates never use it.
- **`.do` URLs:** every legacy URL works with and without `.do`, keeping its query string.
- **Locales:**
  - Supported: `en` (rendered as `en_GB`), `fr`, `es`, `zh`, `pt`, `ja`, `ru`.
  - Switched with the request parameter `request_locale`, stored in the session.
  - The default comes from configuration, not `Accept-Language`.
- **i18n:** bundles named `ApplicationResources_<lang>.properties`, read as UTF-8, falling back to English.
  - **Messages are formatted once.** IPT 3 formatted twice, which turned French `n''a` into `na`.
- **Commit messages:** conventional style (`build:`, `feat:`, `test:`, `docs:`, `chore:`), with no AI or Claude attribution of any kind.
- **Tests:** `mvn -B verify` must pass from the repo root on JDK 25 after every task.

## Review Focus

These failure modes are not exercised by the happy-path tests. Each has a test in the owning task:

1. **Reverse proxy / sub-path.** An IPT behind a reverse proxy or deployed under a sub-path (`/ipt`, `X-Forwarded-Prefix`) must generate every link with that prefix and scheme. Tests: Task 10 (`derivesBaseUrlFromRequestIncludingContextPath`) and Task 12 (`honoursForwardedHeaders`).
2. **Hostile query strings.** They are echoed into the language-menu links and must come out HTML-escaped, never as markup. Test: Task 12 (`escapesHostileQueryStringInLanguageLinks`).
3. **Garbage `request_locale` values.** Values like `xx`, empty, or `<script>` are ignored; the page renders in the current or default language. Test: Task 6 (`ignoresInvalidLocaleValues`).
4. **Keys missing from a translation.** They fall back to English text, not the raw key. Test: Task 2 (`keyMissingInLocaleFallsBackToEnglish`).
5. **Legacy `.do` URLs.** URLs with query strings and context paths, plus HEAD requests, behave exactly like the suffix-less URL. Tests: Task 5 (`keepsQueryStringAndContextPath`) and Task 12 (`headRequestOnDoUrl`).

---

## File structure created by this plan

```
ipt4/
  pom.xml                                   parent: motherpom 61 + Spring Boot BOM, JDK 25 plugin overrides
  .gitignore  .editorconfig  .dockerignore  README.md  renovate.json
  .github/workflows/build.yml               CI: mvn verify, ftl-migrate test, docker build
  docker/Dockerfile                         temurin 25 JRE, non-root, IPT_DATA_DIR=/srv/ipt
  docs/design.md                            approved design (spec)
  docs/BASELINE.md                          frozen port baseline
  docs/deviations.md                        intentional differences from IPT 3
  docs/audits/get-mutators.md               generated: GET links that change state
  tools/ftl-migrate/FtlMigrate.java         template conversion (single-file Java program)
  tools/ftl-migrate/test.sh + test/{input,expected}/
  tools/assets/manifest.json                vendored front-end libraries (Renovate-tracked)
  tools/audit/get-mutators.sh  tools/golden/capture.sh  tools/upstream-delta.sh  tools/docker-smoke.sh
  ipt-core/pom.xml
  ipt-core/src/main/java/org/gbif/ipt/i18n/{Messages,BundleMessages}.java
  ipt-core/src/main/resources/ApplicationResources_{en,es,fa,fr,ja,pt,ru,tr,zh}.properties
  ipt-core/src/test/java/org/gbif/ipt/architecture/CoreBoundaryTest.java
  ipt-core/src/test/java/org/gbif/ipt/i18n/BundleMessagesTest.java
  ipt-core/src/test/java/org/gbif/ipt/compat/FrictionlessJackson2CompatibilityTest.java
  ipt-app/pom.xml
  ipt-app/src/main/java/org/gbif/ipt/app/{IptApplication,IptProperties,CoreBeans}.java
  ipt-app/src/main/java/org/gbif/ipt/web/support/{DoSuffixFilter,WebConfig}.java
  ipt-app/src/main/java/org/gbif/ipt/web/support/i18n/IptLocaleResolver.java
  ipt-app/src/main/java/org/gbif/ipt/web/support/freemarker/
      IptObjectWrapper, IptFreeMarkerConfigurer, IptFreeMarkerConfig, IptFreeMarkerView, PageFallbackHash,
      FreeMarkerSharedVariables, IptTemplateVariables, IptTagLibrary, TextDirective, ParamDirective,
      ParamStack, CsrfFieldDirective, TemplateParams
  ipt-app/src/main/java/org/gbif/ipt/web/support/page/{SiteConfig,ColorScheme,PageContext,PageSupport,GlobalModel,RequestUrls}.java
  ipt-app/src/main/java/org/gbif/ipt/web/security/SecurityConfig.java
  ipt-app/src/main/java/org/gbif/ipt/web/portal/{AboutController,AboutPage,HomeController}.java
  ipt-app/src/main/resources/{application.yml,application-dev.yml,logback-spring.xml}
  ipt-app/src/main/resources/templates/{about.ftl,inc/header.ftl,inc/menu.ftl,inc/footer.ftl,inc/languages.ftl}
  ipt-app/src/main/resources/static/{js,styles,images}/   (vendored from IPT 3)
  ipt-app/src/test/...                      tests + probe controllers/templates (profiles fm-probe, locale-probe)
```

---

### Task 1: Repository bootstrap, multi-module build, core boundary guard

**Files:**
- Create: `.gitignore`, `.editorconfig`, `README.md`, `docs/design.md`, `docs/BASELINE.md`
- Create: `pom.xml`, `ipt-core/pom.xml`, `ipt-app/pom.xml`
- Test: `ipt-core/src/test/java/org/gbif/ipt/architecture/CoreBoundaryTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - Maven modules `ipt-core` and `ipt-app` (GAV `org.gbif.ipt:ipt-core|ipt-app:4.0.0-SNAPSHOT`).
  - Parent properties `spring-boot.version=4.1.1`, `archunit.version=1.5.1`, `frictionless.tableschema-java.version=0.9.9-gbif-SNAPSHOT`.
  - `docs/BASELINE.md` containing the line `baseline: 03582bcf20daa3c27262c2f5e50f0d308db2c2db`. The tools in Task 16 parse it.

- [ ] **Step 1: Initialise git and housekeeping files**

```bash
cd ~/code/gbif/ipt4
git init -b main
mkdir -p docs ipt-core/src/test/java/org/gbif/ipt/architecture ipt-app/src/main/java
cp ~/.claude/plans/migrate-the-existing-gbif-dynamic-bengio.md docs/design.md
```

`.gitignore`:
```
target/
*.iml
.idea/
.vscode/
.DS_Store
/ipt-data/
logs/
```

`.editorconfig`:
```
root = true

[*]
charset = utf-8
end_of_line = lf
indent_style = space
indent_size = 2
insert_final_newline = true
trim_trailing_whitespace = true

[*.md]
trim_trailing_whitespace = false
```

`README.md`:
````markdown
# IPT 4

The GBIF Integrated Publishing Toolkit, rebuilt on Spring Boot 4 and Java 25.
The design is in [docs/design.md](docs/design.md). It is ported from IPT 3 at the commit named in [docs/BASELINE.md](docs/BASELINE.md).

## Build and run

```bash
mvn -B verify
IPT_DATA_DIR=~/ipt4-data mvn -pl ipt-app -am spring-boot:run -Dspring-boot.run.profiles=dev   # http://localhost:7001
docker build -f docker/Dockerfile -t gbif/ipt:4.0.0-SNAPSHOT . && docker run -p 8080:8080 -v ipt4data:/srv/ipt gbif/ipt:4.0.0-SNAPSHOT
```
````

`docs/BASELINE.md`:
```markdown
# Port baseline

baseline: 03582bcf20daa3c27262c2f5e50f0d308db2c2db

IPT4 is ported from the IPT 3 `dwc-dp` branch at this commit (2026-10-09, "feat: Display \"Change dataset type\" action #3177").

Read a legacy file with `git -C ~/code/gbif/ipt show "03582bcf20daa3c27262c2f5e50f0d308db2c2db:<path>"`.
List upstream changes made since the baseline with `tools/upstream-delta.sh`.
```

- [ ] **Step 2: Verify the baseline commit exists locally**

Run: `git -C ~/code/gbif/ipt cat-file -t 03582bcf20daa3c27262c2f5e50f0d308db2c2db`
Expected: `commit`

- [ ] **Step 3: Write the parent `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.gbif</groupId>
    <artifactId>motherpom</artifactId>
    <version>61</version>
  </parent>

  <groupId>org.gbif.ipt</groupId>
  <artifactId>ipt-parent</artifactId>
  <version>4.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <name>IPT 4</name>
  <description>The GBIF Integrated Publishing Toolkit, version 4</description>
  <url>https://github.com/gbif/ipt</url>

  <modules>
    <module>ipt-core</module>
    <module>ipt-app</module>
  </modules>

  <properties>
    <java.version>25</java.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>

    <spring-boot.version>4.1.1</spring-boot.version>
    <archunit.version>1.5.1</archunit.version>
    <frictionless.tableschema-java.version>0.9.9-gbif-SNAPSHOT</frictionless.tableschema-java.version>
    <git-commit-id-maven-plugin.version>10.0.1</git-commit-id-maven-plugin.version>

    <!-- motherpom 61 plugin versions predate JDK 25 -->
    <maven-compiler-plugin.version>3.16.0</maven-compiler-plugin.version>
    <maven-surefire-plugin.version>3.6.0</maven-surefire-plugin.version>
    <maven-failsafe-plugin.version>3.6.0</maven-failsafe-plugin.version>
    <maven-enforcer-plugin.version>3.6.3</maven-enforcer-plugin.version>
    <extra-enforcer-rules.version>1.12.1</extra-enforcer-rules.version>
    <jacoco-maven-plugin.version>0.8.15</jacoco-maven-plugin.version>
  </properties>

  <repositories>
    <repository>
      <id>gbif-all</id>
      <url>https://repository.gbif.org/content/groups/gbif</url>
    </repository>
    <repository>
      <id>gbif-thirdparty-snapshots</id>
      <url>https://repository.gbif.org/content/repositories/thirdparty-snapshot</url>
      <releases>
        <enabled>false</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </repository>
  </repositories>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.gbif.ipt</groupId>
        <artifactId>ipt-core</artifactId>
        <version>${project.version}</version>
      </dependency>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring-boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>com.tngtech.archunit</groupId>
        <artifactId>archunit-junit5</artifactId>
        <version>${archunit.version}</version>
      </dependency>
      <dependency>
        <groupId>io.frictionlessdata</groupId>
        <artifactId>tableschema-java</artifactId>
        <version>${frictionless.tableschema-java.version}</version>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <configuration>
            <!-- motherpom compiles with Error Prone 2.15, which does not run on JDK 25 -->
            <compilerId>javac</compilerId>
            <parameters>true</parameters>
          </configuration>
        </plugin>
        <plugin>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-maven-plugin</artifactId>
          <version>${spring-boot.version}</version>
        </plugin>
        <plugin>
          <groupId>io.github.git-commit-id</groupId>
          <artifactId>git-commit-id-maven-plugin</artifactId>
          <version>${git-commit-id-maven-plugin.version}</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
```

- [ ] **Step 4: Write `ipt-core/pom.xml` with the banned-dependency rule**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.gbif.ipt</groupId>
    <artifactId>ipt-parent</artifactId>
    <version>4.0.0-SNAPSHOT</version>
  </parent>

  <artifactId>ipt-core</artifactId>
  <name>IPT 4 core</name>
  <description>Domain model, persistence and services of the IPT. Must not depend on any web framework.</description>

  <dependencies>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
    </dependency>

    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.assertj</groupId>
      <artifactId>assertj-core</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>com.tngtech.archunit</groupId>
      <artifactId>archunit-junit5</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-enforcer-plugin</artifactId>
        <executions>
          <execution>
            <id>core-has-no-web-dependencies</id>
            <goals>
              <goal>enforce</goal>
            </goals>
            <configuration>
              <rules>
                <bannedDependencies>
                  <searchTransitive>true</searchTransitive>
                  <excludes>
                    <exclude>jakarta.servlet:*</exclude>
                    <exclude>org.springframework:spring-webmvc</exclude>
                    <exclude>org.springframework.boot:*</exclude>
                    <exclude>org.apache.struts:*</exclude>
                    <exclude>org.apache.tomcat.embed:*</exclude>
                  </excludes>
                </bannedDependencies>
              </rules>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 5: Write a minimal `ipt-app/pom.xml`. Task 4 replaces it.**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.gbif.ipt</groupId>
    <artifactId>ipt-parent</artifactId>
    <version>4.0.0-SNAPSHOT</version>
  </parent>

  <artifactId>ipt-app</artifactId>
  <name>IPT 4 application</name>

  <dependencies>
    <dependency>
      <groupId>org.gbif.ipt</groupId>
      <artifactId>ipt-core</artifactId>
    </dependency>
  </dependencies>
</project>
```

- [ ] **Step 6: Write the architecture test**

`ipt-core/src/test/java/org/gbif/ipt/architecture/CoreBoundaryTest.java`:
```java
package org.gbif.ipt.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** ipt-core must stay usable without any web framework (see docs/design.md, section 1). */
@AnalyzeClasses(packages = "org.gbif", importOptions = ImportOption.DoNotIncludeTests.class)
class CoreBoundaryTest {

  @ArchTest
  static final ArchRule coreDoesNotUseWebFrameworks =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "jakarta.servlet..",
              "org.springframework.web.servlet..",
              "org.springframework.boot..",
              "org.apache.struts2..",
              "com.opensymphony.xwork2..")
          .allowEmptyShould(true);
}
```

- [ ] **Step 7: Build. If something fails, read the error before changing anything.**

Run: `mvn -B verify`
Expected: `BUILD SUCCESS`, with `CoreBoundaryTest` run (1 test, 0 failures).

If motherpom's `enforce-bytecode-version` execution rejects a dependency, add that artifact under the rule's `<excludes>` in the parent `pom.xml`, inside `<build><plugins>` with `<id>enforce-bytecode-version</id>`. Name the artifact in the commit message.

- [ ] **Step 8: Prove the banned-dependency rule fires (negative check, do not commit)**

Temporarily add this to `ipt-core/pom.xml` `<dependencies>`:
```xml
<dependency>
  <groupId>jakarta.servlet</groupId>
  <artifactId>jakarta.servlet-api</artifactId>
</dependency>
```
Run: `mvn -B -pl ipt-core validate`
Expected: `BUILD FAILURE` mentioning `jakarta.servlet:jakarta.servlet-api` as a banned dependency. Then remove the dependency again and re-run `mvn -B -pl ipt-core validate` (expected `BUILD SUCCESS`).

- [ ] **Step 9: Commit**

```bash
git add .gitignore .editorconfig README.md docs pom.xml ipt-core ipt-app
git commit -m "build: bootstrap IPT4 multi-module build on Java 25 and Spring Boot 4.1.1 BOM"
```

---

### Task 2: i18n `Messages` in core (exact IPT 3 lookup, single formatting)

**Files:**
- Create: `ipt-core/src/main/resources/ApplicationResources_{en,es,fa,fr,ja,pt,ru,tr,zh}.properties` (copied from the baseline)
- Create: `ipt-core/src/main/java/org/gbif/ipt/i18n/Messages.java`, `BundleMessages.java`
- Test: `ipt-core/src/test/java/org/gbif/ipt/i18n/BundleMessagesTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `org.gbif.ipt.i18n.Messages`:
    - `String get(Locale locale, String key, Object... args)`
    - `String getOrDefault(Locale locale, String key, String defaultValue, Object... args)`
    - `ResourceBundle bundle(Locale locale)`
  - Implementation `org.gbif.ipt.i18n.BundleMessages` (no-arg constructor uses bundle `ApplicationResources`).

Semantics follow `struts2/SimpleTextProvider.java` at the baseline:
- English fallback for unknown locales, independent of the JVM default locale.
- A `null` locale means English.
- A missing key returns the default value, or the key itself.
- `MessageFormat` runs exactly once. This deliberately deviates from IPT 3's double formatting (see `docs/deviations.md`, written in Task 16).

- [ ] **Step 1: Copy the bundles from the baseline**

```bash
B=03582bcf20daa3c27262c2f5e50f0d308db2c2db
mkdir -p ipt-core/src/main/resources
for l in en es fa fr ja pt ru tr zh; do
  git -C ~/code/gbif/ipt show "${B}:src/main/resources/ApplicationResources_${l}.properties" \
    > "ipt-core/src/main/resources/ApplicationResources_${l}.properties"
done
file ipt-core/src/main/resources/ApplicationResources_fr.properties
```
Expected last line: `... UTF-8 text ...` (Java 9+ reads UTF-8 property bundles directly, so `native2ascii` is no longer needed).

- [ ] **Step 2: Write the failing test**

`ipt-core/src/test/java/org/gbif/ipt/i18n/BundleMessagesTest.java`:
```java
package org.gbif.ipt.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class BundleMessagesTest {

  private final Messages messages = new BundleMessages();

  @Test
  void englishText() {
    assertThat(messages.get(Locale.UK, "menu.about")).isEqualTo("About");
  }

  @Test
  void frenchText() {
    assertThat(messages.get(Locale.FRENCH, "menu.home")).isEqualTo("Accueil");
  }

  @Test
  void formatsArguments() {
    assertThat(messages.get(Locale.ENGLISH, "basic.createdByOn", "Ann", "2020"))
        .isEqualTo("created by <b>Ann</b> on 2020");
  }

  @Test
  void unescapesDoubledApostrophesExactlyOnce() {
    assertThat(messages.get(Locale.FRENCH, "about.notRegistered"))
        .isEqualTo("Cette installation IPT n'a pas encore été enregistrée.");
    assertThat(messages.get(Locale.FRENCH, "button.updateRegistration"))
        .isEqualTo("Mettre à jour l'enregistrement");
  }

  @Test
  void missingKeyReturnsKey() {
    assertThat(messages.get(Locale.UK, "no.such.key")).isEqualTo("no.such.key");
  }

  @Test
  void missingKeyReturnsFormattedDefault() {
    assertThat(messages.getOrDefault(Locale.UK, "no.such.key", "Hello {0}", "Ann")).isEqualTo("Hello Ann");
  }

  @Test
  void keyMissingInLocaleFallsBackToEnglish() {
    // ApplicationResources_tr has no about.installation
    assertThat(messages.get(Locale.of("tr"), "about.installation")).isEqualTo("This is a default IPT hosted by");
  }

  @Test
  void unknownLocaleFallsBackToEnglishRegardlessOfJvmDefault() {
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(Locale.FRENCH);
      assertThat(messages.get(Locale.GERMAN, "menu.home")).isEqualTo("Home");
    } finally {
      Locale.setDefault(previous);
    }
  }

  @Test
  void nullLocaleMeansEnglish() {
    assertThat(messages.get(null, "menu.home")).isEqualTo("Home");
  }

  @Test
  void britishEnglishUsesTheEnglishBundle() {
    assertThat(messages.bundle(Locale.UK).getLocale().getLanguage()).isEqualTo("en");
  }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `mvn -B -pl ipt-core test -Dtest=BundleMessagesTest`
Expected: compilation FAILURE: `cannot find symbol: class Messages` / `BundleMessages`.

- [ ] **Step 4: Implement**

`ipt-core/src/main/java/org/gbif/ipt/i18n/Messages.java`:
```java
package org.gbif.ipt.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

/** Localised UI texts from the {@code ApplicationResources} bundles. */
public interface Messages {

  /** The text for {@code key} formatted with {@code args}, or the key itself if no bundle has it. */
  String get(Locale locale, String key, Object... args);

  /** The text for {@code key} formatted with {@code args}, or {@code defaultValue} (formatted) if no bundle has it. */
  String getOrDefault(Locale locale, String key, String defaultValue, Object... args);

  /** The bundle for {@code locale}, falling back to English. */
  ResourceBundle bundle(Locale locale);
}
```

`ipt-core/src/main/java/org/gbif/ipt/i18n/BundleMessages.java`:
```java
package org.gbif.ipt.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Port of the IPT 3 {@code SimpleTextProvider}: English is the fallback for any locale without a bundle, independent of
 * the JVM default locale. Unlike IPT 3, messages are run through {@link MessageFormat} once only; IPT 3 formatted twice,
 * which dropped apostrophes from translations such as French {@code n''a}.
 */
public final class BundleMessages implements Messages {

  public static final String DEFAULT_BUNDLE = "ApplicationResources";

  private static final ResourceBundle.Control ENGLISH_FALLBACK =
      new ResourceBundle.Control() {
        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
          return Locale.ENGLISH.equals(locale) ? null : Locale.ENGLISH;
        }
      };

  private final String baseName;

  public BundleMessages() {
    this(DEFAULT_BUNDLE);
  }

  public BundleMessages(String baseName) {
    this.baseName = baseName;
  }

  @Override
  public ResourceBundle bundle(Locale locale) {
    Locale requested = locale == null ? Locale.ENGLISH : locale;
    ClassLoader loader = BundleMessages.class.getClassLoader();
    try {
      return ResourceBundle.getBundle(baseName, requested, loader, ENGLISH_FALLBACK);
    } catch (MissingResourceException e) {
      return ResourceBundle.getBundle(baseName, Locale.ENGLISH, loader, ENGLISH_FALLBACK);
    }
  }

  @Override
  public String get(Locale locale, String key, Object... args) {
    return getOrDefault(locale, key, null, args);
  }

  @Override
  public String getOrDefault(Locale locale, String key, String defaultValue, Object... args) {
    String pattern;
    try {
      pattern = bundle(locale).getString(key);
    } catch (MissingResourceException e) {
      pattern = defaultValue != null ? defaultValue : key;
    }
    return format(pattern, args);
  }

  private static String format(String pattern, Object[] args) {
    try {
      return MessageFormat.format(pattern, args == null ? new Object[0] : args);
    } catch (IllegalArgumentException e) {
      return pattern;
    }
  }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn -B -pl ipt-core test -Dtest=BundleMessagesTest`
Expected: `Tests run: 10, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add ipt-core
git commit -m "feat(core): port IPT 3 message lookup with English fallback and single formatting"
```

---

### Task 3: Jackson 2 compatibility probe for the frictionless fork

The IPT 3 pom pinned Jackson 2 to 2.15 "because of tableschema-java". Spring Boot 4.1.1 manages Jackson 2 at 2.21.5. This probe proves the GBIF fork works with the Boot-managed Jackson 2 and that all Jackson 2 modules converge on one version. M1 turns the dependency into compile scope.

**Files:**
- Modify: `ipt-core/pom.xml` (test dependency)
- Test: `ipt-core/src/test/java/org/gbif/ipt/compat/FrictionlessJackson2CompatibilityTest.java`

**Interfaces:**
- Consumes: parent dependency management for `io.frictionlessdata:tableschema-java` (Task 1).
- Produces: nothing used by later M0 tasks.

- [ ] **Step 1: Write the test**

`ipt-core/src/test/java/org/gbif/ipt/compat/FrictionlessJackson2CompatibilityTest.java`:
```java
package org.gbif.ipt.compat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.frictionlessdata.tableschema.exception.InvalidCastException;
import io.frictionlessdata.tableschema.exception.TypeInferringException;
import io.frictionlessdata.tableschema.field.Field;
import io.frictionlessdata.tableschema.schema.Schema;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

/** Guards the Jackson 2 version used by the GBIF fork of tableschema-java under the Spring Boot BOM. */
class FrictionlessJackson2CompatibilityTest {

  private static final String SCHEMA =
      "{\"fields\":[{\"name\":\"id\",\"type\":\"integer\"},{\"name\":\"scientificName\",\"type\":\"string\"}]}";

  @Test
  void parsesValidatesAndWritesSchema() throws Exception {
    Schema schema = Schema.fromJson(SCHEMA, true);

    assertThat(schema.getFieldNames()).containsExactly("id", "scientificName");
    assertThat(schema.asJson()).contains("scientificName");
  }

  @Test
  void castsValuesThroughJackson2() throws Exception {
    Field<?> id = Schema.fromJson(SCHEMA, true).getField("id");

    assertThat(id.castValue("42")).isEqualTo(BigInteger.valueOf(42));
    assertThatThrownBy(() -> id.castValue("not-a-number"))
        .isInstanceOfAny(InvalidCastException.class, TypeInferringException.class);
  }

  @Test
  void jackson2ModulesConverge() {
    String databind = com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION.toString();

    assertThat(com.fasterxml.jackson.core.json.PackageVersion.VERSION.toString()).isEqualTo(databind);
    assertThat(com.fasterxml.jackson.dataformat.csv.PackageVersion.VERSION.toString()).isEqualTo(databind);
    assertThat(databind).startsWith("2.");
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -pl ipt-core test -Dtest=FrictionlessJackson2CompatibilityTest`
Expected: compilation FAILURE: `package io.frictionlessdata.tableschema.schema does not exist`.

- [ ] **Step 3: Add the test dependency to `ipt-core/pom.xml` `<dependencies>`**

```xml
    <!-- Compatibility probe only; becomes a compile dependency in M1 (data packages) -->
    <dependency>
      <groupId>io.frictionlessdata</groupId>
      <artifactId>tableschema-java</artifactId>
      <scope>test</scope>
    </dependency>
```

- [ ] **Step 4: Run the test to verify it passes, and record the resolved versions**

Run: `mvn -B -pl ipt-core test -Dtest=FrictionlessJackson2CompatibilityTest`
Expected: `Tests run: 3, Failures: 0`

Run: `mvn -B -pl ipt-core dependency:tree -Dincludes=com.fasterxml.jackson.core:jackson-databind`
Expected: `jackson-databind:jar:2.21.5:test`. If the resolved version is anything else, note it in the commit message.

- [ ] **Step 5: Commit**

```bash
git add ipt-core
git commit -m "test(core): probe frictionless tableschema fork against Boot-managed Jackson 2"
```

---

### Task 4: Spring Boot application skeleton with health endpoint

**Files:**
- Modify (replace): `ipt-app/pom.xml`
- Create: `ipt-app/src/main/java/org/gbif/ipt/app/IptApplication.java`, `IptProperties.java`, `CoreBeans.java`
- Create: `ipt-app/src/main/resources/application.yml`, `application-dev.yml`
- Test: `ipt-app/src/test/java/org/gbif/ipt/app/ApplicationSmokeTest.java`

**Interfaces:**
- Consumes: `org.gbif.ipt.i18n.Messages`, `BundleMessages` (Task 2).
- Produces:
  - `org.gbif.ipt.app.IptProperties`: record `(String dataDir, String baseUrl, String defaultLocale, String analyticsKey, boolean devMode)`, bound to prefix `ipt`.
  - A singleton bean of type `Messages`.
  - `GET /actuator/health`.
  - Spring Boot `BuildProperties` (always) and `GitProperties` (when built inside a git checkout).

- [ ] **Step 1: Replace `ipt-app/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.gbif.ipt</groupId>
    <artifactId>ipt-parent</artifactId>
    <version>4.0.0-SNAPSHOT</version>
  </parent>

  <artifactId>ipt-app</artifactId>
  <name>IPT 4 application</name>
  <description>Spring Boot web application of the IPT</description>

  <dependencies>
    <dependency>
      <groupId>org.gbif.ipt</groupId>
      <artifactId>ipt-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-freemarker</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webmvc-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <finalName>ipt-${project.version}</finalName>
    <plugins>
      <plugin>
        <groupId>io.github.git-commit-id</groupId>
        <artifactId>git-commit-id-maven-plugin</artifactId>
        <executions>
          <execution>
            <id>git-info</id>
            <phase>initialize</phase>
            <goals>
              <goal>revision</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <generateGitPropertiesFile>true</generateGitPropertiesFile>
          <generateGitPropertiesFilename>${project.build.outputDirectory}/git.properties</generateGitPropertiesFilename>
          <failOnNoGitDirectory>false</failOnNoGitDirectory>
          <commitIdGenerationMode>flat</commitIdGenerationMode>
          <includeOnlyProperties>
            <includeOnlyProperty>^git.commit.id$</includeOnlyProperty>
            <includeOnlyProperty>^git.commit.id.abbrev$</includeOnlyProperty>
            <includeOnlyProperty>^git.commit.time$</includeOnlyProperty>
            <includeOnlyProperty>^git.branch$</includeOnlyProperty>
          </includeOnlyProperties>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <executions>
          <execution>
            <goals>
              <goal>build-info</goal>
              <goal>repackage</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <mainClass>org.gbif.ipt.app.IptApplication</mainClass>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 2: Write the failing test**

`ipt-app/src/test/java/org/gbif/ipt/app/ApplicationSmokeTest.java`:
```java
package org.gbif.ipt.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;
import org.gbif.ipt.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationSmokeTest {

  @Autowired MockMvc mvc;
  @Autowired IptProperties props;
  @Autowired Messages messages;

  @Test
  void healthIsUp() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void bindsDefaults() {
    assertThat(props.defaultLocale()).isEqualTo("en");
    assertThat(props.devMode()).isFalse();
  }

  @Test
  void exposesMessages() {
    assertThat(messages.get(Locale.UK, "menu.about")).isEqualTo("About");
  }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `mvn -B -pl ipt-app -am test -Dtest=ApplicationSmokeTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation FAILURE: `cannot find symbol: class IptProperties`.

- [ ] **Step 4: Implement**

`ipt-app/src/main/java/org/gbif/ipt/app/IptApplication.java`:
```java
package org.gbif.ipt.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "org.gbif.ipt")
@ConfigurationPropertiesScan("org.gbif.ipt")
public class IptApplication {

  public static void main(String[] args) {
    SpringApplication.run(IptApplication.class, args);
  }
}
```

`ipt-app/src/main/java/org/gbif/ipt/app/IptProperties.java`:
```java
package org.gbif.ipt.app;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Deployment settings (environment, {@code application.yml}). Installation settings edited in the admin UI live in the
 * data directory and arrive with M1.
 *
 * @param dataDir the IPT data directory, from {@code IPT_DATA_DIR}
 * @param baseUrl public base URL; when blank it is derived from each request (incl. X-Forwarded-* headers)
 * @param defaultLocale language used until a visitor picks another one
 * @param analyticsKey optional Google Analytics key
 * @param devMode development conveniences: template reloading and error details in pages
 */
@ConfigurationProperties("ipt")
public record IptProperties(
    String dataDir,
    String baseUrl,
    @DefaultValue("en") String defaultLocale,
    String analyticsKey,
    boolean devMode) {}
```

`ipt-app/src/main/java/org/gbif/ipt/app/CoreBeans.java`:
```java
package org.gbif.ipt.app;

import org.gbif.ipt.i18n.BundleMessages;
import org.gbif.ipt.i18n.Messages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Beans for ipt-core classes, which carry no Spring annotations of their own. */
@Configuration(proxyBeanMethods = false)
public class CoreBeans {

  @Bean
  public Messages messages() {
    return new BundleMessages();
  }
}
```

`ipt-app/src/main/resources/application.yml`:
```yaml
spring:
  application:
    name: ipt
  main:
    banner-mode: off

server:
  port: 8080
  shutdown: graceful
  forward-headers-strategy: framework
  servlet:
    encoding:
      charset: UTF-8
      force: true

management:
  endpoints:
    web:
      exposure:
        include: health

ipt:
  data-dir: ${IPT_DATA_DIR:}
  # Read by logback-spring.xml before the application context exists
  log-dir: ${IPT_LOG_DIR:${IPT_DATA_DIR:${java.io.tmpdir}/ipt4}/logs}
  default-locale: en
  dev-mode: false
```

`ipt-app/src/main/resources/application-dev.yml`:
```yaml
server:
  port: 7001

ipt:
  dev-mode: true
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn -B -pl ipt-app -am test -Dtest=ApplicationSmokeTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 3, Failures: 0`

- [ ] **Step 6: Run the packaged jar once**

Run: `mvn -B -q -DskipTests package && (java -jar ipt-app/target/ipt-4.0.0-SNAPSHOT.jar --server.port=18081 > /tmp/ipt4-boot.log 2>&1 &) `
Then use the Monitor tool to wait until `curl -fs localhost:18081/actuator/health` returns `{"status":"UP"...}`, and stop the process with `pkill -f ipt-4.0.0-SNAPSHOT.jar`.
Expected: health returns `UP`. `unzip -p ipt-app/target/ipt-4.0.0-SNAPSHOT.jar BOOT-INF/classes/META-INF/build-info.properties` lists `build.version=4.0.0-SNAPSHOT`.

- [ ] **Step 7: Commit**

```bash
git add ipt-app
git commit -m "feat(app): Spring Boot application skeleton with IPT properties and health endpoint"
```

---

### Task 5: `.do` suffix compatibility filter

**Files:**
- Create: `ipt-app/src/main/java/org/gbif/ipt/web/support/DoSuffixFilter.java`, `WebConfig.java`
- Test: `ipt-app/src/test/java/org/gbif/ipt/web/support/DoSuffixFilterTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `org.gbif.ipt.web.support.DoSuffixFilter` (a `OncePerRequestFilter`), registered for `/*` at `Ordered.HIGHEST_PRECEDENCE + 10`. That is before Spring Security (`-100`) and before Spring MVC.
  - `org.gbif.ipt.web.support.WebConfig implements WebMvcConfigurer`. Task 6 adds to it.

- [ ] **Step 1: Write the failing test**

`ipt-app/src/test/java/org/gbif/ipt/web/support/DoSuffixFilterTest.java`:
```java
package org.gbif.ipt.web.support;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DoSuffixFilterTest {

  private HttpServletRequest filter(MockHttpServletRequest request) throws Exception {
    MockFilterChain chain = new MockFilterChain();
    new DoSuffixFilter().doFilter(request, new MockHttpServletResponse(), chain);
    return (HttpServletRequest) chain.getRequest();
  }

  @Test
  void stripsDoSuffix() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/manage/resource.do");
    request.setServletPath("/manage/resource.do");

    HttpServletRequest seen = filter(request);

    assertThat(seen.getRequestURI()).isEqualTo("/manage/resource");
    assertThat(seen.getServletPath()).isEqualTo("/manage/resource");
    assertThat(seen.getRequestURL().toString()).isEqualTo("http://localhost/manage/resource");
  }

  @Test
  void keepsQueryStringAndContextPath() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ipt/archive.do");
    request.setContextPath("/ipt");
    request.setServletPath("/archive.do");
    request.setQueryString("r=a%20b&v=1.2");

    HttpServletRequest seen = filter(request);

    assertThat(seen.getRequestURI()).isEqualTo("/ipt/archive");
    assertThat(seen.getContextPath()).isEqualTo("/ipt");
    assertThat(seen.getServletPath()).isEqualTo("/archive");
    assertThat(seen.getQueryString()).isEqualTo("r=a%20b&v=1.2");
  }

  @Test
  void stripsPathInfoWhenTheServletPathIsEmpty() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/about.do");
    request.setServletPath("");
    request.setPathInfo("/about.do");

    assertThat(filter(request).getPathInfo()).isEqualTo("/about");
  }

  @Test
  void leavesOtherPathsAlone() throws Exception {
    for (String uri : new String[] {"/styles/main.css", "/a.do/b", "/x.dox", "/.do", "/"}) {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
      assertThat(filter(request)).as(uri).isSameAs(request);
    }
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -pl ipt-app -am test -Dtest=DoSuffixFilterTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation FAILURE: `cannot find symbol: class DoSuffixFilter`.

- [ ] **Step 3: Implement**

`ipt-app/src/main/java/org/gbif/ipt/web/support/DoSuffixFilter.java`:
```java
package org.gbif.ipt.web.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

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
```

`ipt-app/src/main/java/org/gbif/ipt/web/support/WebConfig.java`:
```java
package org.gbif.ipt.web.support;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

  /** Runs before Spring Security, so URL authorization rules see the suffix-less path too. */
  @Bean
  public FilterRegistrationBean<DoSuffixFilter> doSuffixFilter() {
    FilterRegistrationBean<DoSuffixFilter> registration = new FilterRegistrationBean<>(new DoSuffixFilter());
    registration.addUrlPatterns("/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return registration;
  }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `mvn -B -pl ipt-app -am test -Dtest='DoSuffixFilterTest,ApplicationSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 7, Failures: 0`

- [ ] **Step 5: Commit**

```bash
git add ipt-app
git commit -m "feat(web): accept legacy .do URLs via a suffix-stripping filter"
```

---

### Task 6: Locale handling (`request_locale`, supported locales, configured default)

**Files:**
- Create: `ipt-app/src/main/java/org/gbif/ipt/web/support/i18n/IptLocaleResolver.java`
- Modify: `ipt-app/src/main/java/org/gbif/ipt/web/support/WebConfig.java`
- Test: `ipt-app/src/test/java/org/gbif/ipt/web/support/i18n/IptLocaleResolverTest.java`, `LocaleChangeTest.java`, `LocaleProbeController.java`

**Interfaces:**
- Consumes: `IptProperties.defaultLocale()` (Task 4) and `WebConfig` (Task 5).
- Produces:
  - `IptLocaleResolver(Supplier<String> configuredDefault)`, a `SessionLocaleResolver`, registered as bean `localeResolver`.
  - `public static final List<Locale> SUPPORTED`.
  - `public static Optional<Locale> supported(Locale)`, which maps any locale to its supported equivalent (`en*` becomes `en_GB`).
  - A `LocaleChangeInterceptor` on parameter `request_locale` with invalid values ignored.

- [ ] **Step 1: Write the failing unit test**

`ipt-app/src/test/java/org/gbif/ipt/web/support/i18n/IptLocaleResolverTest.java`:
```java
package org.gbif.ipt.web.support.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class IptLocaleResolverTest {

  private final MockHttpServletRequest request = new MockHttpServletRequest();
  private final MockHttpServletResponse response = new MockHttpServletResponse();

  @Test
  void defaultsToConfiguredLanguage() {
    assertThat(new IptLocaleResolver(() -> "fr").resolveLocale(request)).isEqualTo(Locale.FRENCH);
  }

  @Test
  void englishMeansBritishEnglish() {
    assertThat(new IptLocaleResolver(() -> "en").resolveLocale(request)).isEqualTo(Locale.UK);
  }

  @Test
  void unsupportedOrBlankDefaultFallsBackToBritishEnglish() {
    assertThat(new IptLocaleResolver(() -> "de").resolveLocale(request)).isEqualTo(Locale.UK);
    assertThat(new IptLocaleResolver(() -> "").resolveLocale(request)).isEqualTo(Locale.UK);
    assertThat(new IptLocaleResolver(() -> null).resolveLocale(request)).isEqualTo(Locale.UK);
  }

  @Test
  void ignoresAcceptLanguageHeader() {
    request.addPreferredLocale(Locale.JAPANESE);
    assertThat(new IptLocaleResolver(() -> "en").resolveLocale(request)).isEqualTo(Locale.UK);
  }

  @Test
  void storesSupportedLocaleInSession() {
    IptLocaleResolver resolver = new IptLocaleResolver(() -> "en");
    resolver.setLocale(request, response, Locale.of("pt", "BR"));
    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.of("pt"));
  }

  @Test
  void ignoresUnsupportedLocale() {
    IptLocaleResolver resolver = new IptLocaleResolver(() -> "en");
    resolver.setLocale(request, response, Locale.FRENCH);
    resolver.setLocale(request, response, Locale.GERMAN);
    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.FRENCH);
  }

  @Test
  void nullResetsToDefault() {
    IptLocaleResolver resolver = new IptLocaleResolver(() -> "en");
    resolver.setLocale(request, response, Locale.FRENCH);
    resolver.setLocale(request, response, null);
    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.UK);
  }
}
```

- [ ] **Step 2: Write the failing integration test and its probe controller**

`ipt-app/src/test/java/org/gbif/ipt/web/support/i18n/LocaleProbeController.java`:
```java
package org.gbif.ipt.web.support.i18n;

import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoint echoing the resolved locale. */
@RestController
@Profile("locale-probe")
public class LocaleProbeController {

  @GetMapping("/_probe/locale")
  public String locale(Locale locale) {
    return locale.toString();
  }
}
```

`ipt-app/src/test/java/org/gbif/ipt/web/support/i18n/LocaleChangeTest.java`:
```java
package org.gbif.ipt.web.support.i18n;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("locale-probe")
class LocaleChangeTest {

  @Autowired MockMvc mvc;

  @Test
  void defaultIsBritishEnglish() throws Exception {
    mvc.perform(get("/_probe/locale")).andExpect(content().string("en_GB"));
  }

  @Test
  void requestLocaleParameterSwitchesAndIsRemembered() throws Exception {
    MockHttpSession session = new MockHttpSession();
    mvc.perform(get("/_probe/locale").param("request_locale", "ja").session(session))
        .andExpect(content().string("ja"));
    mvc.perform(get("/_probe/locale").session(session)).andExpect(content().string("ja"));
  }

  @Test
  void worksOnDoUrls() throws Exception {
    mvc.perform(get("/_probe/locale.do").param("request_locale", "fr")).andExpect(content().string("fr"));
  }

  @Test
  void ignoresInvalidLocaleValues() throws Exception {
    for (String value : new String[] {"xx", "", "<script>alert(1)</script>", "de_DE"}) {
      mvc.perform(get("/_probe/locale").param("request_locale", value))
          .andExpect(status().isOk())
          .andExpect(content().string("en_GB"));
    }
  }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `mvn -B -pl ipt-app -am test -Dtest='IptLocaleResolverTest,LocaleChangeTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation FAILURE: `cannot find symbol: class IptLocaleResolver`.

- [ ] **Step 4: Implement**

`ipt-app/src/main/java/org/gbif/ipt/web/support/i18n/IptLocaleResolver.java`:
```java
package org.gbif.ipt.web.support.i18n;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * IPT 3 locale rules: a visitor's choice ({@code ?request_locale=xx}) is kept in the session, only the enabled UI
 * languages are accepted, and the default is the configured IPT language, never the browser's Accept-Language.
 */
public class IptLocaleResolver extends SessionLocaleResolver {

  /** Enabled UI languages; English is en_GB for day-month-year dates, as in IPT 3. */
  public static final List<Locale> SUPPORTED =
      List.of(
          Locale.UK,
          Locale.FRENCH,
          Locale.of("es"),
          Locale.CHINESE,
          Locale.of("pt"),
          Locale.JAPANESE,
          Locale.of("ru"));

  public IptLocaleResolver(Supplier<String> configuredDefault) {
    setDefaultLocaleFunction(request -> supported(parse(configuredDefault.get())).orElse(Locale.UK));
  }

  @Override
  public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
    if (locale == null) {
      super.setLocale(request, response, null);
      return;
    }
    supported(locale).ifPresent(l -> super.setLocale(request, response, l));
  }

  /** The enabled UI locale with the same language as {@code locale}, if any. */
  public static Optional<Locale> supported(Locale locale) {
    if (locale == null) {
      return Optional.empty();
    }
    return SUPPORTED.stream().filter(s -> s.getLanguage().equals(locale.getLanguage())).findFirst();
  }

  private static Locale parse(String value) {
    return value == null || value.isBlank() ? null : StringUtils.parseLocale(value);
  }
}
```

Modify `WebConfig.java`. The complete file after this step:
```java
package org.gbif.ipt.web.support;

import org.gbif.ipt.app.IptProperties;
import org.gbif.ipt.web.support.i18n.IptLocaleResolver;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

  /** IPT 3 request parameter for switching the UI language. */
  public static final String LOCALE_PARAMETER = "request_locale";

  /** Runs before Spring Security, so URL authorization rules see the suffix-less path too. */
  @Bean
  public FilterRegistrationBean<DoSuffixFilter> doSuffixFilter() {
    FilterRegistrationBean<DoSuffixFilter> registration = new FilterRegistrationBean<>(new DoSuffixFilter());
    registration.addUrlPatterns("/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return registration;
  }

  /** M1 switches the default to the language configured in the data directory. */
  @Bean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
  public LocaleResolver localeResolver(IptProperties props) {
    return new IptLocaleResolver(props::defaultLocale);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    LocaleChangeInterceptor localeChange = new LocaleChangeInterceptor();
    localeChange.setParamName(LOCALE_PARAMETER);
    localeChange.setIgnoreInvalidLocale(true);
    registry.addInterceptor(localeChange);
  }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `mvn -B -pl ipt-app -am test -Dtest='IptLocaleResolverTest,LocaleChangeTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 11, Failures: 0`

- [ ] **Step 6: Commit**

```bash
git add ipt-app
git commit -m "feat(web): IPT 3 locale rules with request_locale switching"
```

---

### Task 7: FreeMarker configured like Struts 7, with page-bean variable fallback

**Files:**
- Create in `ipt-app/src/main/java/org/gbif/ipt/web/support/freemarker/`: `IptObjectWrapper.java`, `IptFreeMarkerConfigurer.java`, `IptFreeMarkerConfig.java`, `IptFreeMarkerView.java`, `PageFallbackHash.java`, `FreeMarkerSharedVariables.java`
- Test: `ipt-app/src/test/java/org/gbif/ipt/web/support/freemarker/FreeMarkerProbeController.java`, `FreeMarkerSetupTest.java`
- Test resources: `ipt-app/src/test/resources/templates/probe/{escaping,square,maps,fallback,missing}.ftl`

**Interfaces:**
- Consumes: `IptProperties.devMode()` (Task 4).
- Produces:
  - Bean `freeMarkerConfigurer` (an `IptFreeMarkerConfigurer`) and bean `freeMarkerViewResolver` (view class `IptFreeMarkerView`, suffix `.ftl`, prefix empty, templates under `classpath:/templates/`).
  - `IptFreeMarkerView.PAGE = "page"`: the model attribute holding the page bean.
  - `IptFreeMarkerView.ACTION = "action"`: an alias of the page bean for templates.
  - Lookup order for template variables: model, then request/session/application attributes (Spring's `AllHttpScopesHashModel`), then page bean property or method, then page bean `get<Name>()`/`is<Name>()` (OGNL-style, so `${baseURL}` finds `getBaseURL()`).
  - `interface FreeMarkerSharedVariables { Map<String, Object> variables(); }`: all beans of this type are merged into FreeMarker shared variables.

- [ ] **Step 1: Write the probe controller and templates**

`ipt-app/src/test/java/org/gbif/ipt/web/support/freemarker/FreeMarkerProbeController.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import java.util.Map;
import java.util.TreeMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Test-only controller rendering templates from src/test/resources/templates/probe. */
@Controller
@Profile("fm-probe")
public class FreeMarkerProbeController {

  @GetMapping("/_probe/{view}")
  public String render(@PathVariable String view, Model model) {
    model.addAttribute("html", "<b>bold</b>");
    model.addAttribute("stringKeyed", new TreeMap<>(Map.of("a", "x", "b", "y")));
    model.addAttribute("emptyMap", Map.of());
    model.addAttribute("modelValue", "from model");
    model.addAttribute(IptFreeMarkerView.PAGE, new ProbePage());
    return "probe/" + view;
  }

  public static class ProbePage {
    public String getTitle() {
      return "From page";
    }

    public String getBaseURL() {
      return "http://probe";
    }

    public String getModelValue() {
      return "from page";
    }

    public String getText(String key) {
      return "T:" + key;
    }
  }
}
```

`ipt-app/src/test/resources/templates/probe/escaping.ftl` (deliberately has no `#ftl` header: the configuration-wide HTML output format must apply):
```
${html}|${html?no_esc}
```

`ipt-app/src/test/resources/templates/probe/square.ftl`:
```
[#ftl]
[#if html?has_content]square:${html}[/#if]
```

`ipt-app/src/test/resources/templates/probe/maps.ftl`:
```
${stringKeyed.get("a")}|${stringKeyed.b}|<#list stringKeyed?keys as k>${k}=${stringKeyed[k]};</#list>|<#if emptyMap?has_content>full<#else>empty</#if>
```

`ipt-app/src/test/resources/templates/probe/fallback.ftl`:
```
${title}|${action.title}|${baseURL}|${getText("k")}|${notThere!"none"}|${modelValue}
```

`ipt-app/src/test/resources/templates/probe/missing.ftl`:
```
${definitelyMissing}
```

- [ ] **Step 2: Write the failing test**

`ipt-app/src/test/java/org/gbif/ipt/web/support/freemarker/FreeMarkerSetupTest.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import freemarker.core.InvalidReferenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("fm-probe")
class FreeMarkerSetupTest {

  @Autowired MockMvc mvc;

  private String render(String view) throws Exception {
    return mvc.perform(get("/_probe/" + view)).andReturn().getResponse().getContentAsString().trim();
  }

  @Test
  void escapesHtmlByDefaultLikeStruts7() throws Exception {
    assertThat(render("escaping")).isEqualTo("&lt;b&gt;bold&lt;/b&gt;|<b>bold</b>");
  }

  @Test
  void supportsSquareBracketSyntax() throws Exception {
    assertThat(render("square")).isEqualTo("square:&lt;b&gt;bold&lt;/b&gt;");
  }

  @Test
  void wrapsMapsLikeStrutsBeanWrapper() throws Exception {
    assertThat(render("maps")).isEqualTo("x|y|a=x;b=y;|empty");
  }

  @Test
  void resolvesMissingVariablesFromPageBean() throws Exception {
    assertThat(render("fallback")).isEqualTo("From page|From page|http://probe|T:k|none|from model");
  }

  @Test
  void undefinedVariablesFailTheRequest() {
    assertThatThrownBy(() -> mvc.perform(get("/_probe/missing")))
        .hasRootCauseInstanceOf(InvalidReferenceException.class);
  }

  @Test
  void servesHtmlAsUtf8() throws Exception {
    assertThat(mvc.perform(get("/_probe/escaping")).andReturn().getResponse().getContentType())
        .isEqualTo("text/html;charset=UTF-8");
  }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `mvn -B -pl ipt-app -am test -Dtest=FreeMarkerSetupTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation FAILURE: `cannot find symbol: variable IptFreeMarkerView`.

- [ ] **Step 4: Implement**

`IptObjectWrapper.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.SimpleMapModel;
import freemarker.ext.util.ModelFactory;
import freemarker.template.Version;
import java.util.Map;

/**
 * Equivalent of Struts' {@code StrutsBeanWrapper} with {@code struts.freemarker.wrapper.altMap=true}: a plain
 * {@link BeansWrapper} that exposes maps as {@link SimpleMapModel}, so templates can use {@code map.get(key)},
 * {@code map[key]} and {@code ?keys} exactly as in IPT 3.
 */
public class IptObjectWrapper extends BeansWrapper {

  private static final ModelFactory FRIENDLY_MAP_FACTORY =
      (object, wrapper) -> new FriendlyMapModel((Map<?, ?>) object, (BeansWrapper) wrapper);

  public IptObjectWrapper(Version incompatibleImprovements) {
    super(incompatibleImprovements);
  }

  @Override
  protected ModelFactory getModelFactory(Class<?> clazz) {
    if (Map.class.isAssignableFrom(clazz)) {
      return FRIENDLY_MAP_FACTORY;
    }
    return super.getModelFactory(clazz);
  }

  /** Like Struts' FriendlyMapModel: empty exactly when the map is empty. */
  static final class FriendlyMapModel extends SimpleMapModel {

    FriendlyMapModel(Map<?, ?> map, BeansWrapper wrapper) {
      super(map, wrapper);
    }

    @Override
    public boolean isEmpty() {
      return ((Map<?, ?>) object).isEmpty();
    }
  }
}
```

`IptFreeMarkerConfigurer.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import freemarker.core.HTMLOutputFormat;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

/**
 * FreeMarker settings copied from Struts 7.3 {@code FreemarkerManager.createConfiguration()}, so the IPT 3 templates
 * render unchanged. Differences: undefined variables fail the request (Struts printed HTML debug output into the page),
 * except in dev mode.
 */
public class IptFreeMarkerConfigurer extends FreeMarkerConfigurer {

  public static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_34;

  private final boolean devMode;

  public IptFreeMarkerConfigurer(boolean devMode) {
    this.devMode = devMode;
  }

  @Override
  protected Configuration newConfiguration() {
    return new Configuration(FREEMARKER_VERSION);
  }

  @Override
  protected void postProcessConfiguration(Configuration config) {
    config.setObjectWrapper(new IptObjectWrapper(FREEMARKER_VERSION));
    config.setOutputFormat(HTMLOutputFormat.INSTANCE);
    config.setAutoEscapingPolicy(Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY);
    config.setLocalizedLookup(false);
    config.setWhitespaceStripping(true);
    config.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);
    config.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
    config.setLogTemplateExceptions(false);
    config.setTemplateExceptionHandler(
        devMode ? TemplateExceptionHandler.HTML_DEBUG_HANDLER : TemplateExceptionHandler.RETHROW_HANDLER);
    config.setTemplateUpdateDelayMilliseconds(devMode ? 0 : 60_000);
  }
}
```

`FreeMarkerSharedVariables.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import java.util.Map;

/** Contributes variables visible in every template, such as the {@code s} tag library. */
public interface FreeMarkerSharedVariables {

  Map<String, Object> variables();
}
```

`IptFreeMarkerConfig.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.gbif.ipt.app.IptProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

/**
 * Replaces Spring Boot's FreeMarker beans, so {@code spring.freemarker.*} properties do not apply; all settings are in
 * {@link IptFreeMarkerConfigurer}.
 */
@Configuration(proxyBeanMethods = false)
public class IptFreeMarkerConfig {

  public static final String TEMPLATE_PATH = "classpath:/templates/";

  @Bean
  public FreeMarkerConfigurer freeMarkerConfigurer(
      IptProperties props, ObjectProvider<FreeMarkerSharedVariables> sharedVariables) {
    IptFreeMarkerConfigurer configurer = new IptFreeMarkerConfigurer(props.devMode());
    configurer.setTemplateLoaderPath(TEMPLATE_PATH);
    configurer.setDefaultEncoding(StandardCharsets.UTF_8.name());
    configurer.setPreferFileSystemAccess(false);
    Map<String, Object> variables = new HashMap<>();
    sharedVariables.orderedStream().forEach(v -> variables.putAll(v.variables()));
    configurer.setFreemarkerVariables(variables);
    return configurer;
  }

  @Bean(name = "freeMarkerViewResolver")
  public FreeMarkerViewResolver freeMarkerViewResolver() {
    FreeMarkerViewResolver resolver = new FreeMarkerViewResolver("", ".ftl");
    resolver.setViewClass(IptFreeMarkerView.class);
    resolver.setContentType("text/html;charset=UTF-8");
    resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 5);
    return resolver;
  }
}
```

`IptFreeMarkerView.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import freemarker.template.SimpleHash;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.servlet.view.freemarker.FreeMarkerView;

/**
 * Gives IPT 3 templates the variable lookup of the Struts value stack: anything not in the model is read from the page
 * bean (model attribute {@value #PAGE}), which templates can also address as {@code action}.
 */
public class IptFreeMarkerView extends FreeMarkerView {

  public static final String PAGE = "page";
  public static final String ACTION = "action";

  @Override
  protected SimpleHash buildTemplateModel(
      Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {
    Object page = model.get(PAGE);
    Map<String, Object> templateModel = model;
    if (page != null && !model.containsKey(ACTION)) {
      templateModel = new LinkedHashMap<>(model);
      templateModel.put(ACTION, page);
    }
    SimpleHash base = super.buildTemplateModel(templateModel, request, response);
    return page == null ? base : new PageFallbackHash(base, page, getObjectWrapper());
  }
}
```

`PageFallbackHash.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/** Template root: the Spring model first, then the page bean, then OGNL-style {@code get<Name>()} on the page bean. */
final class PageFallbackHash extends SimpleHash {

  private final SimpleHash base;
  private final Object page;
  private final TemplateHashModel pageModel;

  PageFallbackHash(SimpleHash base, Object page, ObjectWrapper wrapper) {
    super(wrapper);
    this.base = base;
    this.page = page;
    try {
      this.pageModel = (TemplateHashModel) wrapper.wrap(page);
    } catch (TemplateModelException e) {
      throw new IllegalStateException("Cannot expose page bean " + page.getClass().getName(), e);
    }
  }

  @Override
  public TemplateModel get(String key) throws TemplateModelException {
    TemplateModel value = base.get(key);
    if (value != null) {
      return value;
    }
    value = pageModel.get(key);
    if (value != null) {
      return value;
    }
    Method getter = findGetter(page.getClass(), key);
    if (getter == null) {
      return null;
    }
    try {
      return getObjectWrapper().wrap(getter.invoke(page));
    } catch (ReflectiveOperationException e) {
      throw new TemplateModelException("Cannot read " + key + " from " + page.getClass().getName(), e);
    }
  }

  /** Java beans name {@code getBaseURL()} "BaseURL"; OGNL (and so IPT 3 templates) call it {@code baseURL}. */
  private static Method findGetter(Class<?> type, String key) {
    if (key.isEmpty()) {
      return null;
    }
    String capitalized = Character.toUpperCase(key.charAt(0)) + key.substring(1);
    for (String name : List.of("get" + capitalized, "is" + capitalized)) {
      try {
        return type.getMethod(name);
      } catch (NoSuchMethodException ignored) {
        // try the next accessor name
      }
    }
    return null;
  }

  @Override
  public void put(String key, Object value) {
    base.put(key, value);
  }

  @Override
  public void put(String key, boolean value) {
    base.put(key, value);
  }

  @Override
  public void remove(String key) {
    base.remove(key);
  }

  @Override
  public boolean containsKey(String key) {
    return base.containsKey(key);
  }

  @Override
  public int size() {
    return base.size();
  }

  @Override
  public boolean isEmpty() {
    return base.isEmpty();
  }

  @Override
  public TemplateCollectionModel keys() {
    return base.keys();
  }

  @Override
  public TemplateCollectionModel values() {
    return base.values();
  }

  @Override
  public KeyValuePairIterator keyValuePairIterator() {
    return base.keyValuePairIterator();
  }

  @Override
  @SuppressWarnings({"rawtypes", "deprecation"})
  public Map toMap() throws TemplateModelException {
    return base.toMap();
  }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `mvn -B -pl ipt-app -am test -Dtest=FreeMarkerSetupTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 6, Failures: 0`

If `wrapsMapsLikeStrutsBeanWrapper` fails on `stringKeyed.b`: `SimpleMapModel` does resolve string keys, so first check that `IptObjectWrapper` is the configuration's wrapper. Debug with `assertThat(((FreeMarkerConfigurer) ctx.getBean("freeMarkerConfigurer")).getConfiguration().getObjectWrapper()).isInstanceOf(IptObjectWrapper.class)` before changing templates.

- [ ] **Step 6: Run the whole build**

Run: `mvn -B verify`
Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add ipt-app
git commit -m "feat(web): FreeMarker configured like Struts 7 with page-bean variable fallback"
```

---

### Task 8: Spring Security skeleton (CSRF everywhere, role-protected areas, reserved /api/v1)

**Files:**
- Modify: `ipt-app/pom.xml` (security dependencies)
- Create: `ipt-app/src/main/java/org/gbif/ipt/web/security/SecurityConfig.java`
- Test: `ipt-app/src/test/java/org/gbif/ipt/web/security/SecurityConfigTest.java`

**Interfaces:**
- Consumes: `DoSuffixFilter` ordering (Task 5).
- Produces:
  - Roles `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_PUBLISHER` (and `ROLE_USER`). Rules: `/admin/**` and `/admin-api/**` need ADMIN; `/manage/**` and `/manager-api/**` need MANAGER, PUBLISHER or ADMIN; everything else is public.
  - Anonymous requests to protected paths redirect to `/login`. Login itself arrives in M2.
  - CSRF is on for every non-GET request. Request attribute `_csrf` (`CsrfToken`) is available to templates.
  - `POST /logout` redirects to `/`.
  - `/api/v1/**` is reserved: stateless, no CSRF, no handlers yet.

- [ ] **Step 1: Add dependencies to `ipt-app/pom.xml`**

In `<dependencies>`, after `spring-boot-starter-actuator`:
```xml
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
```
In the test section:
```xml
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security-test</artifactId>
      <scope>test</scope>
    </dependency>
```

- [ ] **Step 2: Write the failing test**

`ipt-app/src/test/java/org/gbif/ipt/web/security/SecurityConfigTest.java`:
```java
package org.gbif.ipt.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

  @Autowired MockMvc mvc;

  @Test
  void anonymousAdminRequestRedirectsToLogin() throws Exception {
    mvc.perform(get("/admin/")).andExpect(status().isFound()).andExpect(redirectedUrl("http://localhost/login"));
  }

  @Test
  void doSuffixedAdminUrlIsProtectedToo() throws Exception {
    mvc.perform(get("/admin/home.do")).andExpect(redirectedUrl("http://localhost/login"));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void managerCannotOpenAdmin() throws Exception {
    mvc.perform(get("/admin/")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminPassesSecurity() throws Exception {
    mvc.perform(get("/admin/nothing-here")).andExpect(status().isNotFound());
  }

  @Test
  void anonymousManageRequestRedirectsToLogin() throws Exception {
    mvc.perform(get("/manage/")).andExpect(redirectedUrl("http://localhost/login"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void plainUserCannotManage() throws Exception {
    mvc.perform(get("/manage/")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "PUBLISHER")
  void publisherCanManage() throws Exception {
    mvc.perform(get("/manage/nothing-here")).andExpect(status().isNotFound());
  }

  @Test
  void postWithoutCsrfTokenIsForbidden() throws Exception {
    mvc.perform(post("/about")).andExpect(status().isForbidden());
  }

  @Test
  void postWithCsrfTokenPassesSecurity() throws Exception {
    MvcResult result = mvc.perform(post("/nothing-here").with(csrf())).andReturn();
    assertThat(result.getResponse().getStatus()).isNotEqualTo(403);
  }

  @Test
  void apiV1IsStatelessAndWithoutCsrf() throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/jobs")).andReturn();
    assertThat(result.getResponse().getStatus()).isNotEqualTo(403);
    assertThat(result.getRequest().getSession(false)).isNull();
  }

  @Test
  void framesAreSameOriginOnly() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
  }

  @Test
  @WithMockUser
  void logoutIsAPostThatReturnsHome() throws Exception {
    mvc.perform(post("/logout").with(csrf())).andExpect(status().isFound()).andExpect(redirectedUrl("/"));
  }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `mvn -B -pl ipt-app -am test -Dtest=SecurityConfigTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL. With Boot's default security, anonymous requests get `401` instead of a redirect to `/login`, and `/api/v1/jobs` gets `403`.

- [ ] **Step 4: Implement**

`ipt-app/src/main/java/org/gbif/ipt/web/security/SecurityConfig.java`:
```java
package org.gbif.ipt.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * Replaces the IPT 3 RequireAdmin/RequireManager interceptors with URL rules, and adds CSRF protection to every form
 * (IPT 3 only protected the login form). Per-resource authorization arrives with the manage pages (M3).
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

  /** Reserved for a token-authenticated REST API (e.g. a later MDT integration); no handlers exist yet. */
  @Bean
  @Order(1)
  SecurityFilterChain apiV1(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/v1/**")
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(AbstractHttpConfigurer::disable);
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain web(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/admin/**", "/admin-api/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/manage/**", "/manager-api/**")
                    .hasAnyRole("MANAGER", "PUBLISHER", "ADMIN")
                    .anyRequest()
                    .permitAll())
        .exceptionHandling(e -> e.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
        .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/"));
    return http.build();
  }

  /** No accounts until M2 ports the IPT users; this also stops Spring Boot generating a default user. */
  @Bean
  UserDetailsService userDetailsService() {
    return username -> {
      throw new UsernameNotFoundException(username);
    };
  }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `mvn -B -pl ipt-app -am test -Dtest=SecurityConfigTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 12, Failures: 0`

- [ ] **Step 6: Run the whole build, since security now applies to every earlier test**

Run: `mvn -B verify`
Expected: `BUILD SUCCESS`. All earlier tests use GET on public paths, so none should need changing. If one fails with 403, it is doing a non-GET request and needs `.with(csrf())`.

- [ ] **Step 7: Commit**

```bash
git add ipt-app
git commit -m "feat(security): role-protected admin/manage areas, CSRF on all forms, reserved /api/v1"
```

---

### Task 9: Struts tag shim (`s.text`, `s.param`) and `csrfField` directive

**Files:**
- Modify: `ipt-app/pom.xml` (add `org.apache.commons:commons-text`)
- Create in `ipt-app/src/main/java/org/gbif/ipt/web/support/freemarker/`: `TemplateParams.java`, `ParamStack.java`, `TextDirective.java`, `ParamDirective.java`, `IptTagLibrary.java`, `CsrfFieldDirective.java`, `IptTemplateVariables.java`
- Test: `ipt-app/src/test/java/org/gbif/ipt/web/support/freemarker/TemplateShimTest.java`
- Test resources: `ipt-app/src/test/resources/templates/probe/{shim,shim_square,unsupported}.ftl`

**Interfaces:**
- Consumes:
  - `Messages.getOrDefault(Locale, String, String, Object...)` (Task 2).
  - `FreeMarkerSharedVariables` and the `fm-probe` controller (Task 7).
  - The Spring Security `CsrfToken` request attribute (Task 8).
- Produces:
  - Shared variable `s`. `s.text` takes `name`, `var`, `escapeHtml`, `escapeJavaScript` and `escapeXml`, with nested `s.param` bodies as arguments; a non-blank body is the default message. `s.param` is body-only. Any other `s.*` tag fails rendering with "is not supported by IPT4 yet". Later milestones add tags.
  - Shared variable `csrfField`: a directive writing `<input type="hidden" name="_csrf" value="…"/>`.

- [ ] **Step 1: Write the probe templates**

`ipt-app/src/test/resources/templates/probe/shim.ftl`:
```
<#ftl output_format="HTML">
A:<@s.text name="menu.about"/>
B:<@s.text name="basic.createdByOn"><@s.param>Ann</@s.param><@s.param>2020</@s.param></@s.text>
C:<@s.text name="no.such.key"/>
D:<@s.text name="no.such.key">Fallback</@s.text>
E:<@s.text var="home" name="menu.home"/>[${home}]
F:<@s.text name="basic.createdByOn" escapeHtml=true><@s.param>Ann</@s.param><@s.param>2020</@s.param></@s.text>
G:<@s.text name="about.notRegistered"/>
H:<@csrfField/>
```

`ipt-app/src/test/resources/templates/probe/shim_square.ftl`:
```
[#ftl output_format="HTML"]
S:[@s.text name="basic.createdByOn"][@s.param]Bo[/@s.param][@s.param]1999[/@s.param][/@s.text]
```

`ipt-app/src/test/resources/templates/probe/unsupported.ftl`:
```
<@s.submit name="save"/>
```

- [ ] **Step 2: Write the failing test**

`ipt-app/src/test/java/org/gbif/ipt/web/support/freemarker/TemplateShimTest.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("fm-probe")
class TemplateShimTest {

  @Autowired MockMvc mvc;

  private String render(String path) throws Exception {
    return mvc.perform(get(path)).andReturn().getResponse().getContentAsString();
  }

  @Test
  void textTagBehavesLikeStruts() throws Exception {
    String html = render("/_probe/shim");

    assertThat(html)
        .contains("A:About")
        .contains("B:created by <b>Ann</b> on 2020")
        .contains("C:no.such.key")
        .contains("D:Fallback")
        .contains("E:[Home]")
        .contains("F:created by &lt;b&gt;Ann&lt;/b&gt; on 2020")
        .contains("G:This IPT installation has not been registered yet.");
  }

  @Test
  void textTagUsesTheRequestLocale() throws Exception {
    String html = render("/_probe/shim?request_locale=fr");

    assertThat(html).contains("A:A propos").contains("G:Cette installation IPT n'a pas encore été enregistrée.");
  }

  @Test
  void squareBracketSyntaxWorks() throws Exception {
    assertThat(render("/_probe/shim_square")).contains("S:created by <b>Bo</b> on 1999");
  }

  @Test
  void csrfFieldWritesTheToken() throws Exception {
    assertThat(render("/_probe/shim")).containsPattern("H:<input type=\"hidden\" name=\"_csrf\" value=\"[^\"]+\"/>");
  }

  @Test
  void unsupportedStrutsTagsFailLoudly() {
    assertThatThrownBy(() -> mvc.perform(get("/_probe/unsupported")))
        .rootCause()
        .hasMessageContaining("s.submit is not supported by IPT4 yet");
  }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `mvn -B -pl ipt-app -am test -Dtest=TemplateShimTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL with a FreeMarker `InvalidReferenceException` for `s`.

- [ ] **Step 4: Add commons-text to `ipt-app/pom.xml` `<dependencies>`. Spring Boot manages its version.**

```xml
    <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-text</artifactId>
    </dependency>
```
Run: `mvn -B -pl ipt-app help:effective-pom -Doutput=/dev/stdout | grep -A2 '<artifactId>commons-text</artifactId>' | head -3`
Expected: a `<version>` line. If Boot does not manage commons-text, add `<commons-text.version>1.15.0</commons-text.version>` to the parent properties and a matching `dependencyManagement` entry. Then re-run.

- [ ] **Step 5: Implement**

`TemplateParams.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import java.util.Map;

/** Reads directive parameters the way Struts tags accepted them (strings, numbers or booleans). */
final class TemplateParams {

  private TemplateParams() {}

  static String string(Map<?, ?> params, String name) throws TemplateModelException {
    Object value = params.get(name);
    if (value == null) {
      return null;
    }
    if (value instanceof TemplateScalarModel scalar) {
      return scalar.getAsString();
    }
    if (value instanceof TemplateNumberModel number) {
      return number.getAsNumber().toString();
    }
    if (value instanceof TemplateBooleanModel bool) {
      return Boolean.toString(bool.getAsBoolean());
    }
    throw new TemplateModelException("Parameter " + name + " must be a string");
  }

  static boolean bool(Map<?, ?> params, String name) throws TemplateModelException {
    Object value = params.get(name);
    if (value == null) {
      return false;
    }
    if (value instanceof TemplateBooleanModel bool) {
      return bool.getAsBoolean();
    }
    if (value instanceof TemplateScalarModel scalar) {
      return Boolean.parseBoolean(scalar.getAsString());
    }
    throw new TemplateModelException("Parameter " + name + " must be a boolean");
  }
}
```

`ParamStack.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import freemarker.core.Environment;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Collects the {@code s.param} values of the innermost enclosing {@code s.text} within one template rendering. */
final class ParamStack {

  private static final Object KEY = new Object();

  private ParamStack() {}

  @SuppressWarnings("unchecked")
  static Deque<List<String>> of(Environment env) {
    Deque<List<String>> stack = (Deque<List<String>>) env.getCustomState(KEY);
    if (stack == null) {
      stack = new ArrayDeque<>();
      env.setCustomState(KEY, stack);
    }
    return stack;
  }
}
```

`TextDirective.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import freemarker.core.Environment;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.StringEscapeUtils;
import org.gbif.ipt.i18n.Messages;

/**
 * Struts {@code <@s.text name="key">}: writes the localised text unescaped, as Struts did. Nested {@code s.param}
 * bodies are the message arguments; other non-blank body text is the default message. {@code var} stores the text
 * in a template variable instead of writing it.
 */
final class TextDirective implements TemplateDirectiveModel {

  private final Messages messages;

  TextDirective(Messages messages) {
    this.messages = messages;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
      throws TemplateException, IOException {
    String name = TemplateParams.string(params, "name");
    if (name == null) {
      throw new TemplateModelException("s.text requires a name");
    }
    List<String> args = new ArrayList<>();
    StringWriter bodyText = new StringWriter();
    Deque<List<String>> stack = ParamStack.of(env);
    stack.push(args);
    try {
      if (body != null) {
        body.render(bodyText);
      }
    } finally {
      stack.pop();
    }
    String defaultMessage = bodyText.toString().isBlank() ? null : bodyText.toString();
    String text = messages.getOrDefault(env.getLocale(), name, defaultMessage, args.toArray());
    if (TemplateParams.bool(params, "escapeHtml")) {
      text = StringEscapeUtils.escapeHtml4(text);
    }
    if (TemplateParams.bool(params, "escapeJavaScript")) {
      text = StringEscapeUtils.escapeEcmaScript(text);
    }
    if (TemplateParams.bool(params, "escapeXml")) {
      text = StringEscapeUtils.escapeXml10(text);
    }
    String var = TemplateParams.string(params, "var");
    if (var != null) {
      env.setVariable(var, new SimpleScalar(text));
    } else {
      env.getOut().write(text);
    }
  }
}
```

`ParamDirective.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/** Struts {@code <@s.param>body</@s.param>}: adds its rendered body as an argument of the enclosing {@code s.text}. */
final class ParamDirective implements TemplateDirectiveModel {

  @Override
  @SuppressWarnings("rawtypes")
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
      throws TemplateException, IOException {
    StringWriter value = new StringWriter();
    if (body != null) {
      body.render(value);
    }
    List<String> args = ParamStack.of(env).peek();
    if (args != null) {
      args.add(value.toString());
    }
  }
}
```

`IptTagLibrary.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Map;
import org.gbif.ipt.i18n.Messages;

/**
 * The subset of the Struts {@code s} tag library that IPT4 templates may still use. Tags are added as milestones port
 * the pages that need them; any other tag fails rendering, so leftovers cannot go unnoticed.
 */
public final class IptTagLibrary implements TemplateHashModel {

  private final Map<String, TemplateModel> tags;

  public IptTagLibrary(Messages messages) {
    this.tags = Map.of("text", new TextDirective(messages), "param", new ParamDirective());
  }

  @Override
  public TemplateModel get(String key) throws TemplateModelException {
    TemplateModel tag = tags.get(key);
    if (tag == null) {
      throw new TemplateModelException("Struts tag s." + key + " is not supported by IPT4 yet");
    }
    return tag;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }
}
```

`CsrfFieldDirective.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateModel;
import java.io.IOException;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.HtmlUtils;

/** {@code <@csrfField/>}: the hidden CSRF token input every POST form needs. */
public final class CsrfFieldDirective implements TemplateDirectiveModel {

  @Override
  @SuppressWarnings("rawtypes")
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
      throws IOException {
    CsrfToken token = currentToken();
    if (token == null) {
      return;
    }
    env.getOut()
        .write(
            "<input type=\"hidden\" name=\""
                + HtmlUtils.htmlEscape(token.getParameterName())
                + "\" value=\""
                + HtmlUtils.htmlEscape(token.getToken())
                + "\"/>");
  }

  private static CsrfToken currentToken() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servlet) {
      return (CsrfToken) servlet.getRequest().getAttribute(CsrfToken.class.getName());
    }
    return null;
  }
}
```

`IptTemplateVariables.java`:
```java
package org.gbif.ipt.web.support.freemarker;

import java.util.Map;
import org.gbif.ipt.i18n.Messages;
import org.springframework.stereotype.Component;

/** Template-wide variables: the Struts tag shim {@code s} and the {@code csrfField} directive. */
@Component
public class IptTemplateVariables implements FreeMarkerSharedVariables {

  private final Messages messages;

  public IptTemplateVariables(Messages messages) {
    this.messages = messages;
  }

  @Override
  public Map<String, Object> variables() {
    return Map.of("s", new IptTagLibrary(messages), "csrfField", new CsrfFieldDirective());
  }
}
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `mvn -B -pl ipt-app -am test -Dtest='TemplateShimTest,FreeMarkerSetupTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 11, Failures: 0`

- [ ] **Step 7: Commit**

```bash
git add ipt-app
git commit -m "feat(web): Struts s.text/s.param template shim and csrfField directive"
```

---

### Task 10: Page context, `PageSupport`, `GlobalModel` and the M0 `cfg` stand-in

**Files:**
- Create in `ipt-app/src/main/java/org/gbif/ipt/web/support/page/`: `ColorScheme.java`, `SiteConfig.java`, `PageContext.java`, `PageSupport.java`, `RequestUrls.java`, `GlobalModel.java`
- Test: `ipt-app/src/test/java/org/gbif/ipt/web/support/page/{ColorSchemeTest,SiteConfigTest,RequestUrlsTest,GlobalModelTest,PageSupportTest}.java`

**Interfaces:**
- Consumes:
  - `IptProperties` (Task 4), `Messages`/`BundleMessages` (Task 2).
  - `IptLocaleResolver.supported` (Task 6), `IptFreeMarkerView.PAGE` (Task 7).
  - The Spring Security `SecurityContextHolder` (Task 8).
- Produces:
  - `record ColorScheme(String primaryColor, String navbarColor, String navbarLinkColor, String navbarGbifLogoColor, String navbarActiveTabColor, String linkColor)` with `static ColorScheme DEFAULT` and `static String toRgb(String hex)`.
  - `SiteConfig` (`@Component`), exposed to templates as `cfg`. It has the IPT 3 `AppConfig` method names: `getBaseUrl()`, `getLogoRedirectUrl()`, `getVersion()`, `getShortVersion()`, `getAnalyticsKey()`, `devMode()`, `getRegistryType()`, `getPortalUrl()`, `getCsrfPageRefreshDelay()`, `getDefaultLocale()`, `getProperty(String)`, `getColorSchemeConfig()`. Constructor: `SiteConfig(IptProperties, Optional<BuildProperties>, Optional<GitProperties>)`.
  - `record PageContext(Messages messages, Locale locale, SiteConfig cfg, String baseUrl, String requestUrl, boolean loggedIn, boolean adminRights, boolean managerRights)`.
  - `class PageSupport(PageContext)`: the IPT 3 BaseAction view getters, listed in the code below.
  - `GlobalModel` (`@ControllerAdvice`) puts `pageCtx` (`PageContext`) and `page` (`PageSupport`) into every controller's model. Controllers replace `page` with a subclass built from `pageCtx`.
  - `RequestUrls.withoutLocaleParameter(String baseUrl, HttpServletRequest)`.

- [ ] **Step 1: Write the failing tests**

`ColorSchemeTest.java`:
```java
package org.gbif.ipt.web.support.page;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ColorSchemeTest {

  @Test
  void convertsHexToCssRgbTriplet() {
    assertThat(ColorScheme.toRgb("#61a861")).isEqualTo("97,168,97");
    assertThat(ColorScheme.toRgb("#ffffff")).isEqualTo("255,255,255");
  }

  @Test
  void defaultsMatchIpt3() {
    assertThat(ColorScheme.DEFAULT.primaryColor()).isEqualTo("#61a861");
    assertThat(ColorScheme.DEFAULT.navbarColor()).isEqualTo("#78b578");
    assertThat(ColorScheme.DEFAULT.linkColor()).isEqualTo("#4ba2ce");
  }
}
```

`SiteConfigTest.java`:
```java
package org.gbif.ipt.web.support.page;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Properties;
import org.gbif.ipt.app.IptProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;

class SiteConfigTest {

  private static IptProperties props(String baseUrl) {
    return new IptProperties(null, baseUrl, "en", null, false);
  }

  @Test
  void versionCarriesTheGitRevisionLikeIpt3() {
    Properties build = new Properties();
    build.setProperty("version", "4.0.0-SNAPSHOT");
    Properties git = new Properties();
    git.setProperty("commit.id.abbrev", "abc1234");

    SiteConfig cfg =
        new SiteConfig(props(null), Optional.of(new BuildProperties(build)), Optional.of(new GitProperties(git)));

    assertThat(cfg.getVersion()).isEqualTo("4.0.0-SNAPSHOT-rabc1234");
    assertThat(cfg.getShortVersion()).isEqualTo("4.0.0-SNAPSHOT");
  }

  @Test
  void versionWithoutBuildInfo() {
    SiteConfig cfg = new SiteConfig(props(null), Optional.empty(), Optional.empty());
    assertThat(cfg.getVersion()).isEqualTo("dev");
  }

  @Test
  void baseUrlLosesTrailingSlashesAndBlankMeansUnset() {
    assertThat(new SiteConfig(props("https://ipt.example.org//"), Optional.empty(), Optional.empty()).getBaseUrl())
        .isEqualTo("https://ipt.example.org");
    assertThat(new SiteConfig(props(" "), Optional.empty(), Optional.empty()).getBaseUrl()).isNull();
  }
}
```

`RequestUrlsTest.java`:
```java
package org.gbif.ipt.web.support.page;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RequestUrlsTest {

  @Test
  void dropsOnlyTheLocaleParameter() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/manage/resource");
    request.setServletPath("/manage/resource");
    request.setQueryString("r=abc&request_locale=fr&v=1.0");

    assertThat(RequestUrls.withoutLocaleParameter("https://ipt.example.org", request))
        .isEqualTo("https://ipt.example.org/manage/resource?r=abc&v=1.0");
  }

  @Test
  void noQuestionMarkWhenOnlyTheLocaleParameterWasPresent() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/about");
    request.setServletPath("/about");
    request.setQueryString("request_locale=fr");

    assertThat(RequestUrls.withoutLocaleParameter("http://localhost", request)).isEqualTo("http://localhost/about");
  }

  @Test
  void usesPathInfoWhenTheServletPathIsEmpty() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/about");
    request.setServletPath("");
    request.setPathInfo("/about");

    assertThat(RequestUrls.withoutLocaleParameter("http://localhost", request)).isEqualTo("http://localhost/about");
  }
}
```

`GlobalModelTest.java`:
```java
package org.gbif.ipt.web.support.page;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import org.gbif.ipt.app.IptProperties;
import org.gbif.ipt.i18n.BundleMessages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class GlobalModelTest {

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private static GlobalModel globalModel(String baseUrl) {
    SiteConfig cfg =
        new SiteConfig(new IptProperties(null, baseUrl, "en", null, false), Optional.empty(), Optional.empty());
    return new GlobalModel(cfg, new BundleMessages());
  }

  private static MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ipt/about");
    request.setContextPath("/ipt");
    request.setServletPath("/about");
    return request;
  }

  @Test
  void derivesBaseUrlFromRequestIncludingContextPath() {
    PageContext ctx = globalModel(null).pageContext(request(), Locale.UK);

    assertThat(ctx.baseUrl()).isEqualTo("http://localhost/ipt");
    assertThat(ctx.requestUrl()).isEqualTo("http://localhost/ipt/about");
  }

  @Test
  void configuredBaseUrlWins() {
    PageContext ctx = globalModel("https://ipt.example.org/").pageContext(request(), Locale.UK);

    assertThat(ctx.baseUrl()).isEqualTo("https://ipt.example.org");
  }

  @Test
  void anonymousVisitorsHaveNoRights() {
    PageContext ctx = globalModel(null).pageContext(request(), Locale.UK);

    assertThat(ctx.loggedIn()).isFalse();
    assertThat(ctx.managerRights()).isFalse();
    assertThat(ctx.adminRights()).isFalse();
  }

  @Test
  void publisherHasManagerButNotAdminRights() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "p@example.org", "n/a", AuthorityUtils.createAuthorityList("ROLE_PUBLISHER")));

    PageContext ctx = globalModel(null).pageContext(request(), Locale.UK);

    assertThat(ctx.loggedIn()).isTrue();
    assertThat(ctx.managerRights()).isTrue();
    assertThat(ctx.adminRights()).isFalse();
  }

  @Test
  void adminHasAllRights() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "a@example.org", "n/a", AuthorityUtils.createAuthorityList("ROLE_ADMIN")));

    PageContext ctx = globalModel(null).pageContext(request(), Locale.UK);

    assertThat(ctx.managerRights()).isTrue();
    assertThat(ctx.adminRights()).isTrue();
  }

  @Test
  void unsupportedLocaleRendersBritishEnglish() {
    assertThat(globalModel(null).pageContext(request(), Locale.GERMAN).locale()).isEqualTo(Locale.UK);
  }
}
```

`PageSupportTest.java`:
```java
package org.gbif.ipt.web.support.page;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.gbif.ipt.app.IptProperties;
import org.gbif.ipt.i18n.BundleMessages;
import org.junit.jupiter.api.Test;

class PageSupportTest {

  private static PageSupport page(Locale locale) {
    SiteConfig cfg = new SiteConfig(new IptProperties(null, null, "en", null, false), Optional.empty(), Optional.empty());
    return new PageSupport(
        new PageContext(
            new BundleMessages(), locale, cfg, "http://localhost", "http://localhost/about", false, false, false));
  }

  @Test
  void exposesBaseActionStyleGetters() {
    PageSupport page = page(Locale.UK);

    assertThat(page.getBaseURL()).isEqualTo("http://localhost");
    assertThat(page.getBase()).isEqualTo("http://localhost");
    assertThat(page.getLogoRedirectURL()).isEqualTo("http://localhost");
    assertThat(page.getRequestURL()).isEqualTo("http://localhost/about");
    assertThat(page.getLocaleLanguage()).isEqualTo("en");
    assertThat(page.getPrimaryColor()).isEqualTo("97,168,97");
    assertThat(page.getNavbarColor()).isEqualTo("120,181,120");
    assertThat(page.getRegisteredIpt()).isNull();
    assertThat(page.getActionWarnings()).isEmpty();
  }

  @Test
  void localisesTexts() {
    PageSupport page = page(Locale.FRENCH);

    assertThat(page.getText("about.notRegistered")).isEqualTo("Cette installation IPT n'a pas encore été enregistrée.");
    assertThat(page.getText("basic.createdByOn", List.of("Ann", "2020"))).isEqualTo("créé par <b>Ann</b> le 2020");
    assertThat(page.getText("no.such.key", "fallback")).isEqualTo("fallback");
  }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -B -pl ipt-app -am test -Dtest='ColorSchemeTest,SiteConfigTest,RequestUrlsTest,GlobalModelTest,PageSupportTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation FAILURE: `cannot find symbol: class ColorScheme`.

- [ ] **Step 3: Implement**

`ColorScheme.java`:
```java
package org.gbif.ipt.web.support.page;

/**
 * UI theme colours (hex). M0 always uses the IPT 3 defaults; M1 reads the installation's scheme from the data
 * directory ({@code config/.uiSettings/ipt-color-scheme.properties}).
 */
public record ColorScheme(
    String primaryColor,
    String navbarColor,
    String navbarLinkColor,
    String navbarGbifLogoColor,
    String navbarActiveTabColor,
    String linkColor) {

  public static final ColorScheme DEFAULT =
      new ColorScheme("#61a861", "#78b578", "#ffffff", "#ffffff", "#ffffff", "#4ba2ce");

  /** {@code "#61a861"} to {@code "97,168,97"}, the form the CSS variables in inc/header.ftl expect. */
  public static String toRgb(String hex) {
    return Integer.valueOf(hex.substring(1, 3), 16)
        + ","
        + Integer.valueOf(hex.substring(3, 5), 16)
        + ","
        + Integer.valueOf(hex.substring(5, 7), 16);
  }
}
```

`SiteConfig.java`:
```java
package org.gbif.ipt.web.support.page;

import java.util.Optional;
import org.gbif.ipt.app.IptProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Component;

/**
 * The configuration the page layout reads, exposed to templates as {@code cfg} with the method names of the IPT 3
 * {@code AppConfig}. M1 replaces this M0 stand-in with the core AppConfig backed by the data directory.
 */
@Component
public class SiteConfig {

  static final int CSRF_PAGE_REFRESH_DELAY_MS = 10 * 60 * 1000;
  private static final String GBIF_UAT_PORTAL = "https://www.gbif-uat.org";

  private final IptProperties props;
  private final String version;
  private final String shortVersion;

  public SiteConfig(IptProperties props, Optional<BuildProperties> build, Optional<GitProperties> git) {
    this.props = props;
    this.shortVersion = build.map(BuildProperties::getVersion).orElse("dev");
    this.version =
        git.map(GitProperties::getShortCommitId).map(id -> shortVersion + "-r" + id).orElse(shortVersion);
  }

  public String getBaseUrl() {
    String base = props.baseUrl();
    if (base == null || base.isBlank()) {
      return null;
    }
    return base.strip().replaceAll("/+$", "");
  }

  /** Configurable in the admin UI from M5; until then the logo links to the base URL. */
  public String getLogoRedirectUrl() {
    return null;
  }

  public String getVersion() {
    return version;
  }

  public String getShortVersion() {
    return shortVersion;
  }

  public String getAnalyticsKey() {
    return props.analyticsKey();
  }

  public boolean devMode() {
    return props.devMode();
  }

  /** Chosen during setup (M2); until then the IPT runs in test mode like an unconfigured IPT 3. */
  public String getRegistryType() {
    return null;
  }

  public String getPortalUrl() {
    return GBIF_UAT_PORTAL;
  }

  public int getCsrfPageRefreshDelay() {
    return CSRF_PAGE_REFRESH_DELAY_MS;
  }

  public String getDefaultLocale() {
    return props.defaultLocale();
  }

  /** Free-form installation properties ({@code ipt.properties}) arrive with the data directory in M1. */
  public String getProperty(String key) {
    return null;
  }

  public ColorScheme getColorSchemeConfig() {
    return ColorScheme.DEFAULT;
  }
}
```

`PageContext.java`:
```java
package org.gbif.ipt.web.support.page;

import java.util.Locale;
import org.gbif.ipt.i18n.Messages;

/** Per-request facts every page bean needs; built by {@link GlobalModel}. */
public record PageContext(
    Messages messages,
    Locale locale,
    SiteConfig cfg,
    String baseUrl,
    String requestUrl,
    boolean loggedIn,
    boolean adminRights,
    boolean managerRights) {}
```

`PageSupport.java`:
```java
package org.gbif.ipt.web.support.page;

import java.util.List;
import java.util.Locale;

/**
 * The view half of the IPT 3 {@code BaseAction}: templates reach these getters as top-level variables
 * ({@code ${baseURL}}) and through {@code action}. Page-specific beans extend this class.
 */
public class PageSupport {

  private final PageContext ctx;

  public PageSupport(PageContext ctx) {
    this.ctx = ctx;
  }

  protected PageContext context() {
    return ctx;
  }

  public SiteConfig getCfg() {
    return ctx.cfg();
  }

  public String getBase() {
    return ctx.baseUrl();
  }

  public String getBaseURL() {
    return ctx.baseUrl();
  }

  public String getLogoRedirectURL() {
    String url = ctx.cfg().getLogoRedirectUrl();
    return url != null ? url : ctx.baseUrl();
  }

  /** The current URL without {@code request_locale}, for the language menu. */
  public String getRequestURL() {
    return ctx.requestUrl();
  }

  public Locale getLocale() {
    return ctx.locale();
  }

  public String getLocaleLanguage() {
    return ctx.locale().getLanguage();
  }

  public boolean isLoggedIn() {
    return ctx.loggedIn();
  }

  public boolean isAdminRights() {
    return ctx.adminRights();
  }

  public boolean isManagerRights() {
    return ctx.managerRights();
  }

  /** The logged-in IPT user; user accounts arrive in M2. */
  public Object getCurrentUser() {
    return null;
  }

  /** The registered IPT installation; registration arrives in M1. */
  public Object getRegisteredIpt() {
    return null;
  }

  public Object getDefaultOrganisation() {
    return null;
  }

  public List<String> getActionWarnings() {
    return List.of();
  }

  public List<String> getActionErrors() {
    return List.of();
  }

  public List<String> getActionMessages() {
    return List.of();
  }

  public String getPrimaryColor() {
    return ColorScheme.toRgb(ctx.cfg().getColorSchemeConfig().primaryColor());
  }

  public String getNavbarColor() {
    return ColorScheme.toRgb(ctx.cfg().getColorSchemeConfig().navbarColor());
  }

  public String getNavbarLinkColor() {
    return ColorScheme.toRgb(ctx.cfg().getColorSchemeConfig().navbarLinkColor());
  }

  public String getNavbarGbifLogoColor() {
    return ColorScheme.toRgb(ctx.cfg().getColorSchemeConfig().navbarGbifLogoColor());
  }

  public String getNavbarActiveTabColor() {
    return ColorScheme.toRgb(ctx.cfg().getColorSchemeConfig().navbarActiveTabColor());
  }

  public String getLinkColor() {
    return ColorScheme.toRgb(ctx.cfg().getColorSchemeConfig().linkColor());
  }

  public String getText(String key) {
    return ctx.messages().get(ctx.locale(), key);
  }

  public String getText(String key, List<?> args) {
    return ctx.messages().get(ctx.locale(), key, args.toArray());
  }

  public String getText(String key, String[] args) {
    return ctx.messages().get(ctx.locale(), key, (Object[]) args);
  }

  public String getText(String key, String defaultValue) {
    return ctx.messages().getOrDefault(ctx.locale(), key, defaultValue);
  }

  public String getText(String key, String defaultValue, List<?> args) {
    return ctx.messages().getOrDefault(ctx.locale(), key, defaultValue, args.toArray());
  }

  public String getText(String key, String defaultValue, String[] args) {
    return ctx.messages().getOrDefault(ctx.locale(), key, defaultValue, (Object[]) args);
  }

  public String getTextWithDynamicArgs(String key, String... args) {
    return ctx.messages().get(ctx.locale(), key, (Object[]) args);
  }
}
```

`RequestUrls.java`:
```java
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
```

`GlobalModel.java`:
```java
package org.gbif.ipt.web.support.page;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.gbif.ipt.i18n.Messages;
import org.gbif.ipt.web.support.freemarker.IptFreeMarkerView;
import org.gbif.ipt.web.support.i18n.IptLocaleResolver;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Adds the default page bean to every controller model. Controllers needing page-specific getters replace
 * {@value #PAGE} with a {@link PageSupport} subclass built from the {@value #PAGE_CONTEXT} attribute.
 */
@ControllerAdvice
public class GlobalModel {

  public static final String PAGE = IptFreeMarkerView.PAGE;
  public static final String PAGE_CONTEXT = "pageCtx";

  private final SiteConfig cfg;
  private final Messages messages;

  public GlobalModel(SiteConfig cfg, Messages messages) {
    this.cfg = cfg;
    this.messages = messages;
  }

  @ModelAttribute
  public void addPage(Model model, HttpServletRequest request, Locale locale) {
    PageContext ctx = pageContext(request, locale);
    model.addAttribute(PAGE_CONTEXT, ctx);
    model.addAttribute(PAGE, new PageSupport(ctx));
  }

  PageContext pageContext(HttpServletRequest request, Locale locale) {
    String baseUrl =
        Optional.ofNullable(cfg.getBaseUrl())
            .orElseGet(() -> ServletUriComponentsBuilder.fromContextPath(request).build().toUriString());
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    boolean loggedIn = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    Set<String> roles = loggedIn ? AuthorityUtils.authorityListToSet(auth.getAuthorities()) : Set.of();
    boolean admin = roles.contains("ROLE_ADMIN");
    boolean manager = admin || roles.contains("ROLE_MANAGER") || roles.contains("ROLE_PUBLISHER");
    Locale pageLocale = IptLocaleResolver.supported(locale).orElse(Locale.UK);
    return new PageContext(
        messages,
        pageLocale,
        cfg,
        baseUrl,
        RequestUrls.withoutLocaleParameter(baseUrl, request),
        loggedIn,
        admin,
        manager);
  }
}
```

- [ ] **Step 4: Run the tests to verify they pass, including the fm-probe tests**

`GlobalModel` now adds `page` before the probe controller replaces it.

Run: `mvn -B -pl ipt-app -am test -Dtest='ColorSchemeTest,SiteConfigTest,RequestUrlsTest,GlobalModelTest,PageSupportTest,FreeMarkerSetupTest,TemplateShimTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 27, Failures: 0`

- [ ] **Step 5: Commit**

```bash
git add ipt-app
git commit -m "feat(web): page context, BaseAction-compatible PageSupport and global page model"
```

---

### Task 11: `ftl-migrate`, the re-runnable template conversion tool

**Files:**
- Create: `tools/ftl-migrate/FtlMigrate.java`, `tools/ftl-migrate/test.sh`
- Create: `tools/ftl-migrate/test/input/{angle,square,script}.ftl`, `tools/ftl-migrate/test/expected/{angle,square,script}.ftl`

**Interfaces:**
- Consumes: nothing. It is a standalone single-file Java program run with `java tools/ftl-migrate/FtlMigrate.java <sourceDir> <targetDir>`.
- Produces: converted `.ftl` files under the target directory with the same relative paths, plus `WARN <file>:<line>: …` lines on stderr. Rewrites:
  1. `/WEB-INF/pages/` becomes `/`.
  2. `Session.curr_user` becomes `currentUser`.
  3. `Parameters.` (not already `RequestParameters.`) becomes `RequestParameters.`.
  4. A CSRF field is inserted on the line after every POST `<form …>` that starts its line: `<@csrfField/>`, or `[@csrfField/]` in square-bracket templates. Idempotent.
- Warnings: POST forms not at line start (likely inside JS strings), and the OGNL leftovers `%{`, `#attr`, `stack.`, `#parameters`, `#session`.

- [ ] **Step 1: Write the fixtures (the failing test)**

`tools/ftl-migrate/test/input/angle.ftl`:
```
<#include "/WEB-INF/pages/inc/header.ftl">
<#if (Session.curr_user)??>${Session.curr_user.email}</#if>
<#if Parameters.showAll??>all</#if>
<form action="${baseURL}/manage/x.do" method="post" id="f">
  <input name="a"/>
</form>
<form action="search" method="get"></form>
<a href="%{foo}">x</a>
```

`tools/ftl-migrate/test/expected/angle.ftl`:
```
<#include "/inc/header.ftl">
<#if (currentUser)??>${currentUser.email}</#if>
<#if RequestParameters.showAll??>all</#if>
<form action="${baseURL}/manage/x.do" method="post" id="f">
<@csrfField/>
  <input name="a"/>
</form>
<form action="search" method="get"></form>
<a href="%{foo}">x</a>
```

`tools/ftl-migrate/test/input/square.ftl`:
```
[#ftl output_format="HTML"]
[#include "/WEB-INF/pages/inc/menu.ftl"/]
    <FORM method='POST'
          action="${baseURL}/login.do">
[@s.text name="portal.login"/]
</FORM>
```

`tools/ftl-migrate/test/expected/square.ftl`:
```
[#ftl output_format="HTML"]
[#include "/inc/menu.ftl"/]
    <FORM method='POST'
          action="${baseURL}/login.do">
[@csrfField/]
[@s.text name="portal.login"/]
</FORM>
```

`tools/ftl-migrate/test/input/script.ftl` and `tools/ftl-migrate/test/expected/script.ftl` (identical; the form inside a JS string must not be touched):
```
<#ftl output_format="HTML">
<script>
  var html = '<form method="post" action="x">';
</script>
```

`tools/ftl-migrate/test.sh`:
```bash
#!/usr/bin/env bash
# Tests FtlMigrate: expected output, idempotency, and warnings.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT

java "$here/FtlMigrate.java" "$here/test/input" "$out/once" 2> "$out/report.txt"
diff -ru "$here/test/expected" "$out/once"

java "$here/FtlMigrate.java" "$out/once" "$out/twice" 2> /dev/null
diff -ru "$here/test/expected" "$out/twice"

grep -q "angle.ftl:9: Struts/OGNL construct '%{' needs manual migration" "$out/report.txt"
grep -q "script.ftl:3: POST form not at line start, add the CSRF field manually" "$out/report.txt"
echo "ftl-migrate: OK"
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `chmod +x tools/ftl-migrate/test.sh && tools/ftl-migrate/test.sh`
Expected: FAIL with `error: file not found: .../FtlMigrate.java`.

- [ ] **Step 3: Implement**

`tools/ftl-migrate/FtlMigrate.java`:
```java
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Converts IPT 3 (Struts) FreeMarker templates for IPT4. Re-runnable and idempotent, so templates changed upstream can
 * be converted again. See docs/design.md, section 5.
 *
 * <p>Usage: {@code java tools/ftl-migrate/FtlMigrate.java <sourceDir> <targetDir>}
 */
public class FtlMigrate {

  private static final String FORM = "<form\\b[^>]*\\bmethod\\s*=\\s*[\"']post[\"'][^>]*>";
  private static final Pattern POST_FORM = Pattern.compile(FORM, Pattern.CASE_INSENSITIVE);
  private static final Pattern POST_FORM_AT_LINE_START =
      Pattern.compile("(?m)^[ \\t]*" + FORM, Pattern.CASE_INSENSITIVE);
  private static final Pattern FIRST_FTL_TAG = Pattern.compile("([<\\[])[#@]");
  private static final Pattern PARAMETERS = Pattern.compile("(?<![\\w.])Parameters\\.");
  private static final List<String> LEFTOVERS = List.of("%{", "#attr", "stack.", "#parameters", "#session");

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println("Usage: java FtlMigrate.java <sourceDir> <targetDir>");
      System.exit(2);
    }
    Path source = Path.of(args[0]);
    Path target = Path.of(args[1]);
    List<Path> templates;
    try (Stream<Path> files = Files.walk(source)) {
      templates = files.filter(p -> p.toString().endsWith(".ftl")).sorted().toList();
    }
    for (Path file : templates) {
      Path relative = source.relativize(file);
      String migrated = migrate(Files.readString(file, StandardCharsets.UTF_8));
      Path out = target.resolve(relative.toString());
      Files.createDirectories(out.getParent());
      Files.writeString(out, migrated, StandardCharsets.UTF_8);
      report(relative, migrated);
    }
    System.err.println("Migrated " + templates.size() + " templates into " + target);
  }

  static String migrate(String template) {
    String out = template.replace("/WEB-INF/pages/", "/");
    out = out.replace("Session.curr_user", "currentUser");
    out = PARAMETERS.matcher(out).replaceAll("RequestParameters.");
    return insertCsrfFields(out, csrfField(out));
  }

  static String csrfField(String template) {
    Matcher m = FIRST_FTL_TAG.matcher(template);
    return m.find() && m.group(1).equals("[") ? "[@csrfField/]" : "<@csrfField/>";
  }

  static String insertCsrfFields(String template, String field) {
    Matcher m = POST_FORM_AT_LINE_START.matcher(template);
    StringBuilder out = new StringBuilder();
    int last = 0;
    while (m.find()) {
      out.append(template, last, m.end());
      String rest = template.substring(m.end()).stripLeading();
      if (!rest.startsWith("<@csrfField") && !rest.startsWith("[@csrfField")) {
        out.append('\n').append(field);
      }
      last = m.end();
    }
    return out.append(template.substring(last)).toString();
  }

  static void report(Path file, String template) {
    Matcher m = POST_FORM.matcher(template);
    while (m.find()) {
      int lineStart = template.lastIndexOf('\n', m.start()) + 1;
      if (!template.substring(lineStart, m.start()).isBlank()) {
        System.err.printf(
            "WARN %s:%d: POST form not at line start, add the CSRF field manually%n", file, lineOf(template, m.start()));
      }
    }
    String[] lines = template.split("\n", -1);
    for (int i = 0; i < lines.length; i++) {
      for (String marker : LEFTOVERS) {
        if (lines[i].contains(marker)) {
          System.err.printf("WARN %s:%d: Struts/OGNL construct '%s' needs manual migration%n", file, i + 1, marker);
        }
      }
    }
  }

  private static int lineOf(String text, int index) {
    int line = 1;
    for (int i = 0; i < index; i++) {
      if (text.charAt(i) == '\n') {
        line++;
      }
    }
    return line;
  }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `tools/ftl-migrate/test.sh`
Expected: last line `ftl-migrate: OK`

- [ ] **Step 5: Dry-run on all baseline templates and keep the warning count for the record**

```bash
B=03582bcf20daa3c27262c2f5e50f0d308db2c2db
stage="$(mktemp -d)"
git -C ~/code/gbif/ipt archive "$B" src/main/webapp/WEB-INF/pages | tar -x -C "$stage" --strip-components=5
java tools/ftl-migrate/FtlMigrate.java "$stage" "$stage-out" 2> /tmp/ftl-migrate-report.txt
tail -1 /tmp/ftl-migrate-report.txt; grep -c '^WARN' /tmp/ftl-migrate-report.txt
rm -rf "$stage" "$stage-out"
```
Expected: `Migrated 116 templates into ...` plus a WARN count. Quote both numbers in the commit message body. The warnings are the M2–M5 manual-work list.

- [ ] **Step 6: Commit**

```bash
git add tools/ftl-migrate
git commit -m "feat(tools): re-runnable FreeMarker template migration from IPT 3"
```

---

### Task 12: Static assets, layout templates and the about page

**Files:**
- Create: `ipt-app/src/main/resources/static/{js,styles,images}/**` (vendored from the baseline)
- Create: `ipt-app/src/main/resources/templates/{about.ftl,inc/header.ftl,inc/menu.ftl,inc/footer.ftl,inc/languages.ftl}` (migrated, then edited by hand)
- Modify: `ipt-app/src/main/resources/static/js/global.js` (CSRF header for jQuery AJAX)
- Create: `ipt-app/src/main/java/org/gbif/ipt/web/portal/{AboutController,AboutPage,HomeController}.java`
- Test: `ipt-app/src/test/java/org/gbif/ipt/web/portal/AboutPageTest.java`

**Interfaces:**
- Consumes: everything from Tasks 4–11. `GlobalModel.PAGE` and `GlobalModel.PAGE_CONTEXT`, `PageSupport`, `PageContext`, `FtlMigrate`.
- Produces:
  - `GET /about` (and `/about.do`).
  - `GET /` redirects to `/about`. M2 replaces this with the portal home.
  - `AboutPage extends PageSupport` with `getTitle()`, `getIptKey()`, `getIptDescription()`, `getHostingOrganisationName()`, `getPortalUrl()`.
  - Static files at `/js/**`, `/styles/**`, `/images/**`.

- [ ] **Step 1: Write the failing test**

`ipt-app/src/test/java/org/gbif/ipt/web/portal/AboutPageTest.java`:
```java
package org.gbif.ipt.web.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class AboutPageTest {

  @Autowired MockMvc mvc;

  private String html(MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(request)
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/html;charset=UTF-8"))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Test
  void rendersTheIpt3AboutPage() throws Exception {
    String html = html(get("/about"));

    assertThat(html)
        .contains("<title>About</title>")
        .contains("About this IPT installation")
        .contains("This IPT installation has not been registered yet.")
        .contains("--color-gbif-primary: 97,168,97;")
        .contains("href=\"http://localhost/styles/main.css\"")
        .contains("Integrated Publishing Toolkit (IPT) Version")
        .contains("TEST MODE")
        .containsPattern("<meta name=\"_csrf\" content=\"[^\"]+\"/>")
        .doesNotContain("<@s.", "[@s.", "${");
  }

  @Test
  void loginFormInMenuCarriesCsrfToken() throws Exception {
    assertThat(html(get("/about")))
        .containsPattern("action=\"http://localhost/login.do\" method=\"post\"[^>]*>\\s*<input type=\"hidden\" name=\"_csrf\"");
  }

  @Test
  void legacyDoUrlWorks() throws Exception {
    assertThat(html(get("/about.do"))).contains("About this IPT installation");
  }

  @Test
  void headRequestOnDoUrl() throws Exception {
    mvc.perform(head("/about.do")).andExpect(status().isOk());
  }

  @Test
  void switchesToFrenchAndRemembersIt() throws Exception {
    MockHttpSession session = new MockHttpSession();

    assertThat(html(get("/about").param("request_locale", "fr").session(session)))
        .contains("<title>A propos</title>")
        .contains("Cette installation IPT n'a pas encore été enregistrée.");
    assertThat(html(get("/about").session(session))).contains("À propos de cette installation IPT");
  }

  @Test
  void languageMenuLinksKeepTheCurrentPage() throws Exception {
    assertThat(html(get("/about").param("request_locale", "fr")))
        .contains("href=\"http://localhost/about?request_locale=es\"")
        .doesNotContain("request_locale=fr&amp;request_locale");
  }

  @Test
  void escapesHostileQueryStringInLanguageLinks() throws Exception {
    // a client can send the query unencoded; it is echoed into the language links via requestURL
    String html =
        html(
            get("/about")
                .with(
                    request -> {
                      request.setQueryString("x=\"><script>alert(1)</script>");
                      return request;
                    }));

    assertThat(html)
        .doesNotContain("<script>alert(1)</script>")
        .contains("&lt;script&gt;alert(1)&lt;/script&gt;");
  }

  @Test
  void honoursForwardedHeaders() throws Exception {
    String html =
        html(
            get("/about")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "ipt.example.org")
                .header("X-Forwarded-Prefix", "/ipt"));

    assertThat(html).contains("href=\"https://ipt.example.org/ipt/styles/main.css\"");
  }

  @Test
  void anonymousMenuHasNoManageOrAdminLinks() throws Exception {
    assertThat(html(get("/about"))).doesNotContain("href=\"http://localhost/manage/\"", "href=\"http://localhost/admin/\"");
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminMenuHasManageAndAdminLinks() throws Exception {
    assertThat(html(get("/about")))
        .contains("href=\"http://localhost/manage/\"")
        .contains("href=\"http://localhost/admin/\"");
  }

  @Test
  void servesVendoredStaticFiles() throws Exception {
    mvc.perform(get("/styles/main.css")).andExpect(status().isOk());
    mvc.perform(get("/js/jquery/jquery-3.7.1.min.js")).andExpect(status().isOk());
  }

  @Test
  void rootRedirectsToAboutUntilThePortalHomeIsPorted() throws Exception {
    mvc.perform(get("/")).andExpect(status().isFound()).andExpect(redirectedUrl("/about"));
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -pl ipt-app -am test -Dtest=AboutPageTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL. `/about` returns 404.

- [ ] **Step 3: Vendor the static assets unchanged and commit them separately (a large, review-free diff)**

```bash
B=03582bcf20daa3c27262c2f5e50f0d308db2c2db
mkdir -p ipt-app/src/main/resources/static
git -C ~/code/gbif/ipt archive "$B" src/main/webapp/js src/main/webapp/styles src/main/webapp/images \
  | tar -x -C ipt-app/src/main/resources/static --strip-components=3
ls ipt-app/src/main/resources/static
git add ipt-app/src/main/resources/static
git commit -m "chore(app): vendor IPT 3 static assets unchanged from baseline 03582bcf"
```
Expected `ls` output: `images  js  styles`

- [ ] **Step 4: Migrate the five layout templates with the tool and commit the raw result**

```bash
B=03582bcf20daa3c27262c2f5e50f0d308db2c2db
stage="$(mktemp -d)"
P=src/main/webapp/WEB-INF/pages
git -C ~/code/gbif/ipt archive "$B" "$P/about.ftl" "$P/inc/header.ftl" "$P/inc/menu.ftl" "$P/inc/footer.ftl" "$P/inc/languages.ftl" \
  | tar -x -C "$stage" --strip-components=5
java tools/ftl-migrate/FtlMigrate.java "$stage" ipt-app/src/main/resources/templates
rm -rf "$stage"
grep -n 'csrfField' ipt-app/src/main/resources/templates/inc/menu.ftl
git add ipt-app/src/main/resources/templates
git commit -m "chore(app): migrate IPT 3 layout and about templates with ftl-migrate"
```
Expected: stderr ends with `Migrated 5 templates`. The `grep` shows one `[@csrfField/]` line right after the menu's login `<form ... method="post" ...>`.

- [ ] **Step 5: Hand edits. Add CSRF meta tags to `inc/header.ftl`.**

In `ipt-app/src/main/resources/templates/inc/header.ftl`, replace:
```
    <meta name="viewport" content="width=device-width, initial-scale=1">
```
with:
```
    <meta name="viewport" content="width=device-width, initial-scale=1">
    [#if _csrf??]
    <meta name="_csrf" content="${_csrf.token}"/>
    <meta name="_csrf_header" content="${_csrf.headerName}"/>
    [/#if]
```

- [ ] **Step 6: Hand edits. Make logout a POST in `inc/menu.ftl` (documented deviation).**

Replace the dropdown logout item:
```
                                    <li>
                                        <a class="dropdown-item menu-link" href="${baseURL}/logout.do">
                                            <svg class="account-dropdown-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24" data-testid="LogoutIcon">
                                                <path d="m17 7-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"></path>
                                            </svg>
                                            [@s.text name="menu.logout"/]
                                        </a>
                                    </li>
```
with:
```
                                    <li>
                                        <form action="${baseURL}/logout" method="post">
                                            [@csrfField/]
                                            <button type="submit" class="dropdown-item menu-link">
                                                <svg class="account-dropdown-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24" data-testid="LogoutIcon">
                                                    <path d="m17 7-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"></path>
                                                </svg>
                                                [@s.text name="menu.logout"/]
                                            </button>
                                        </form>
                                    </li>
```
Replace the small-screen logout link:
```
                            <a class="nav-link ps-0 show-xl-smaller" href="${baseURL}/logout.do">
                                [@s.text name="menu.logout"/]
                            </a>
```
with:
```
                            <form action="${baseURL}/logout" method="post" class="d-inline">
                                [@csrfField/]
                                <button type="submit" class="nav-link ps-0 show-xl-smaller btn btn-link">
                                    [@s.text name="menu.logout"/]
                                </button>
                            </form>
```

- [ ] **Step 7: Hand edits. Send the CSRF token with jQuery AJAX.**

Append to `ipt-app/src/main/resources/static/js/global.js`:
```js

// IPT4: send the Spring Security CSRF token (meta tags in inc/header.ftl) with every jQuery AJAX request.
$(function () {
  var token = $('meta[name="_csrf"]').attr('content');
  var header = $('meta[name="_csrf_header"]').attr('content');
  if (token && header) {
    var headers = {};
    headers[header] = token;
    $.ajaxSetup({ headers: headers });
  }
});
```

- [ ] **Step 8: Write the about controller, page bean and root redirect**

`ipt-app/src/main/java/org/gbif/ipt/web/portal/AboutPage.java`:
```java
package org.gbif.ipt.web.portal;

import java.util.UUID;
import org.gbif.ipt.web.support.page.PageContext;
import org.gbif.ipt.web.support.page.PageSupport;

/** View getters of the IPT 3 {@code AboutAction}. Registration details arrive with the registration manager (M1). */
public class AboutPage extends PageSupport {

  public AboutPage(PageContext ctx) {
    super(ctx);
  }

  /** Registered name, else {@code about.title} from ipt.properties, else the default text. */
  public String getTitle() {
    String configured = getCfg().getProperty("about.title");
    return configured != null ? configured : getText("about.title");
  }

  public UUID getIptKey() {
    return null;
  }

  public String getIptDescription() {
    return null;
  }

  public String getHostingOrganisationName() {
    return null;
  }

  public String getPortalUrl() {
    return getCfg().getPortalUrl();
  }
}
```

`ipt-app/src/main/java/org/gbif/ipt/web/portal/AboutController.java`:
```java
package org.gbif.ipt.web.portal;

import org.gbif.ipt.web.support.page.GlobalModel;
import org.gbif.ipt.web.support.page.PageContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class AboutController {

  @GetMapping("/about")
  public String about(@ModelAttribute(name = GlobalModel.PAGE_CONTEXT, binding = false) PageContext ctx, Model model) {
    model.addAttribute(GlobalModel.PAGE, new AboutPage(ctx));
    return "about";
  }
}
```

`ipt-app/src/main/java/org/gbif/ipt/web/portal/HomeController.java`:
```java
package org.gbif.ipt.web.portal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Until the portal home page is ported (M2), the root shows the about page. */
@Controller
public class HomeController {

  @GetMapping("/")
  public String home() {
    return "redirect:/about";
  }
}
```

- [ ] **Step 9: Run the tests to verify they pass**

Run: `mvn -B -pl ipt-app -am test -Dtest=AboutPageTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 12, Failures: 0`

If a template fails with an `InvalidReferenceException`, the message names the variable. Add the missing getter to `PageSupport` or `SiteConfig`, using the IPT 3 `BaseAction`/`AppConfig` method name. Do not edit the template.

- [ ] **Step 10: Look at the page in a browser**

Run `IPT_DATA_DIR=/tmp/ipt4-data mvn -pl ipt-app -am spring-boot:run -Dspring-boot.run.profiles=dev` in the background. Open `http://localhost:7001/about` (for example with the chrome-devtools MCP `navigate_page` + `take_screenshot`). Compare with an IPT 3 about page, such as `https://ipt.gbif.org/about.do`. Expected: same navbar, about card and footer. Differences other than the TEST MODE badge and the version text are bugs. Stop the server afterwards.

- [ ] **Step 11: Commit**

```bash
git add ipt-app
git commit -m "feat(portal): about page rendered from the IPT 3 templates, CSRF meta tags and POST logout"
```

---

### Task 13: Logging to the data directory (`debug.log`, `admin.log`)

**Files:**
- Create: `ipt-app/src/main/resources/logback-spring.xml`
- Test: `ipt-app/src/test/java/org/gbif/ipt/app/LoggingConfigurationTest.java`

**Interfaces:**
- Consumes: property `ipt.log-dir` (Task 4 `application.yml`, default `$IPT_DATA_DIR/logs`).
- Produces:
  - `<log-dir>/debug.log`: all loggers; `org.gbif` at DEBUG.
  - `<log-dir>/admin.log`: `org.gbif` loggers at WARN and above.
  - Both use the IPT 3 pattern `%-5p %d{dd-MMM-yyyy HH:mm:ss} [%c] - %m%n`, roll at 10 MB and keep 7 files, as IPT 3 did. The admin log viewer (M5) reads these files.

- [ ] **Step 1: Write the failing test**

`ipt-app/src/test/java/org/gbif/ipt/app/LoggingConfigurationTest.java`:
```java
package org.gbif.ipt.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "ipt.log-dir=target/logging-test/logs")
class LoggingConfigurationTest {

  @Test
  void writesIpt3StyleDebugAndAdminLogs() throws Exception {
    Logger log = LoggerFactory.getLogger("org.gbif.ipt.logging.probe");
    String marker = UUID.randomUUID().toString();

    log.debug("debug {}", marker);
    log.warn("warn {}", marker);

    Path dir = Path.of("target/logging-test/logs");
    String debugLog = Files.readString(dir.resolve("debug.log"));
    String adminLog = Files.readString(dir.resolve("admin.log"));

    assertThat(debugLog).contains("debug " + marker).contains("warn " + marker);
    assertThat(adminLog).contains("warn " + marker).doesNotContain("debug " + marker);
    assertThat(adminLog)
        .containsPattern(
            "WARN  \\d{2}-\\S{3,4}-\\d{4} \\d{2}:\\d{2}:\\d{2} \\[org\\.gbif\\.ipt\\.logging\\.probe\\] - warn " + marker);
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -pl ipt-app -am test -Dtest=LoggingConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL with `NoSuchFileException: target/logging-test/logs/debug.log`.

- [ ] **Step 3: Implement**

`ipt-app/src/main/resources/logback-spring.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- Same files, pattern and rolling as IPT 3 (config/LoggingConfiguration.java), so the admin log viewer is unchanged. -->
<configuration>
  <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
  <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

  <springProperty scope="context" name="logDir" source="ipt.log-dir"/>
  <property name="IPT_PATTERN" value="%-5p %d{dd-MMM-yyyy HH:mm:ss} [%c] - %m%n"/>

  <appender name="DEBUG_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${logDir}/debug.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.FixedWindowRollingPolicy">
      <fileNamePattern>${logDir}/debug.log.%i</fileNamePattern>
      <minIndex>1</minIndex>
      <maxIndex>7</maxIndex>
    </rollingPolicy>
    <triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
      <maxFileSize>10MB</maxFileSize>
    </triggeringPolicy>
    <encoder>
      <pattern>${IPT_PATTERN}</pattern>
      <charset>UTF-8</charset>
    </encoder>
  </appender>

  <appender name="ADMIN_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${logDir}/admin.log</file>
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
      <level>WARN</level>
    </filter>
    <rollingPolicy class="ch.qos.logback.core.rolling.FixedWindowRollingPolicy">
      <fileNamePattern>${logDir}/admin.log.%i</fileNamePattern>
      <minIndex>1</minIndex>
      <maxIndex>7</maxIndex>
    </rollingPolicy>
    <triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
      <maxFileSize>10MB</maxFileSize>
    </triggeringPolicy>
    <encoder>
      <pattern>${IPT_PATTERN}</pattern>
      <charset>UTF-8</charset>
    </encoder>
  </appender>

  <logger name="org.gbif" level="DEBUG">
    <appender-ref ref="ADMIN_FILE"/>
  </logger>
  <logger name="org.springframework" level="WARN"/>
  <logger name="freemarker" level="WARN"/>

  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="DEBUG_FILE"/>
  </root>
</configuration>
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -B -pl ipt-app -am test -Dtest=LoggingConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 1, Failures: 0`

- [ ] **Step 5: Run the whole build. Other test contexts must now log to the default directory without errors.**

Run: `mvn -B verify`
Expected: `BUILD SUCCESS`, with no `ERROR in ch.qos.logback` lines in the output.

- [ ] **Step 6: Commit**

```bash
git add ipt-app
git commit -m "feat(app): IPT 3 compatible debug.log and admin.log in the data directory"
```

---

### Task 14: Docker image

**Files:**
- Create: `docker/Dockerfile`, `.dockerignore`, `tools/docker-smoke.sh`

**Interfaces:**
- Consumes: the `ipt-app` boot jar `ipt-app/target/ipt-4.0.0-SNAPSHOT.jar` (Task 4), and `/actuator/health`, `/about` and logging from earlier tasks.
- Produces:
  - Image `gbif/ipt:4.0.0-SNAPSHOT`, running as user `ipt` on port `8080`.
  - `IPT_DATA_DIR=/srv/ipt` declared as `VOLUME`, and a `HEALTHCHECK` on `/actuator/health`.
  - Arguments pass through to the jar, so M1's `docker run … gbif/ipt:4 migrate --from … --to …` works.

- [ ] **Step 1: Write the smoke test (the failing test)**

`tools/docker-smoke.sh`:
```bash
#!/usr/bin/env bash
# Builds the IPT4 image, starts it and checks health, the about page, file logging and the non-root user.
set -euo pipefail
image=${1:-gbif/ipt:4.0.0-SNAPSHOT}
port=${PORT:-18080}
name="ipt4-smoke-$$"

docker build -f docker/Dockerfile -t "$image" .
docker run -d --name "$name" -p "$port:8080" "$image" > /dev/null
trap 'docker rm -f "$name" > /dev/null' EXIT

for _ in $(seq 1 90); do
  if curl -fs "http://localhost:$port/actuator/health" | grep -q '"UP"'; then break; fi
  sleep 2
done
curl -fs "http://localhost:$port/actuator/health" | grep -q '"UP"'
curl -fs "http://localhost:$port/about.do" | grep -q 'About this IPT installation'
curl -fs "http://localhost:$port/about?request_locale=fr" | grep -q 'propos de cette installation IPT'
docker exec "$name" test -s /srv/ipt/logs/debug.log
test "$(docker exec "$name" id -un)" = "ipt"
echo "docker smoke test: OK"
```

Run: `chmod +x tools/docker-smoke.sh && tools/docker-smoke.sh`
Expected: FAIL with `failed to read dockerfile: open docker/Dockerfile: no such file or directory`.

- [ ] **Step 2: Implement**

`.dockerignore`:
```
**/target
.idea
*.iml
.git
docs
```

`docker/Dockerfile`:
```dockerfile
# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:25-jre
LABEL org.opencontainers.image.title="GBIF IPT 4" \
      org.opencontainers.image.source="https://github.com/gbif/ipt"
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system ipt \
    && useradd --system --gid ipt --home-dir /opt/ipt ipt \
    && mkdir -p /opt/ipt /srv/ipt \
    && chown ipt:ipt /srv/ipt
ENV IPT_DATA_DIR=/srv/ipt
VOLUME /srv/ipt
COPY --from=build /src/ipt-app/target/ipt-*.jar /opt/ipt/ipt.jar
USER ipt
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -fs http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/opt/ipt/ipt.jar"]
```

- [ ] **Step 3: Run the smoke test to verify it passes**

Run: `tools/docker-smoke.sh`
Expected: last line `docker smoke test: OK`. The first build downloads all dependencies and takes several minutes; run it in the background and wait for completion.

- [ ] **Step 4: Commit**

```bash
git add docker .dockerignore tools/docker-smoke.sh
git commit -m "build: Docker image running the IPT4 jar as non-root with /srv/ipt data volume"
```

---

### Task 15: CI, Renovate and the vendored-asset manifest

**Files:**
- Create: `.github/workflows/build.yml`, `renovate.json`, `tools/assets/manifest.json`
- Test: `ipt-app/src/test/java/org/gbif/ipt/app/VendoredAssetsManifestTest.java`

**Interfaces:**
- Consumes: `ipt-app/src/main/resources/static/**` (Task 12), `tools/ftl-migrate/test.sh` (Task 11), `docker/Dockerfile` (Task 14).
- Produces:
  - `tools/assets/manifest.json`, an object `{"libraries": [{"name", "npm", "version", "paths"}]}`. Each library is written on one line with key order `name, npm, version, paths`, which the Renovate regex manager matches.
  - A GitHub Actions workflow `build`.

Jenkins integration (docker.gbif.org push) is in M6, together with the release pipeline. M0 only has GitHub Actions; this is recorded in `docs/deviations.md` (Task 16).

- [ ] **Step 1: Write the failing test**

`ipt-app/src/test/java/org/gbif/ipt/app/VendoredAssetsManifestTest.java`:
```java
package org.gbif.ipt.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Every vendored library listed in tools/assets/manifest.json exists, in the one-line form Renovate parses. */
class VendoredAssetsManifestTest {

  private static final Path MANIFEST = Path.of("../tools/assets/manifest.json");
  private static final Path STATIC = Path.of("src/main/resources/static");

  @Test
  void listedFilesExistAndEntriesAreRenovateReadable() throws Exception {
    String text = Files.readString(MANIFEST);
    JsonNode libraries = JsonMapper.builder().build().readTree(text).get("libraries");

    assertThat(libraries.size()).isGreaterThan(10);
    for (int i = 0; i < libraries.size(); i++) {
      JsonNode library = libraries.get(i);
      JsonNode paths = library.get("paths");
      for (int j = 0; j < paths.size(); j++) {
        assertThat(STATIC.resolve(paths.get(j).asString())).as(library.get("name").asString()).exists();
      }
      if (!library.get("npm").isNull() && !library.get("version").isNull()) {
        assertThat(text)
            .contains(
                "\"npm\": \"" + library.get("npm").asString() + "\", \"version\": \""
                    + library.get("version").asString() + "\"");
      }
    }
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -pl ipt-app -am test -Dtest=VendoredAssetsManifestTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL with `NoSuchFileException: ../tools/assets/manifest.json`.

- [ ] **Step 3: Implement**

`tools/assets/manifest.json` (versions read from file names and headers at the baseline; `null` means the version was not recorded upstream and must be identified at the next update):
```json
{
  "comment": "Third-party front-end libraries vendored under ipt-app/src/main/resources/static. Renovate proposes updates via the npm datasource; re-vendor the files by hand and review the pages visually. Keep each library on one line.",
  "libraries": [
    {"name": "bootstrap", "npm": "bootstrap", "version": "5.0.1", "paths": ["js/bootstrap/bootstrap.bundle.min.js", "styles/bootstrap/bootstrap.min.css"]},
    {"name": "bootstrap-icons", "npm": "bootstrap-icons", "version": null, "paths": ["styles/bootstrap-icons/font/bootstrap-icons.css"]},
    {"name": "jquery", "npm": "jquery", "version": "3.7.1", "paths": ["js/jquery/jquery-3.7.1.min.js"]},
    {"name": "jquery-ui", "npm": "jquery-ui", "version": "1.14.2", "paths": ["js/jquery/jquery-ui-1.14.2.min.js"]},
    {"name": "jquery-ui-css", "npm": null, "version": "1.12.1", "paths": ["styles/jquery/jquery-ui.min-1.12.1.css"]},
    {"name": "datatables", "npm": "datatables.net", "version": "1.13.6", "paths": ["js/jquery/jquery.dataTables-1.13.6.min.js", "styles/dataTables/jquery.dataTables-1.13.6.min.css"]},
    {"name": "datatables-responsive", "npm": "datatables.net-responsive", "version": "2.5.0", "paths": ["js/jquery/dataTables.responsive-2.5.0.min.js", "styles/dataTables/responsive.dataTables-2.5.0.min.css"]},
    {"name": "select2", "npm": "select2", "version": "4.0.13", "paths": ["js/select2/select2-4.0.13.full.min.js", "js/select2/select2-4.0.13.min.js", "styles/select2/select2-4.0.13.min.css"]},
    {"name": "select2-bootstrap4-theme", "npm": "@ttskch/select2-bootstrap4-theme", "version": null, "paths": ["styles/select2/select2-bootstrap4.min.css"]},
    {"name": "bootstrap-datepicker", "npm": "bootstrap-datepicker", "version": "1.9.0", "paths": ["js/datepicker/bootstrap-datepicker.min.js", "styles/datepicker/bootstrap-datepicker.min.css"]},
    {"name": "leaflet", "npm": "leaflet", "version": "0.7.7", "paths": ["js/leaflet/leaflet.js", "styles/leaflet/leaflet.css"]},
    {"name": "leaflet-locationfilter", "npm": null, "version": "0.1", "paths": ["js/leaflet/locationfilter.js", "styles/leaflet/locationfilter.css"]},
    {"name": "jszip", "npm": "jszip", "version": "3.10.1", "paths": ["js/jszip/jszip-3.10.1.min.js"]},
    {"name": "html5sortable", "npm": "html5sortable", "version": "0.13.3", "paths": ["js/sortable/html5sortable-0.13.3.js", "js/sortable/html5sortable-0.13.3.min.js"]},
    {"name": "modernizr", "npm": null, "version": "2.6.2", "paths": ["js/modernizr.js"]},
    {"name": "jconfirmation", "npm": null, "version": null, "paths": ["js/jconfirmation.jquery.js"]},
    {"name": "ajaxfileupload", "npm": null, "version": null, "paths": ["js/ajaxfileupload.js"]}
  ]
}
```

`renovate.json`:
```json
{
  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
  "extends": ["config:recommended"],
  "labels": ["dependencies"],
  "packageRules": [
    {
      "matchUpdateTypes": ["minor", "patch"],
      "matchCurrentVersion": "!/^0/",
      "automerge": true,
      "automergeType": "pr"
    },
    {
      "matchUpdateTypes": ["major"],
      "automerge": false
    },
    {
      "matchManagers": ["custom.regex"],
      "automerge": false,
      "description": "Vendored front-end libraries: re-vendor the files and check the pages visually"
    },
    {
      "matchPackageNames": ["io.frictionlessdata:tableschema-java"],
      "automerge": false,
      "description": "GBIF fork pins Jackson 2; FrictionlessJackson2CompatibilityTest must pass"
    }
  ],
  "customManagers": [
    {
      "customType": "regex",
      "managerFilePatterns": ["/^tools/assets/manifest\\.json$/"],
      "matchStrings": ["\"npm\": \"(?<depName>[^\"]+)\", \"version\": \"(?<currentValue>[^\"]+)\""],
      "datasourceTemplate": "npm"
    }
  ],
  "schedule": ["after 11pm every weekday", "before 5am every weekday", "every weekend"],
  "prHourlyLimit": 2,
  "prConcurrentLimit": 20
}
```

`.github/workflows/build.yml`:
```yaml
name: build

on:
  push:
    branches: [main]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
          cache: maven
      - name: Build and test
        run: mvn -B verify
      - name: Template migration tool
        run: tools/ftl-migrate/test.sh
      - name: Docker image
        run: docker build -f docker/Dockerfile -t gbif/ipt:ci .
```

- [ ] **Step 4: Run the tests and validate the Renovate config**

Run: `mvn -B -pl ipt-app -am test -Dtest=VendoredAssetsManifestTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 1, Failures: 0`

Run (needs Node.js; skip with a note in the commit body if Node is unavailable): `npx --yes --package renovate -- renovate-config-validator renovate.json`
Expected: `Config validated successfully`

- [ ] **Step 5: Commit**

```bash
git add .github renovate.json tools/assets ipt-app/src/test/java/org/gbif/ipt/app/VendoredAssetsManifestTest.java
git commit -m "build: GitHub Actions CI, Renovate with vendored front-end library tracking"
```

---

### Task 16: Porting tools, audit and deviation log

**Files:**
- Create: `tools/upstream-delta.sh`, `tools/golden/capture.sh`, `tools/audit/get-mutators.sh`
- Create (generated): `docs/audits/get-mutators.md`
- Create: `docs/deviations.md`

**Interfaces:**
- Consumes: the `baseline:` line of `docs/BASELINE.md` (Task 1), and the legacy repo at `$LEGACY` (default `~/code/gbif/ipt`).
- Produces:
  - `tools/upstream-delta.sh [from] [to]`: upstream commits, each with its changed files tagged by IPT4 destination (`core`, `web`, `ftl`, `static`, `i18n`, `test`, `other`).
  - `tools/golden/capture.sh <ipt3-base-url> <out-dir> [shortname…]`: status, header and body files for each legacy public endpoint. They are the golden files for M2 parity tests.
  - `docs/audits/get-mutators.md`: a table of state-changing GET links (template, line, code). Pages are converted to POST as they are ported in M2–M5.

- [ ] **Step 1: `tools/upstream-delta.sh`**

```bash
#!/usr/bin/env bash
# Lists IPT 3 commits made after the IPT4 port baseline, each changed file tagged with where it lands in IPT4.
# Usage: tools/upstream-delta.sh [from (default: baseline)] [to (default: origin/dwc-dp)]
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
LEGACY=${LEGACY:-$HOME/code/gbif/ipt}
from=${1:-$(sed -n 's/^baseline: //p' "$here/../docs/BASELINE.md")}
to=${2:-origin/dwc-dp}

git -C "$LEGACY" fetch --quiet origin || echo "warning: could not fetch $LEGACY, using local refs" >&2
git -C "$LEGACY" log --reverse --format='%n## %h %s (%an, %as)' --name-only "$from..$to" |
  while IFS= read -r line; do
    case "$line" in
      "## "* | "") echo "$line" ;;
      src/main/java/org/gbif/ipt/action/* | src/main/java/org/gbif/ipt/struts2/*) echo "  web    $line" ;;
      src/main/resources/struts*) echo "  web    $line" ;;
      src/main/resources/ApplicationResources_*) echo "  i18n   $line" ;;
      src/main/java/* | src/main/resources/*) echo "  core   $line" ;;
      src/main/webapp/WEB-INF/pages/*) echo "  ftl    $line" ;;
      src/main/webapp/*) echo "  static $line" ;;
      src/test/*) echo "  test   $line" ;;
      *) echo "  other  $line" ;;
    esac
  done
```

Run: `chmod +x tools/upstream-delta.sh && tools/upstream-delta.sh 03582bcf20daa3c27262c2f5e50f0d308db2c2db~3 03582bcf20daa3c27262c2f5e50f0d308db2c2db`
Expected: three `## <sha> <subject>` blocks, each followed by tagged file lines such as `  ftl    src/main/webapp/WEB-INF/pages/manage/overview.ftl`.

- [ ] **Step 2: `tools/golden/capture.sh`**

```bash
#!/usr/bin/env bash
# Captures responses of a running IPT 3 as golden files for IPT4 parity tests (read-only GET/HEAD requests).
# Usage: tools/golden/capture.sh <ipt3-base-url> <out-dir> [resource-shortname ...]
set -euo pipefail
base=${1:?IPT 3 base URL, e.g. https://ipt.gbif.org}
out=${2:?output directory}
shift 2
mkdir -p "$out"

capture() { # <name> <path>
  curl -sS -D "$out/$1.headers" -o "$out/$1.body" -w '%{http_code}\n' "$base$2" > "$out/$1.status"
}

capture inventory-dataset /inventory/dataset
capture inventory-v2-dataset /inventory/v2/dataset
capture api-resources /api/resources
capture api-health /api/health
capture rss /rss.do
capture dcat /dcat.do
capture about /about.do

for r in "$@"; do
  capture "resource-$r" "/resource?r=$r"
  capture "eml-$r" "/eml.do?r=$r"
  capture "rtf-$r" "/rtf.do?r=$r"
  curl -sS -I "$base/archive.do?r=$r" > "$out/archive-$r.headers"
  modified=$(grep -i '^last-modified:' "$out/archive-$r.headers" | cut -d' ' -f2- | tr -d '\r' || true)
  if [ -n "$modified" ]; then
    curl -sS -o /dev/null -w '%{http_code}\n' -H "If-Modified-Since: $modified" "$base/archive.do?r=$r" \
      > "$out/archive-$r.if-modified-since.status"
  fi
done
echo "Captured $(ls "$out" | wc -l | tr -d ' ') files into $out"
```

Run: `chmod +x tools/golden/capture.sh && tools/golden/capture.sh https://ipt.gbif.org /tmp/ipt3-golden && cat /tmp/ipt3-golden/inventory-dataset.status`
Expected: `Captured 21 files into /tmp/ipt3-golden`, then `200`. These are 7 endpoints × 3 files. Do not commit captures; M2 decides which instance and resources become the parity fixtures.

- [ ] **Step 3: `tools/audit/get-mutators.sh` and the generated report**

```bash
#!/usr/bin/env bash
# Lists links and JavaScript navigations in the IPT 3 templates that change state with a GET request.
# IPT4 protects state changes with CSRF tokens, so each must become a POST form when its page is ported.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
LEGACY=${LEGACY:-$HOME/code/gbif/ipt}
base=${1:-$(sed -n 's/^baseline: //p' "$here/../../docs/BASELINE.md")}
actions='delete|remove|undelete|publish|cancel|makePublic|makePrivate|register|reserveDoi|deleteDoi|deleteVersion|logout|ynchronize|updateExtension|updateDataPackage|Reload|Automap|changeTokens|associateWithNetwork|addManager|deleteManager|addNetwork|deleteNetwork|resource-'
pattern="(href=|location\\.href|window\\.location|\\(location\\)\\.attr)[^;]*(${actions})"

echo "# GET requests that change state (IPT 3 baseline ${base:0:8})"
echo
echo "Generated by \`tools/audit/get-mutators.sh\`. Each row must become a POST with a CSRF token when its template is ported."
echo
echo "| Template | Line | Code |"
echo "|---|---|---|"
git -C "$LEGACY" grep -n -E "$pattern" "$base" -- 'src/main/webapp/WEB-INF/pages' |
  sed -E "s#^${base}:src/main/webapp/WEB-INF/pages/##" |
  while IFS=: read -r file line code; do
    code=$(printf '%s' "$code" | sed -E 's/^[[:space:]]+//; s/\|/\\|/g; s/`/'"'"'/g' | cut -c1-160)
    echo "| $file | $line | \`$code\` |"
  done
```

Run:
```bash
chmod +x tools/audit/get-mutators.sh
mkdir -p docs/audits
tools/audit/get-mutators.sh > docs/audits/get-mutators.md
grep -c '^| [a-z]' docs/audits/get-mutators.md
grep 'inc/menu.ftl' docs/audits/get-mutators.md
```
Expected: a positive row count, and the menu's two `logout.do` links listed. Task 12 already converted those, so they are the first two rows resolved.

- [ ] **Step 4: Write `docs/deviations.md`**

```markdown
# Intentional differences from IPT 3

Each entry says what changed and why. Keep this list current: it becomes the IPT 4 release notes.

| Area | IPT 3 | IPT 4 | Why | Since |
|---|---|---|---|---|
| Packaging | WAR for Tomcat/Jetty, RPM | Executable jar and Docker image | One supported runtime; no servlet-container setup | M0 |
| Data directory | Chosen in setup step 1 or `WEB-INF/datadir.location` | `IPT_DATA_DIR` / `ipt.data-dir` only | A jar has no `WEB-INF`; containers configure by environment | M0 |
| Translations | Messages formatted twice, dropping apostrophes (fr: "na pas") | Formatted once ("n'a pas") | Bug fix | M0 |
| Logout | GET link `logout.do` | POST form | CSRF protection of state changes | M0 |
| CSRF | Login form only | Every POST form and AJAX request | Security | M0 |
| Template errors | HTML debug output inside the page | Request fails (error page); debug output only with `ipt.dev-mode` | No stack traces shown to visitors | M0 |
| Accept-Language | Ignored (configured default) | Ignored (configured default) | Unchanged, listed for clarity | M0 |
| Error Prone | Compiled with Error Prone (motherpom) | Plain javac | Error Prone 2.15 in motherpom 61 does not run on JDK 25; re-enable with a current version later | M0 |
| CI | Jenkins | GitHub Actions; Jenkins release pipeline in M6 | M0 has nothing to deploy | M0 |
| Home page | Portal home | Redirect to `/about` until M2 | Portal home is ported in M2 | M0 (temporary) |
| Health check | `/api/health` (JSON) | `/actuator/health` for Docker; `/api/health` returns in M2 with its IPT 3 JSON shape | Container health check needed before the portal is ported | M0 (temporary) |
```

- [ ] **Step 5: Format with GBIF conventions and run everything once more**

Run: `mvn -B spotless:apply && mvn -B verify && tools/ftl-migrate/test.sh`
Expected: `BUILD SUCCESS` and `ftl-migrate: OK`. Spotless adds the GBIF license headers and import order to the Java files.

- [ ] **Step 6: Commit**

```bash
git add tools docs ipt-core ipt-app
git commit -m "docs: porting tools (upstream delta, golden capture, GET audit) and deviation log"
```

---

## Milestone M0 exit check (design spec, section 7)

- [ ] `mvn -B verify` is green on JDK 25.
- [ ] `tools/docker-smoke.sh` prints `docker smoke test: OK`: the image serves the about page with theme colours and locale switching.
- [ ] `git log --oneline` shows one commit per task (plus the two vendoring commits in Task 12), none with AI attribution.
