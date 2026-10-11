# IPT4: migrate the GBIF IPT from Struts to Spring Boot 4

## Context

The GBIF IPT (`~/code/gbif/ipt`, v3.3.7-SNAPSHOT) is a 63k-LOC Java web app built on Struts 7, Spring XML wiring and FreeMarker, and it ships as a WAR. Struts is a legacy, security-prone framework, and its idioms leaked into the core:
- About 22 non-web classes take `BaseAction` for i18n and errors.
- OGNL binds forms straight onto the shared in-memory `Resource`.
- Templates rely on implicit value-stack variables.
- The code carries 95 `@StrutsParameter` annotations.

There are also gaps a rewrite should close:
- CSRF protection exists only on the login form.
- The session id is not rotated on login.
- Five `ajaxStack` actions have no auth check: `/manage/cancel`, `/manage/peek`, `/manage/mappingPeek`, `/manage/report` and `/admin/bulkReport`.
- A hard-coded PBE key encrypts stored secrets.
- Background threads are never shut down.
- XStream reflection needs `--add-opens`.

**Goal.** A new, simple and maintainable code base in `~/code/gbif/ipt4` (currently empty) on Spring Boot 4 / Java 25. It keeps the IPT's look, public URLs and behaviour. It leaves cheap seams for a later integration of GBIF's **Metabarcoding Data Toolkit (MDT)**, which is not integrated now. The MDT is a Node/Express + React CRA/AntD app with GBIF.org-account login, an async-job REST API, DwC-A/DwC-DP output and its own registry installation.

This plan doubles as the design spec. At M0 it is copied to `ipt4/docs/design.md`.

## Agreed decisions

| Topic | Decision |
|---|---|
| Framework | Spring Boot 4 (Spring Framework 7) with Spring MVC, FreeMarker, Spring Security and Bean Validation |
| Java | 25 LTS |
| Port baseline | `origin/dwc-dp` frozen at `03582bcf20daa3c27262c2f5e50f0d308db2c2db` (master + 165 DwC-DP commits). Later upstream commits are triaged with `tools/upstream-delta.sh` and ported per milestone. |
| Persistence | Jackson JSON files in the data dir replace the XStream XML. EML, archives, sources, logos, extensions, vocabularies and schemas stay as files. No database. |
| Legacy data | One-time, non-destructive `ipt migrate --from <ipt3> --to <ipt4>` |
| Packaging | Executable jar plus Docker image. WAR, Tomcat and RPM/jetty-runner are dropped. |
| Delivery | Milestones M0–M6. Each runs against a migrated real data dir; parity is reached at M6. |
| Styling | Keep templates and CSS. Minor deviations are listed explicitly below. |

## 1. Target architecture

```
ipt4/
  pom.xml              org.gbif.ipt:ipt-parent:4.0.0-SNAPSHOT, parent org.gbif:motherpom:61,
                       imports spring-boot-dependencies BOM (same pattern as ~/code/gbif/registry/pom.xml)
  ipt-core/            model, JSON persistence, services, tasks, validation, i18n, feedback, jobs. NO servlet/MVC/Struts deps
  ipt-legacy-import/   frozen IPT3 model + XStream config + converters + MigrateCommand (only place XStream lives)
  ipt-app/             Spring Boot app: controllers, security, FreeMarker support, templates, static assets, CLI dispatch
  ipt-parity-tests/    -Pparity only: Testcontainers IPT3 vs IPT4 comparison; never deployed
  tools/               ftl-migrate/ (template conversion), assets/ (vendored-lib manifest + update.sh), upstream-delta.sh
  docker/Dockerfile    eclipse-temurin:25-jre, non-root, IPT_DATA_DIR=/srv/ipt, VOLUME, HEALTHCHECK /api/health
```

- **Enforcing the core boundary.** ipt-core uses maven-enforcer `bannedDependencies` (no `jakarta.servlet*`, `spring-webmvc` or `struts*`) plus an ArchUnit test. `spring-web` is allowed for `RestClient`.
- **Packages.** Core keeps the legacy names (`org.gbif.ipt.model|service|task|validation|utils|config`, `org.gbif.datapackage`), so `git am --directory=ipt-core` can apply upstream deltas.
  - New in core: `org.gbif.ipt.{i18n,feedback,persistence,job,http}`.
  - App: `org.gbif.ipt.app`, `org.gbif.ipt.web.{portal,manage,admin,setup,account,api,security,support}`.
  - Legacy import: `org.gbif.ipt.legacy.v3.{model,xstream}`.
- **Starters:** webmvc, freemarker, security, validation, mail, restclient, actuator (health only).
- **Jackson.** Jackson 3 (`tools.jackson`) for all IPT-owned JSON and YAML. Jackson 2 stays transitive for gbif-api, gbif-doi, dwc-dp-analyser-lib and the frictionless fork (pinned 2.15). Annotations are shared between the two. M0 adds a convergence check; the fallback is to update the GBIF frictionless fork.
- **HTTP.** `IptHttp` wraps `RestClient` on the JDK `HttpClient`, adds the proxy from `ipt.properties` and a User-Agent, and downloads via `BodyHandlers.ofFile`. HttpClient 4, gbif-httputils and Gson are removed.
- **Logging.** Logback (Boot default). `logback-spring.xml` writes `${ipt.data-dir}/logs/debug.log` and `admin.log` with the legacy pattern, so the admin log viewer is unchanged. Existing log4j-api calls work via `log4j-to-slf4j`. `LoggingConfiguration` and the `log4j2*.xml` files are deleted.
- **Dropped libraries:** Struts and its plugins, at.favre bcrypt (Spring's `BCryptPasswordEncoder` reads `$2a$12$`), HttpClient 4, Gson, native2ascii, buildnumber (replaced by `spring-boot:build-info` + git-commit-id), `IPTModule` and `applicationContext.xml`.
- **Kept libraries:** dwca-io, dwc-api, gbif-api, gbif-metadata-profile-eml, gbif-doi, gbif-parsers, dwc-dp-analyser-lib, the frictionless fork, json-schema-validator, POI and excel-streaming-reader, openrtf, digester3, tika, owasp-html-sanitizer, jsoup, Lombok, the JDBC drivers, jakarta.mail.

## 2. Core extraction (M1)

1. **i18n.**
   - `Messages`/`Texts` replace `SimpleTextProvider`.
   - `BundleMessages` ports `struts2/SimpleTextProvider.java`: the English fallback is kept, and the bundle `ApplicationResources` is loaded as UTF-8 without the `Locale.setDefault` hack. **It formats once.** IPT 3 ran `MessageFormat` twice, which turned French `n''a` into "na" (verified on 2026-10-10); this fix is recorded in `docs/deviations.md`.
   - A test compares old and new output for every key in all 9 locales.
2. **Feedback.** `Feedback` (`info`/`warn`/`error`/`fieldError`/`texts()`) replaces every `BaseAction` parameter, in:
   - `ResourceManager`, `ResourcePublicationManager`, `ResourceImportService` and `RegistryManager`
   - the `*ManagerImpl` classes and `PublishingMonitor`
   - the validators, including the 232 `getText` calls in `EmlValidator`, converted by regex

   Implementations: `RequestFeedback` (web), `ErrorsFeedback` (wraps `BindingResult`), `LoggingFeedback` (jobs and CLI, locale captured at submit) and `CollectingFeedback` (tests). `ActionLogger`, `ActionErrorCollector`, `StrutsI18n` and the `new BaseAction(...)`-for-getText calls go away.
3. **Relocations.**
   - `RequireManagerInterceptor.isAuthorized` becomes `ResourceAccessPolicy.canManage(User, Resource)` in core, with the rule kept verbatim.
   - `action.portal.OrganizedTaxonomicKeywords` and the related coverage classes move to `org.gbif.ipt.model.metadata`.
   - Struts annotations come off `FrictionlessMetadata`, `CamtrapMetadata` and `DatatableResult`.
   - `config/SetupAction` moves to `web.setup`.
4. **Wiring.**
   - `@ConfigurationProperties("ipt")` holds `dataDir` (`${IPT_DATA_DIR:}`), `upload.maxSize` (10 GB), `publishing.maxThreads` (6) and the registry URLs.
   - `@Bean`s for `DataDir`, `AppConfig`, `JdbcSupport` and `IptHttp`; services use `@Service` with constructor injection.
5. **Jobs.**
   - `JobService` uses a bounded `ThreadPoolTaskExecutor` with `JobHandle` (key, type, Future, `StatusReport`). `ResourcePublicationManager` delegates to it.
   - The `PublishingMonitor` loop becomes `@Scheduled(fixedDelay=10s)` and `ExtensionMonitor` becomes `@Scheduled` daily.
   - Shutdown is graceful: `server.shutdown=graceful`, `awaitTermination`, and `@PreDestroy` cancels running jobs and releases locks.
6. **Concurrency.** A per-resource `ReadWriteLock` in `ResourceIndex` guards apply+save and publication snapshots.

## 3. Data dir v4 and migration

### Format changes

Only the files marked `*` change; everything else is kept and copied unchanged.

```
ipt-datadir.json*        {"format":"ipt","version":1,"createdBy":"4.0.0","migratedFrom":{...}}   (written last = commit marker)
config/ipt.properties    unchanged
config/users.json*       bcrypt hashes kept
config/registration.json*  secrets as {aesgcm}base64(iv|ct|tag)
config/.secret.key*      32 random bytes, mode 600, overridable by IPT_SECRET_KEY
config/.uiSettings, .extensions, .vocabularies, .dataPackages   unchanged
resources/<sn>/resource.json*, inferred-metadata.json*   (schemaVersion field)
resources/<sn>/eml*.xml, datapackage*.json, metadata*.yaml, *.zip, *.rtf, logo.*, publication.log, sources/   unchanged
```

### Jackson mapping (`persistence.DataDirJson`)

- Field visibility, so getters never leak.
- Legacy `omitField` fields become `transient`/`@JsonIgnore`.
- Sources use `@JsonTypeInfo` with `text`, `excel`, `sql` and `url`.
- Output is deterministic (sorted).
- References are stored as keys through mixins:
  - User: email
  - Organisation: UUID
  - Extension: rowType
  - Term: qualified name
  - Schema/table/field: name
  - Mapping source: source name
- On load, `ResourceLinker` swaps the key stubs for the managed instances and reports missing extensions as legacy does. No reflection hacks.

### Supporting classes

- `JsonResourcePersister` plugs into the existing `ResourcePersister` seam on dwc-dp, next to `XStreamResourcePersister` and `ResourceXStreamFactory`.
- `UserStore` and `RegistrationStore` replace the XStream code in `UserAccountManagerImpl` and `RegistrationManagerImpl`.
- `AtomicFiles` writes to a temp file, fsyncs, then does an `ATOMIC_MOVE`.
- `SecretCipher` does AES-256-GCM.

### Startup format check (`DataDirFormat`)

- No `ipt.data-dir` configured: refuse to start with a clear message.
- Empty directory: run setup.
- Marker present: run `DataDirUpgrader` steps (JSON-tree transforms, with a backup first).
- IPT3 layout without a marker: abort and print the `migrate` command.

### Importer (`ipt-legacy-import`)

**Frozen model.** Generated once by a script from the baseline's `model/**`, converters, legacy and inferred classes, and `OrganizedTaxonomicKeywords`.
- XStream gets `aliasPackage` plus the legacy aliases from `ResourceXStreamFactory`.
- Permissions are restricted to `org.gbif.**`, `java.util/math/net`.
- Reference converters are replaced by key stubs, so no network or extension registry is needed.
- `Add-Opens` is set in the boot-jar manifest.

**CLI.** `IptApplication.main` dispatches `migrate` without starting the web context:

```
java -jar ipt.jar migrate --from /old --to /new [--dry-run] [--link-artifacts] [--include-logs]
docker run --rm -v ipt3:/old:ro -v ipt4:/srv/ipt gbif/ipt:4 migrate --from /old --to /srv/ipt
```

**Migration steps.**
1. Validate the source and refuse a non-empty target.
2. Generate the secret key.
3. Copy the config directories.
4. Convert users. Legacy PBE passwords are decrypted by `LegacyPbe` and re-hashed with bcrypt.
5. Convert registration from v2, or from v1 via `model/legacy`. PBE secrets are re-encrypted with AES-GCM.
6. For each resource: `resource.xml` becomes `resource.json` (JDBC passwords re-encrypted) and inferred metadata becomes `inferred-metadata.json`. Pre-2.2.1 `dwca.zip` is renamed. Artifacts are copied, or hard-linked only if immutable and versioned.
7. Write `migration-report.{json,txt}`.
8. Load the result in-process with ipt-core and verify the counts.
9. Write the marker.

## 4. Web layer: Struts → Spring MVC mapping

### Controllers, page beans and forms

Each action becomes three pieces:
- a `@Controller` for routing, binding and redirects;
- a page bean (`XxxPage extends PageSupport`) holding the view-side getters of the old action;
- a form class holding the old `@StrutsParameter` properties.

`PageSupport` replaces the view half of `BaseAction`: `getText`, `baseURL`, `currentUser`, theme colours, role checks and `registeredIpt`. `POSTAction` becomes a handler convention: GET shows input; POST `params="!cancel&!delete"` saves; `params="delete"` deletes; `params="cancel"` cancels.

`OverviewAction` (2.1k LOC) splits into `ManageOverviewController`, `ResourceLifecycleController`, `PublicationController`, `RegistryController`, `DoiController` and `ReplaceMetadataController`. Its `resource-*` wildcard becomes explicit POST mappings.

### Value-stack compatibility

`IptFreeMarkerView` resolves template variables that are missing from the model against the page bean, and aliases `action` to it. `@ControllerAdvice GlobalModel` adds `baseURL`, `currentUser`, `cfg`, `locale`, the theme colours, `enabledLocales`, `actionMessages`/`actionErrors`/`warnings` and `fieldErrors`.

### Interceptors

| Struts interceptor | Spring replacement |
|---|---|
| RequireAdmin / RequireManager (login) | `SecurityFilterChain`: `/admin/**` and `/admin-api/**` need ADMIN. `/manage/**` and `/manager-api/**` need MANAGER, PUBLISHER or ADMIN. Saved-request redirect after login. |
| RequireManager per resource + ResourceSession | `@Managed Resource` argument resolver: `r` param or session value, then 404 / 401-manager / locked redirect, via `ResourceAccessPolicy`. An ArchUnit rule requires `@Managed` on every `/manage` handler that uses `r`. This closes the ajaxStack gaps, and `/admin/bulkReport` needs ADMIN. |
| PrivateDeletedResource | `@PortalResource Resource` resolver: 404 / 410 / 401 |
| SetupAndCancel | `SetupRedirectInterceptor` (all routes except setup, static assets and `/api/health`) plus the `params="cancel"` mappings |
| RedirectMessage / messageStore | `Feedback` request bean, copied to the model or the `FlashMap` on `redirect:` |
| DefaultLocale + IptI18n | `LocaleChangeInterceptor("request_locale")` + `IptLocaleResolver` (session-based; default from config; only the 7 enabled locales) |
| CsrfLogin | Spring Security CSRF on every non-GET request |
| XssFieldError + SanitizeHtmlFilter | Port `SanitizeHtmlFilter` as-is (`FilterRegistrationBean`); flagged fields become field errors |
| `*-conversion.properties` | `@InitBinder` customisers (`EmlBinderCustomizer`, `CamtrapBinderCustomizer`, `DataPackageBinderCustomizer`) plus global Formatters (legacy multi-pattern dates, URI, camtrap enums) |
| `__checkbox_` | `binder.setFieldMarkerPrefix("__checkbox_")`, so existing hidden fields keep working |
| EmptyToNull | `StringTrimmerEditor(true)` |

### Form binding on a working copy

Handlers no longer bind onto the live `Resource`. Each POST:
1. deep-copies the edited sub-graph (`SerializationUtils.clone(eml)`);
2. applies the section's `clearOwnedLists`, ported verbatim from `MetadataAction.prepare()`;
3. binds with `setAllowedFields(section.allowedFields())` and `autoGrowCollectionLimit=100_000`;
4. validates through `ErrorsFeedback`;
5. on success calls `resourceService.applyEml(...)` under the resource write lock, then redirects to the next section.

Template field names (`eml.associatedParties[0].address.city`) stay unchanged, because Spring property paths use the same syntax.

### Uploads, downloads, JSON and errors

- **Uploads.** `MultipartFile`, with multipart max size from `ipt.upload.max-size` and multipart location `${data-dir}/tmp/uploads`. Tomcat gets `max-parameter-count=100000`, a higher `max-part-count` and `max-http-form-post-size=-1`.
- **Downloads** (`eml`, `metadata`, `archive`, `logo`, `rtf`, `publicationlog`, `sourcelog`, `appLogo`, `raw-source`, `logfile`). `ResponseEntity<FileSystemResource>`, with `checkNotModified` for 304 and HEAD support. Content types and Content-Disposition names are copied from the legacy `ResourceFileAction`.
- **JSON.** Explicit DTO records for `/inventory/dataset`, `/inventory/v2/dataset`, `/api/resources`, `/api/health`, `/manager-api/*` and `/admin-api/*`. These are pinned by golden files captured from IPT3, because the Struts JSON result serialised the whole action.
- **RSS** renders `portal/rss.ftl`. **DCAT** streams `GenerateDCAT`.
- **Errors.** `IptErrorController` and the `@ControllerAdvice` handlers render the existing `error/*.ftl`, `portal/404|410.ftl` and `manage/404.ftl` with the right status codes.

### `.do` compatibility

One `OncePerRequestFilter` runs at highest precedence, before Security. It strips a trailing `.do` from `requestURI`/`servletPath`/`requestURL` and leaves the query string untouched. Controllers map clean paths, and trailing slashes such as `/manage/` are mapped explicitly. Generated public URLs keep `.do`, so the GBIF registry endpoints (`archive.do?r=&v=`, `eml.do?r=`, `resource?r=`, `rss.do`, `dcat.do`, `inventory/dataset`) never change.

### Security

- **Login.** `formLogin` on `/login` with `usernameParameter("email")`. `UserDetailsService` is backed by `UserAccountManager` and passwords use `BCryptPasswordEncoder`.
- **Sessions.** Session-fixation protection is on. The session timeout comes from `ipt.properties`.
- **CSRF.**
  - Every form gets `<@csrfField/>`, inserted by script.
  - `header.ftl` gets a `<meta name="_csrf">` tag, and `global.js` sends it via `$.ajaxSetup`.
  - The iframe uploads in `ajaxfileupload.js` append `?_csrf=`.
- **Headers.** `frameOptions.sameOrigin()`, plus CORS for `/inventory/**` and `/api/**`.
- **No state changes on GET.** An M0 audit finds state-changing GET links and converts them to POST.

## 5. Templates and assets

### Location and FreeMarker configuration

- The 116 `.ftl` files move to `ipt-app/src/main/resources/templates/` and keep the `.ftl` suffix. **Correction (verified 2026-10-10 against struts2-core 7.3.0):** Struts 7's `FreemarkerManager` already sets HTML output format with `ENABLE_IF_DEFAULT` auto-escaping, incompatible improvements 2.3.34, and a `BeansWrapper` with a friendly map model. IPT4 configures exactly that, so escaping behaviour is unchanged.
- `FreeMarkerConfigurer` is set up to match Struts' `FreemarkerManager`:
  - `tag_syntax=auto_detect`
  - the same `incompatible_improvements`
  - `?api` stays disabled (no template uses it)
  - an object wrapper equivalent to `StrutsBeanWrapper`
  - `rethrow` exception handling in tests
- The `datadir::` loader is dropped unless the docs show it is a used feature (it looks unwired on dwc-dp).

### Text function `t` (the Struts tag shim was removed in M7)

Until M7 a runtime shim `s` (`IptTagLibrary`, about 10 Java directives) kept the Struts tags working. M7 removed it. Templates now use FreeMarker only:
- `${t("key", args...)}` (`TextFunction`) replaces `<@s.text>` and `<@s.param>`.
  - It returns HTML markup: message texts are not escaped, as with `s.text`.
  - String arguments are escaped and numbers formatted like `${n}`; markup arguments (a captured `<#assign>`, another `t()`) are inserted as they are.
  - `t(k)?markup_string` gives plain text (Struts' `escapeHtml=true`).
- `s.submit`, `s.hidden`, `s.file`, `s.select` and `s.checkbox` became the HTML that Struts' simple theme wrote. Checkboxes are followed by a hidden `false` field instead of Struts' `__checkbox_` marker; the first value wins, so an unticked box binds false.
- `s.fielderror` became the `fieldError` macro and `s.property` the `propertyText` function, both in `macros/forms.ftl`.
- Lines that held only a Struts tag got `<#t>`/`<#lt>`/`<#rt>`, so FreeMarker's white-space stripping and the rendered pages stay the same.
- The crawler's page snapshots (`-Dipt.snapshots=DIR`) compared every page before and after the removal.

### Script `tools/ftl-migrate/`

The script is re-runnable on upstream templates. It:
- rewrites `/WEB-INF/pages/` paths;
- replaces `Session.curr_user` with `currentUser` and `Parameters.` with `RequestParameters.`;
- inserts `<@csrfField/>` into POST forms;
- converts the Struts tags (see above), keeping white-space stripping, and reports the ones it cannot convert (`s.select`, `s.checkbox`, default texts, OGNL);
- reports leftover OGNL (`%{}`, `#attr`, `stack.`).

About 20–25 templates need manual work: `manage/overview*.ftl`, `manage/eml/contacts.ftl`, `macros/metadata*.ftl` (JS index renaming), `mapping*.ftl`, `dataPackageMapping*.ftl`, `translation*.ftl`, `source.ftl`, `create_input.ftl`, `dp-metadata/**`, `portal/resource_*.ftl`, `login.ftl`, `inc/menu.ftl`, `admin/ui_management.ftl` and `admin/logs.ftl`.

### Layout

Keep the `inc/header`, `menu` and `footer` includes for now. A layout macro is an M7 cleanup.

### Assets

- Vendored static files go under `ipt-app/src/main/resources/static/{js,styles,images}`, so `${baseURL}/js/...` URLs stay the same.
- `tools/assets/manifest.json` records version, URL and sha256 for each library, and a Renovate regex manager keeps them current.
- Summernote is vendored instead of loaded from cdnjs.
- Theme CSS variables come from `ColorSchemeConfig` via `GlobalModel`.

### Minor deviations from IPT3 (documented in the release notes)

- The data dir is configured only via `IPT_DATA_DIR`/`ipt.data-dir`; setup step 1 (choose data dir) and `datadir.location` are removed.
- Logout is a POST (a small form in the menu).
- Former state-changing GET links become POST buttons.
- No WAR or RPM; jar and Docker only.
- M7: Bootstrap 5.0.1 → 5.3.8 (done; `main.css` keeps 5.0's table colours and grey read-only fields); still open: self-hosted Roboto, optionally a layout macro.

## 6. MDT readiness seams (design only, no MDT code)

- **Module boundary.** A future `ipt-mdt` depends only on `ipt-core`, whose no-web rule is enforced. That keeps both options open: porting the Node pipeline to Java behind the MDT React UI, or running MDT as a sidecar whose output is imported.
- **Import API.** Stabilise `ResourceImportService` (already on dwc-dp) with `createFromDwcArchive`, `createFromFrictionlessDataPackage`, `createFromEml` and `replaceData`, all taking `Feedback`.
  - Add a fixture test: a DwC-A with `meta.xml` `default=` values imports as `PropertyMapping.defaultValue`.
  - This covers the MDT meta.xml-vs-per-row defaults interop issue (MDT-UI #161).
- **Jobs.** `JobService` is generic over type, key and `StatusReport`, matching MDT's async job + polling pattern. A later `/api/v1/jobs/{id}` is a thin controller.
- **Authentication.** The `AuthenticationManager` is built from a `List<AuthenticationProvider>`. A GBIF.org account provider (registry-api `user/login` JWT, like MDT) with auto-provisioning can be added without touching controllers. Users stay keyed by email.
- **Reserved paths.**
  - `/api/v1/**` gets its own stateless `SecurityFilterChain` placeholder that returns 404, ready for a token-authenticated REST API and OpenAPI.
  - `/mdt/**` is reserved for a future SPA bundle; no catch-all mappings, and headers that allow an `index.html` fallback.
- **Registry identity.** `RegistrationManager`/`RegistryManager` stay web-independent, so MDT datasets could later register under the IPT's installation instead of a separate MDT installation.

## 7. Milestones

Each milestone exits with the app running on a migrated copy of a real IPT3 data dir, for example a copy of a GBIF test IPT plus one large resource.

### M0: Skeleton
- **Scope:**
  - Multi-module POM; Boot app with `IptFreeMarkerView`, the `s` shim (removed in M7) and `GlobalModel`.
  - Static assets; header, menu and footer; `about.ftl`; `/actuator/health` for the Docker health check. The legacy `/api/health` JSON is ported in M2, pinned by golden files.
  - Logback, the `.do` filter, the Security skeleton.
  - Dockerfile, CI (GitHub Actions; the Jenkins release pipeline follows in M6), Renovate, enforcer/ArchUnit rules, Jackson convergence check.
  - GET-mutator audit; IPT3 golden-capture scripts; `docs/design.md`; `tools/upstream-delta.sh`.
- **Exit:** `mvn verify` is green on JDK 25; `docker run` serves the about page with theme and locale switching.

### M1: Core, persistence and importer
- **Scope:**
  - Move `model/**`, `service/**`, `task/**`, `validation/**`, `utils/**`, `org/gbif/datapackage/**` and the non-web parts of `config/` into ipt-core.
  - The section 2 refactor and section 3 persistence.
  - `IptHttp` (rewriting `RegistryManagerImpl`, the extension/vocabulary/schema downloads and `URLUtils`), Gson removal, `JobService`.
  - All of ipt-legacy-import.
  - Port the core tests (JUnit 6, Mockito 5), with `MockDataDir`/`MockAppConfig` in a test-jar.
- **Exit:**
  - A real IPT3 dir migrates with a clean report and IPT4 boots on it.
  - A headless publish (`GenerateDwca`, `GenerateDataPackage`, `GenerateDarwinCoreDataPackage`) produces output semantically equal to IPT3's.

### M2: Public portal, auth and setup
- **Scope:**
  - `action/portal/*` (Home, Resource detail and preview, ResourceFile, AppFile, DCAT, Inventory v1/v2, Fallback).
  - Login, Account, About, Health, and setup steps 2–5.
  - Templates: `portal/*`, `login`, `account`, `about`, `health`, `admin/setup_*`, `error/*`, `inc/*`.
  - CSRF, locale, error pages, downloads with 304.
- **Exit:** the parity suite is green for public URLs, inventory JSON, rss, dcat, eml/archive/logo/rtf, headers and 304, and the registry-URL contract test passes.

### M3: Manage, without metadata editors
- **Scope:**
  - Controllers ported from `manage/*`: Home, CreateResource, Overview (without registry/DOI), PublicationSettings, AutoPublish, Source, Mapping, DataPackageMapping, ValidationReport, Translation, DataPackageFieldTranslation, VersionHistory, ResourceFile, UrlMetadata, MetadataAgentSuggester.
  - The vocabulary popup.
  - The matching templates.
- **Exit:** create a resource from DwC-A, from EML and from a data package; map; publish DwC-A and DwC-DP; cancel; restore a version. All archives equal IPT3's.

### M4: Metadata editors
- **Scope:**
  - `MetadataAction` (14 EML sections plus logo), `CamtrapMetadataAction`, `DataPackageMetadataAction` (Frictionless, ColDP, DwC-DP).
  - `manage/eml/**`, `dp-metadata/**`, `macros/metadata*.ftl`.
  - Binder customisers and the validators via `ErrorsFeedback`.
- **Exit:**
  - A GET followed by an unchanged POST of every section leaves `eml.xml` byte-identical.
  - Validation messages match IPT3 on the invalid fixtures.
  - A 5000-parameter contacts POST works.

### M5: Admin
- **Scope:** `admin/*` (Config, Users, Extensions, Vocabularies, DataPackageSchemas, Logs, UiManagement, Registration, Organisations, BulkPublication, PublishAll, PublishingStatus and its API, DeleteVersion) and the matching templates.
- **Exit:** the admin checklist passes; installing and updating extensions and schemas from rs.gbif.org works.

### M6: Registry/DOI, hardening, release
- **Scope:**
  - Resource registration and update, organisation change, networks, reserving and deleting DOIs, and publishing with a DOI, tested against GBIF UAT and DataCite test.
  - IPT installation registration.
  - Authz matrix and CSRF review; load test.
  - Antora docs (install, configuration, migration, deviations).
  - Docker release to docker.gbif.org and Docker Hub `gbif/ipt:4`.
- **Exit:** parity checklist signed off; 4.0.0-RC.

### M7 (optional, after parity)
Bootstrap 5.3, self-hosted fonts, a layout macro, replacing `@s.*` with `${t()}` and removing the shim, an `/api/v1` OpenAPI skeleton.

## 8. Verification

- **Core.** Port the ~40 core test classes. Network ITs get `@Tag("network")` and are excluded by default.
- **Migration.**
  - Run every fixture in the legacy repo's `src/test/resources/resources/{res1,amphibians,event}/resource*.xml` (including v1.1, DOI and version-history variants) and `config/registration{,2,_invalid}.xml` through XStream, then v4 JSON, then an IPT4 load, and tree-compare.
  - Re-import is byte-identical, PBE secrets re-encrypt correctly, and IPT4 refuses to start on an IPT3 dir.
- **Web.**
  - `@WebMvcTest` + `MockMvcTester` per controller.
  - An authorisation matrix test enumerates every handler from `RequestMappingHandlerMapping` and calls it as anonymous user, User, Manager-not-on-resource, Manager-on-resource and Admin, with and without CSRF.
- **Templates.**
  - A `@SpringBootTest(RANDOM_PORT)` crawler logs in as admin and follows every link and GET form from `/`, `/manage/` and `/admin/`. It asserts no 4xx/5xx, no FreeMarker errors, and no leftover `${`, `[@s` or `<@s` in the HTML (checked with jsoup).
  - A coverage list fails the build if any `.ftl` is never rendered.
- **Golden files.**
  - Inventory v1/v2, `/api/*`, `/manager-api/*` and `/admin-api/*` are captured from an IPT3 build of the baseline and compared in JSONAssert strict mode with timestamps masked.
  - The i18n old-vs-new comparison covers all keys and locales.
- **Parity harness** (`-Pparity`). Testcontainers runs IPT3 (baseline) and IPT4 (migrated copy of the same dir) and compares a URL spec list:
  - JSON: semantic comparison
  - EML/RSS/DCAT: XMLUnit, ignoring timestamps
  - DwC-A/DP zips: `meta.xml` via XMLUnit, data as sorted line multisets
  - headers and 304 behaviour
  - a scripted publish on both instances
- **Visual.** Playwright screenshots of about 30 key pages at 1280 px with a pixel-diff report. A person reviews it; it does not gate the build.
- **Running locally.**
  - `IPT_DATA_DIR=~/ipt4-data mvn -pl ipt-app -am spring-boot:run -Dspring-boot.run.profiles=dev` (port 7001).
  - Docker: `docker run -p 8080:8080 -v ipt4data:/srv/ipt gbif/ipt:4.0.0-SNAPSHOT`.
  - Migration: `java -jar ipt-app/target/ipt-4.0.0-SNAPSHOT.jar migrate --from <copy of IPT3 dir> --to ~/ipt4-data`.

## 9. Key risks and mitigations

| Risk | Mitigation |
|---|---|
| dwc-dp keeps moving | Frozen SHA. Weekly `upstream-delta.sh` triage. Same package names keep patches applicable; upstream template changes go through `tools/ftl-migrate`. Resource-format changes after the freeze must be mirrored in the frozen legacy model. |
| JSON shape drift on harvested endpoints | Golden files from IPT3 are a merge gate |
| FreeMarker setup differences between Struts and Spring | Explicit config matching Struts, rethrow in tests, full render and crawl coverage |
| Binding semantics (list clearing, auto-grow, empty-to-null, JS index renaming) | Verbatim `clearOwnedLists`, high auto-grow limit, per-section round-trip tests |
| CSRF/POST breaking JS flows and ajax uploads | M0 audit, global ajax header, `?_csrf` on iframe uploads, crawler test |
| Tomcat parameter and part limits | Raised explicitly; 5000-parameter test |
| Jackson 2.15 pin from the frictionless fork | Convergence check; update the fork if needed |
| i18n quirks (apostrophes, double format) | Exact port plus an exhaustive comparison test. Crowdin files and keys stay the same (verify encoding after dropping native2ascii). |
| XStream on Java 25 (importer only) | `PureJavaReflectionProvider`, safe converters, manifest `Add-Opens` |
| Operators losing the WAR and the data-dir chooser | Clear docs, startup error message, Docker-first install guide |

## Critical legacy files (read from `origin/dwc-dp`)

- `src/main/java/org/gbif/ipt/service/manage/impl/{ResourceXStreamFactory,XStreamResourcePersister,ResourcePublicationManagerImpl,ResourceImportServiceImpl}.java`, `service/manage/ResourcePersister.java`
- `src/main/java/org/gbif/ipt/service/admin/impl/{UserAccountManagerImpl,RegistrationManagerImpl,ConfigManagerImpl}.java`
- `src/main/java/org/gbif/ipt/action/{BaseAction,POSTAction}.java`, `action/manage/{ManagerBaseAction,OverviewAction,MetadataAction}.java`
- `src/main/java/org/gbif/ipt/struts2/{SimpleTextProvider,RequireManagerInterceptor,SanitizeHtmlFilter}.java`, `config/{IPTModule,DataDir,AppConfig,PublishingMonitor}.java`
- `src/main/resources/{struts.xml,struts-*.xml,applicationContext.xml}`
- `src/main/webapp/WEB-INF/pages/{macros/forms.ftl,macros/form_field_error.ftl,inc/header.ftl,inc/menu.ftl}`
