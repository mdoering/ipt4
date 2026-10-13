# IPT 4 design

IPT 4 is the GBIF Integrated Publishing Toolkit ported from Struts to Spring Boot 4 on Java 25. It keeps IPT 3's
pages, public URLs, data and behaviour; the intentional differences are listed in [deviations.md](deviations.md). This
document describes how IPT 4 is built today. What is covered and what is still open before 4.0 is in
[parity-checklist.md](parity-checklist.md); the reasoning behind individual calls is in [decisions.md](decisions.md).

**Status.** Version `4.0.0-SNAPSHOT`, experimental and not approved by GBIF. Images are published to
`ghcr.io/mdoering/ipt4`, not to Docker Hub.

## Why a port

IPT 3 (`~/code/gbif/ipt`) is a Java web app on Struts 7, Spring XML wiring and FreeMarker, shipped as a WAR. Struts is a
legacy, security-prone framework, and its idioms had leaked into the core: non-web classes took `BaseAction` for i18n
and errors, OGNL bound forms straight onto the shared in-memory `Resource`, and templates relied on value-stack
variables. IPT 3 also had gaps that the port closes: CSRF protection only on the login form, no session id rotation
at login, AJAX actions without access checks, stored secrets encrypted with a key compiled into the code, background
threads never shut down, and XStream persistence that needs reflection into the JDK.

The goal is a simpler, maintainable code base that publishers, the GBIF registry and harvesters cannot tell apart from
IPT 3, with room for a later integration of GBIF's Metabarcoding Data Toolkit (MDT, see §7).

## Key decisions

| Topic | Decision |
|---|---|
| Framework | Spring Boot 4 (Spring Framework 7): Spring MVC, Spring Security, FreeMarker |
| Java | 25 |
| Port baseline | IPT 3 `dwc-dp` branch frozen at the commit in [BASELINE.md](BASELINE.md); later upstream commits are listed by `tools/upstream-delta.sh` and ported by hand |
| Web layer | The Struts actions are kept as page beans run by a small Struts emulation on Spring MVC (§5), so upstream changes still apply |
| Persistence | JSON files in the data directory instead of XStream XML; EML, archives, sources, logos, extensions, vocabularies and schemas stay files. No database |
| IPT 3 data | Converted once, without touching the original, by `java -jar ipt.jar migrate` |
| Packaging | Executable jar and Docker image (`linux/amd64`, `linux/arm64`); no WAR, Tomcat or RPM |
| Look | IPT 3's templates and CSS, on Bootstrap 5.3 |

## 1. Modules and build

```
pom.xml               org.gbif.ipt:ipt-parent, parent org.gbif:motherpom:61, imports the Spring Boot BOM
ipt-core/             model, JSON persistence, services, publishing tasks, validation, i18n, feedback, jobs, HTTP
ipt-legacy-import/    reads IPT 3 data directories with XStream; the migrate command (only module with XStream)
ipt-app/              the Spring Boot application: web layer, security, templates, static assets, migrate dispatch
docker/Dockerfile     multi-stage build; eclipse-temurin:25-jre, non-root user ipt, IPT_DATA_DIR=/srv/ipt
tools/                porting, template migration, image and release scripts (§9)
docs/                 these documents and the Antora user documentation (docs/modules/ROOT)
```

- **Core boundary.** `ipt-core` has no web dependencies. An enforcer rule bans `jakarta.servlet`, `spring-webmvc`,
  Spring Boot, Tomcat, Struts and XStream from it (transitively), and the ArchUnit test `CoreBoundaryTest` bans the
  packages. Core depends on `spring-context` and `spring-security-crypto` only.
- **Other build rules.** The root enforcer rule `no-legacy-http-or-gson` bans HttpClient 4, gbif-httputils and Gson in
  every module. ArchUnit: `XStreamBoundaryTest` (XStream only in `org.gbif.ipt.legacy`), `SharedDateFormatTest` (no
  `DateFormat` shared between threads).
- **Packages.** Core keeps IPT 3's package names (`org.gbif.ipt.{config,model,service,task,utils,validation}`,
  `org.gbif.datapackage`) so upstream patches apply, and adds `org.gbif.ipt.{i18n,feedback,persistence,job,http}`. The
  app has `org.gbif.ipt` (`IptApplication`), `org.gbif.ipt.app` (wiring, start-up, scheduling) and
  `org.gbif.ipt.web.{portal,manage,admin,account,setup,security,error,support}`. The importer is `org.gbif.ipt.legacy`
  with `legacy.v3.{model,xstream}`.
- **Jackson.** Jackson 3 (`tools.jackson`) for the data directory, registry responses and the JSON endpoints. Jackson 2
  stays for the code ported from IPT 3's data-package support (`org.gbif.datapackage`, the Frictionless, Camtrap DP and
  ColDP metadata models, YAML) and for the libraries that need it, among them the tableschema fork;
  `FrictionlessJackson2CompatibilityTest` checks that the Jackson 2 modules converge.
- **HTTP.** `IptHttp` wraps the JDK `HttpClient`, with the proxy from `ipt.properties` and the User-Agent
  `GBIF-IPT/<version>`; downloads stream to files.
- **Logging.** Logback. `logback-spring.xml` writes `debug.log` and `admin.log` (warnings and errors of `org.gbif`, read
  by the admin log page) with IPT 3's pattern into `IPT_LOG_DIR`, by default `<data dir>/logs`, rolling at 10 MB.
- **Libraries.** Struts, Guice, at.favre bcrypt (Spring's `BCryptPasswordEncoder` reads the same hashes), HttpClient 4,
  Gson, native2ascii and buildnumber are gone; build information comes from `spring-boot:build-info` and
  `git-commit-id`. Error Prone is off: motherpom's version does not run on JDK 25.
- **Dependencies** are kept current by Renovate (`renovate.json`), including the vendored front-end libraries listed in
  `tools/assets/manifest.json`.

## 2. Core

- **i18n.** `Messages`/`BundleMessages` load the bundle `ApplicationResources` (9 languages, no base bundle) and format
  each message once (IPT 3 formatted twice and lost apostrophes). English is the fallback per key, so a translation
  missing a key shows English, not the key. `SimpleTextProvider` remains as a facade for ported code.
  `LegacyTextsComparisonTest` checks every key in every language against IPT 3's output.
- **Feedback.** Services report messages through the `Feedback` interface instead of a `BaseAction`. It keeps
  BaseAction's method names (`getText`, `addActionMessage`, `addActionWarning`, `addActionError`, `addFieldError`), so
  ported call sites change only their parameter type. Implementations: `CollectingFeedback`, `LoggingFeedback` (jobs,
  CLI) and, in the app, the request-scoped `RequestFeedback`; pages implement `Feedback` by delegating to it.
- **Access rule.** `ResourceAccessPolicy.canManage(User, Resource)` holds IPT 3's manager rule, used by every manage
  page and API.
- **Wiring.** Core classes carry no Spring annotations (constructor `@Inject` only). `CoreBeans` in the app registers them
  with one `@Import` list plus `@Bean` methods for the data directory, `AppConfig`, the cipher, `IptHttp`, JDBC support,
  the persister and the importer, mirroring IPT 3's Guice module.
- **Configuration.** Deployment settings come from Spring (`IptProperties`: `ipt.data-dir`, `ipt.base-url`,
  `ipt.default-locale`, `ipt.dev-mode`, `ipt.defaults[...]`; also `ipt.trusted-proxies`, `ipt.upload.max-size`,
  `ipt.log-dir`); administrator settings stay in `config/ipt.properties`. Built-in endpoints (registry, DataCite) and
  defaults such as the publishing thread count are IPT 3's `application.properties`, renamed `ipt-defaults.properties`
  and overridable with `ipt.defaults[...]`. The user documentation lists them all (`configuration.adoc`).
- **Jobs.** `JobService` runs publications on a fixed pool of daemon threads, one job per resource, with a `JobHandle`
  per job. `ResourcePublicationManagerImpl` owns its instance; closing it at shutdown cancels running jobs and waits
  for them (`server.shutdown: graceful`). `MonitorScheduler` runs the publishing monitor every 10 s and the extension
  monitor daily (`ipt.scheduling.enabled` turns both off, for tests).
- **Concurrency.** `ResourceLocks` keeps a fair read-write lock per resource. Saves of `resource.json`, EML, data package
  metadata and inferred metadata take the resource's write lock, and pages hold it around a metadata save. Rule: never
  take a manager-wide monitor while holding a resource lock (`ResourceLockOrderTest`). Files that a publication copies
  while an edit may be writing them are written atomically.
- **Main services**, as in IPT 3: `ConfigManager`, `RegistrationManager`, `UserAccountManager`, `ExtensionManager`,
  `VocabulariesManager`, `DataPackageSchemaManager`, `RegistryManager`, `SourceManager`, `ResourceManager` and
  `ResourcePublicationManager`. Parts of IPT 3's `ResourceManagerImpl` were split out: `ResourceIndex`,
  `ResourceLoader`, `ResourceImportService`, `ResourceTypeService`, `ResourceVersioningService`,
  `ResourceMetadataLoader`, `ResourceMetadataInferringService` and `ResourceCoordinator`.

## 3. Data directory

The data directory is set with `IPT_DATA_DIR` (or `ipt.data-dir`); IPT 4 does not start without one. Files marked `*`
are new or changed from IPT 3; the rest keep IPT 3's format.

```
ipt-datadir.json*             marker: format, version, createdBy, created, migratedFrom, keyCheck (written last)
config/ipt.properties         administrator settings, as in IPT 3
config/users.json*            users; bcrypt password hashes as in IPT 3
config/registration.json*     IPT registration and organisations; secrets as {aesgcm}base64(iv|ciphertext|tag)
config/.secret.key*           32-byte AES key, owner-only; or IPT_SECRET_KEY instead
config/.extensions, .vocabularies, .dataPackages, .uiSettings, .gbifreg   as in IPT 3
resources/<name>/resource.json*, inferred-metadata.json*
resources/<name>/eml*.xml, datapackage*.json, metadata*.yaml, *.zip, *.rtf, logo.*, publication.log, sources/
logs/, tmp/ (emptied at start), migration-report.json/.txt (after a migration)
```

- **JSON mapping.** `DataDirJson` maps the core model by field (getters never leak) with deterministic output. Shared
  objects are stored as keys: users by email, organisations by key, extensions by row type, terms by qualified name,
  schemas, tables and fields by name, mapping sources by source name. On load `ResourceLinker` replaces the keys with
  the managed objects and reports missing extensions as IPT 3 did. `JsonResourcePersister`, `UserStore` and
  `RegistrationStore` read and write the files.
- **Writes** go through `AtomicFiles`: temporary file, fsync, atomic move. `SecretCipher` encrypts secrets with AES-256-GCM
  and a fresh IV per save.
- **Start-up check** (`DataDirStartup`, `DataDirFormat`): no location configured → refuse to start; empty or missing
  directory → create the key and marker, then the setup wizard; IPT 4 directory → refuse a newer format version or a
  key that does not match the marker's key check, else start; IPT 3 directory → refuse and print the `migrate` command;
  key without marker (an interrupted setup or migration) or anything else → refuse. There is no upgrader yet: format
  version 1 is the only one.

### Migration from IPT 3

`IptApplication.main` runs `migrate` without starting the web application:

```
java -jar ipt.jar migrate --from <ipt3 dir> --to <new dir> [--dry-run] [--link-artifacts] [--include-logs]
docker run --rm -v /var/lib/ipt3:/old:ro -v ipt-data:/srv/ipt ghcr.io/mdoering/ipt4:main migrate --from /old --to /srv/ipt
```

`LegacyXmlReader` reads IPT 3's XML with IPT 3's XStream mapping directly onto the core model (no separate legacy model;
small helper classes in `legacy.v3`). XStream may only create `org.gbif.**` classes and JDK value types, because
uploaded resource folders are read the same way. `Migrator`:

1. checks that the source is an IPT 3 directory and the target empty, and that neither contains the other;
2. creates the new secret key;
3. copies the configuration, converting `users.xml` to `users.json` (IPT 3's PBE-encrypted passwords are decrypted and
   hashed with bcrypt) and `registration2.xml` or the older `registration.xml` to `registration.json` (secrets
   re-encrypted);
4. converts each resource's `resource.xml` and `inferredMetadata.xml`, reloads the JSON and compares the counts of
   sources, mappings, fields, versions and managers, and copies the other files (`--link-artifacts` hard-links
   published versions instead). SQL passwords are re-encrypted; a resource that fails keeps its files and
   `resource.xml.ipt3` and is reported;
5. copies the logs if asked, writes `migration-report.json` and `.txt`, and writes the marker last.

The original directory is only read. Uploaded IPT 3 or IPT 4 resource folders go through the same reader
(`ResourceFolderImporter`).

## 4. Security

- **Login.** Spring Security form login on IPT 3's `/login` with the `email` and `password` fields, checked against
  `UserAccountManager` (bcrypt). The session id changes at login; the session timeout comes from `ipt.properties`.
  After login only local paths are followed.
- **Authorization.** URL rules: `/admin/**` and `/admin-api/**` need an admin; `/manage/**`, `/manager-api/**` and
  `/resource/preview` a manager (`IptAuthorization`). Rights are read from the user record on every request, so role
  changes and deleted accounts take effect at once. Per resource, `ManageGuard` (manage pages) and `PortalGuard`
  (public pages and downloads) apply `ResourceAccessPolicy` and IPT 3's 401/404/410 answers.
- **CSRF** on every POST: forms carry `<@csrfField/>`, `inc/header.ftl` writes the token into meta tags, and
  `static/js/csrf.js` adds it to same-origin AJAX requests and turns IPT 3's state-changing links into POST forms. No
  state changes on GET.
- **Input.** `SanitizeHtmlFilter` (ported) strips markup from every parameter except the rich-text and DocBook fields,
  which may hold only the editors' elements; changed fields become field errors.
- **Headers.** `X-Frame-Options: SAMEORIGIN`; `Cache-Control: no-store` except downloads and static assets; session
  cookie `HttpOnly` and `SameSite=Lax` (`Secure` by setting). `X-Forwarded-*` headers are used only from
  `ipt.trusted-proxies` (`TrustedProxyForwardedHeaderFilter`).
- **Secrets** (organisation, DOI agency and SQL passwords) are encrypted in the data directory and never written into
  pages or logs.

## 5. Web layer

### Pages and controllers

IPT 3's actions were ported as page beans rather than rewritten, so that their code stays close enough to IPT 3 for
upstream changes to apply:

- **Pages.** 48 `@PageBean` classes (Spring prototype beans), one per IPT 3 action. `PageSupport` is the view half of
  `BaseAction` (texts, `baseURL`, current user, rights, theme colours); `ActionPage` adds the request, session, IPT 3's
  result names and the `prepare()`/`validate()`/`execute()` lifecycle; `PostActionPage` is IPT 3's `POSTAction` (GET
  shows the form, POST saves, `delete`/`cancel` flags); `ManagerBasePage` and `PortalBasePage` load the resource from `r`
  and `v`.
- **Controllers.** 20 Spring MVC `@Controller`s map the URLs and run a fresh page through `ActionRunner`, passing a map
  from IPT 3 result names to views, which takes the place of `struts.xml`. `ManagePageController` runs `ManageGuard`
  first; `ActionResults` holds the global and per-area results (error pages, login and home redirects). One
  `OverviewController` serves the resource overview, including the `POST /manage/resource-{method}` actions.
- **Lifecycle** (`ActionRunner`): `prepare()`, uploads handed over, parameters bound, sanitiser findings turned into
  field errors, `validate()`, then the action method, or the input view on errors; `afterAction()` and removal of
  uploaded temporary files always run.

### Binding

Parameters are bound onto the page as Struts did, with IPT 3's allow-list and stricter checks:

- Only properties marked `@StrutsParameter(depth = n)` (IPT 3's annotation, kept) are bound, up to that depth, plus a
  page's `extraAllowedFields()`.
- `ActionRunner` checks each path against the objects actually there, not only the declared types: never into files,
  and below the top level never into shared objects (users, organisations, extensions, vocabularies, schemas,
  resources, the IPT, networks, sources).
- A GET binds top-level parameters only, because it carries no CSRF token.
- Lists may grow by at most max(10,000, 10 × parameter count) elements per request; form posts are limited to 10 MB.
- Conversions are IPT 3's, applied per class and property by `StrutsConversions` (year-only dates, comma decimals,
  coordinate ranges, Camtrap DP vocabularies). A checkbox is followed by a hidden `false` field; the first value wins.
- **Working copies.** Metadata editors bind onto a copy of the EML or data package metadata. Only a valid section is
  put on the resource, under its write lock; resource fields posted with a rejected section are restored
  (`ResourceFormFields`).

### Templates

- 115 FreeMarker templates in `ipt-app/src/main/resources/templates`, configured as Struts configured FreeMarker
  (`IptFreeMarkerConfig`): HTML output format with auto-escaping, incompatible improvements 2.3.34, automatic tag
  syntax, a `BeansWrapper` with Struts' map model. Template errors fail the request; `ipt.dev-mode` shows FreeMarker's
  debug output instead.
- **Variables.** `IptFreeMarkerView` resolves names in Struts' order: model, then the page bean (also as `action`), then
  request, session and application attributes. `GlobalModel` adds the messages, warnings and errors; `PageContext`
  holds the request's locale, configuration, base URL, current user and feedback.
- **Struts tags are gone.** `${t("key", args...)}` (`TextFunction`) replaces `<@s.text>`: message texts are markup,
  arguments are escaped. Form fields are plain HTML or the macros in `macros/forms.ftl` (`fieldError`, `propertyText`).
  `tools/ftl-migrate` converts upstream templates the same way.
- Pages include `inc/header.ftl`, `inc/menu.ftl` and `inc/footer.ftl` as in IPT 3; there is no layout macro.
- **Messages** collected during a request go into the model, or into the flash map on a redirect
  (`FeedbackFlashInterceptor`). Start-up problems are shown as warnings (`StartupWarningsInterceptor`).
- **Language.** `request_locale` switches the language for the session (`IptLocaleResolver`, the 7 languages IPT 3
  offers); the default is the configured IPT language, never `Accept-Language`.

### Requests, files and APIs

- **`.do` URLs.** `DoSuffixFilter` strips a trailing `.do` before security and routing, so controllers map clean paths
  and every IPT 3 URL keeps working; generated links keep `.do`, so the registry's endpoints never change.
- **Setup.** `SetupGuard` sends every request to the setup wizard until it is complete; static files, login and the
  health checks stay available.
- **Uploads** are multipart, up to `ipt.upload.max-size` (10 GB), stored under generated names in the data directory's
  `tmp/uploads` (`UploadStore`) and deleted after the request.
- **Downloads** (`DownloadController`: EML, metadata, archive, logo, RTF, logs) check `PortalGuard`, answer
  `If-Modified-Since` with 304 and HEAD without a body, with IPT 3's content types and file names.
- **JSON** (`/inventory/dataset`, `/inventory/v2/dataset`, `/api/resources`, `/api/health`, `/manager-api/*`,
  `/admin-api/*`) keeps the Struts JSON plugin's shapes: page properties under explicit roots, sorted as Struts sorted
  them (`StrutsJson`). RSS renders `portal/rss.ftl`; DCAT streams Turtle from `GenerateDCAT`.
- **Errors.** `IptErrorController` and the page results render IPT 3's error templates (`error/*`, `portal/404|410`,
  `manage/404`) with the matching status.
- **Health.** `/api/health` with IPT 3's JSON; `/actuator/health` for container health checks.

### Static assets

Vendored under `ipt-app/src/main/resources/static/{js,styles,images}`, so `${baseURL}/js/...` URLs are unchanged, and
listed with their versions in `tools/assets/manifest.json`: Bootstrap 5.3.8 (`main.css` keeps 5.0's look), jQuery
3.7.1, jQuery UI, DataTables, select2, Leaflet and others; Roboto is served from `static/styles/fonts/roboto` (no
requests to Google). Summernote, the rich-text editor of three EML sections, is still loaded from cdnjs. The theme
colours from the UI management page become CSS variables in `inc/header.ftl`.

## 6. Delivery

- **CI** (`.github/workflows/build.yml`): every push and pull request runs `mvn -B verify`, the JavaScript tests,
  `tools/ftl-migrate/test.sh` and `tools/docker-smoke.sh` (builds the image, checks health, the setup wizard, file
  logging and the non-root user). Every green build of `main` is published as `ghcr.io/mdoering/ipt4:main` and
  `:sha-<commit>`, for `linux/amd64` and `linux/arm64` (`tools/docker-publish.sh`; the jar is built once, natively).
- **Releases** (`.github/workflows/release.yml`): pushing a tag `v<version>` builds and tests that commit with the version
  from the tag (`tools/set-version.sh`) and publishes `ghcr.io/mdoering/ipt4:<version>`, a final version also as
  `:latest` (`tools/push-image.sh`). Started by hand it is a dry run. The GBIF `Jenkinsfile` does the same, but its push
  to Docker Hub (`gbif/ipt`) is switched off until GBIF approves IPT 4.

## 7. MDT readiness

GBIF's Metabarcoding Data Toolkit (a Node/Express and React app with GBIF.org account login, an asynchronous job API,
DwC-A and DwC-DP output and its own registry installation) is not integrated. What exists for a later integration:

- **Module boundary.** A future `ipt-mdt` module can depend on `ipt-core` alone, whose no-web rule is enforced. That
  keeps both options open: porting the MDT pipeline to Java behind its React UI, or running the MDT beside the IPT and
  importing its output.
- **Import.** `ResourceImportService` creates resources from EML, DwC-A, Frictionless and ColDP packages (it still
  reports through `ActionLogger`, not `Feedback`, and has no `replaceData` yet).
- **Jobs.** `JobService` is generic over job type and key, matching the MDT's job and polling pattern.
- **API.** `/api/v1/**` has its own stateless security chain without CSRF and no handlers yet, reserved for a
  token-authenticated REST API.
- **Accounts.** Authentication is one `IptAuthenticationProvider`; a GBIF.org account provider would be added next to it.
  Users stay keyed by email.
- **Registry.** `RegistrationManager` and `RegistryManager` are web-independent, so MDT datasets could register under the
  IPT's installation.

## 8. Verification

`mvn -B verify` runs about 140 test classes offline (JUnit 6, AssertJ, Mockito, ArchUnit); the user documentation's
`testing.adoc` lists the commands.

- **Core.** IPT 3's core tests, ported, plus tests for the new persistence, cipher, jobs, HTTP and locks. The four
  `*IT` classes in ipt-core never run: motherpom excludes them from surefire and failsafe is not configured.
- **i18n.** `LegacyTextsComparisonTest` reproduces IPT 3's double formatting for every key in all 9 languages and allows
  only the apostrophes IPT 3 lost.
- **Migration.** IPT 3's own fixtures (11 `res1` variants, amphibians, event, registration v1 and v2) go through
  `LegacyXmlReader` and are compared as JSON trees with fixtures converted once by IPT 3's XStream setup
  (`JsonFixturesTest`); `MigratorTest` covers secrets, refusals, dry runs and hard links; `MigrateAndPublishTest`
  migrates a data directory, starts IPT 4 on it and publishes an archive equal to what IPT 3's `GenerateDwcaTest`
  expects. `DataDirStartupTest` covers the start-up refusals.
- **Web.** `@SpringBootTest` with MockMvc on fixture data directories, per area (portal, manage, metadata editors,
  admin, setup, account). Security, upload limits, large forms, error pages, login and the setup wizard are also tested
  over real HTTP (`RANDOM_PORT`), because MockMvc hides forwards, committed responses and multipart limits.
- **Authorization.** Three matrix tests enumerate every handler from Spring's handler mapping: every state-changing
  request without a CSRF token is refused (403); anonymous users are sent to login; users and managers of another
  resource get 401 in the manage area, users and managers 401 in the admin area.
- **Pages.** `CrawlerTest` follows every link and GET form from `/`, `/manage/` and `/admin/`, logged out and as
  admin, and fails on any error status, FreeMarker error or template code left in the HTML. With
  `-Dipt.snapshots=DIR` it writes normalised pages for comparing before and after a change. `TemplatesParseTest`
  parses every template.
- **JSON and registry contract.** `JsonApiTest` pins the inventory, `/api/resources` and `/api/health` shapes taken
  from IPT 3 responses (`tools/golden/capture.sh` captures them from a running IPT 3; no captures are committed);
  `RegistryContractTest` checks the URLs the GBIF registry and harvesters use, with and without `.do`.
- **Registry and DataCite.** `StubRegistry` and a stateful `StubDataCite` (JDK HTTP servers) stand in for GBIF and
  DataCite in the registration and DOI tests. `UatRegistrationTest` (tag `uat`) does the real round trip against
  GBIF UAT and DataCite test; `RegistryExtensionsNetworkTest` (tag `network`) reads rs.gbif.org. Both tags are excluded
  by default.
- **Concurrency.** `ConcurrentUseTest` runs reads, saves and a publication at once (no server errors, no deadlock,
  response times); `ResourceLockOrderTest` checks the lock order.
- **Architecture.** `CoreBoundaryTest`, `XStreamBoundaryTest`, `SharedDateFormatTest` and the enforcer rules (§1).
- **Outside Maven.** JavaScript tests (`node --test`: CSRF helpers, DocBook rendering), `tools/ftl-migrate/test.sh` and
  `tools/docker-smoke.sh`, all run by CI.
- **By hand.** Visual comparison of key pages with IPT 3, and the `uat` run.

Run locally: `mvn -B -DskipTests install && IPT_DATA_DIR=~/ipt4-data mvn -pl ipt-app spring-boot:run
-Dspring-boot.run.profiles=dev` (port 7001, `ipt.dev-mode`), or the Docker image (README).

## 9. Following IPT 3

- **Baseline.** [BASELINE.md](BASELINE.md) names the IPT 3 commit IPT 4 was ported from. `tools/upstream-delta.sh` lists
  the IPT 3 commits since then, each file tagged with where it lands in IPT 4. Core keeps IPT 3's package and class
  names, so core changes apply with `git am --directory=ipt-core`; page beans keep the actions' code.
- **Templates.** Upstream template changes go through `tools/ftl-migrate`, which converts Struts tags, inserts CSRF
  fields and reports what it cannot convert.
- **Porting scripts** used for the initial port: `tools/port/port-core.sh` (the web-free packages, verbatim),
  `tools/port/port-action.py` (an action to a page bean), `tools/audit/get-mutators.sh` (state-changing GET links,
  report in `docs/audits/get-mutators.md`), `tools/golden/capture.sh` (IPT 3 responses as golden files).
- **IPT 3 files most often needed:** `service/manage/impl/{ResourceManagerImpl,ResourcePublicationManagerImpl,
  ResourceImportServiceImpl}.java`, `service/admin/impl/{UserAccountManagerImpl,RegistrationManagerImpl,
  ConfigManagerImpl}.java`, `action/{BaseAction,POSTAction}.java`, `action/manage/{ManagerBaseAction,OverviewAction,
  MetadataAction}.java`, `struts2/{SimpleTextProvider,RequireManagerInterceptor,SanitizeHtmlFilter}.java`,
  `src/main/resources/{struts.xml,struts-*.xml,applicationContext.xml}` and
  `src/main/webapp/WEB-INF/pages/{macros/forms.ftl,inc/header.ftl,inc/menu.ftl}`.

## 10. Risks

| Risk | Mitigation |
|---|---|
| IPT 3 keeps moving | Frozen baseline, `upstream-delta.sh` triage, same package and class names, `ftl-migrate` for templates. Resource-format changes upstream need a matching change in the JSON mapping and the importer |
| JSON shape drift on harvested endpoints | `JsonApiTest` pins the shapes of IPT 3's responses; registry URL contract test |
| FreeMarker set up differently from Struts | Explicit configuration matching Struts, template errors fail the request, full render and crawl tests |
| Binding differences (list clearing, auto-grow, empty values, renumbered indexes) | IPT 3's `prepare()` code kept in the pages, per-section round-trip tests, large-form tests |
| Jackson 2 pinned by the tableschema fork | `FrictionlessJackson2CompatibilityTest`; update the fork if needed |
| Translations | Exact port of IPT 3's bundle handling plus the comparison test; Crowdin files and keys unchanged |
| XStream on Java 25 (importer only) | Restricted type permissions; only in `ipt-legacy-import` |
| Operators losing the WAR and the data directory chooser | Docker-first installation guide, clear start-up messages, the `migrate` command |

## Not built from the original plan

The port started from a milestone plan (`docs/superpowers/plans/`). These parts of it were dropped or changed; the
reasons are in [decisions.md](decisions.md):

- Separate controllers, page beans and form classes per action, and the split of IPT 3's `OverviewAction` into six
  controllers: the actions became page beans run by `ActionRunner` (§5).
- A generated, frozen copy of IPT 3's model for the importer: XStream reads onto the core model.
- A data directory upgrader: not needed until the format changes.
- A parity harness running IPT 3 and IPT 4 side by side in containers, and Playwright screenshot comparisons: IPT 3
  does not build on JDK 25; parity is tested against IPT 3's own test expectations and fixtures, and response shapes
  captured from a running IPT 3.
- Validation errors through a `BindingResult` (`ErrorsFeedback`): pages collect errors as IPT 3's actions did.
- Committed golden files compared in strict mode, and a test failing on a template that is never rendered.
- A layout macro and an `/api/v1` OpenAPI skeleton: still optional.
