# IPT 4 decisions and known issues

Why IPT 4 behaves as it does where the port had a choice, each with what it costs if the call was wrong, and the
known issues that are still open. How the code is built is in [design.md](design.md); user-visible differences from
IPT 3 are in [deviations.md](deviations.md); the items that block 4.0 are in [parity-checklist.md](parity-checklist.md).

## Decisions

### Build and repository

- **Spring Boot convention for the main class.** `IptApplication` sits in `org.gbif.ipt` so `@SpringBootTest` classes in
  `org.gbif.ipt.web.*` find it; the app's own beans stay in `org.gbif.ipt.app`. Cost if wrong: none.
- **GeoTools repository.** The tableschema fork needs GeoTools from osgeo.org; the parent pom adds that repository
  with the fork's own repository id, so local mirrors still apply. Cost if wrong: one more remote repository.
- **Build information** sits at `META-INF/build-info.properties` in the jar (Boot 4 layout). Cost if wrong: none.
- **Core services are registered in the app** (`CoreBeans`, an `@Import` list mirroring IPT 3's Guice module) instead
  of carrying `@Service` annotations, so ported classes stay annotation-free and diffable with IPT 3. Cost if wrong: one
  list to keep in sync with new services.
- **`application.properties` renamed `ipt-defaults.properties`** (it clashed with Spring Boot's file); overridable with
  `ipt.defaults[...]`. Cost if wrong: none.
- **Jackson 2 kept for the ported data package code**, Jackson 3 only for IPT 4's own JSON: a verbatim port and a
  smaller diff. Cost if wrong: a later annotation-package migration.
- **Tests never reach gbif.org.** App tests run on `target/test-data-dir` with registry URLs pointing at a dead local
  port; tests that need the network or UAT carry the tags `network` and `uat`, excluded with the `excludedGroups`
  property (run with `-DexcludedGroups= -Dgroups=network`). Cost if wrong: none.
- **Images and releases.** Every green build of `main` and every pushed tag `v<version>` is published to
  `ghcr.io/mdoering/ipt4` for `linux/amd64` and `linux/arm64`; nothing goes to Docker Hub or `docker.gbif.org` while
  IPT 4 is experimental and not approved by GBIF. The `Jenkinsfile`'s credential id (`gbif-dockerhub`) and tool names
  are assumptions to check before its push is turned on. Cost if wrong: the first Jenkins release fails until adjusted.

### Core and persistence

- **`Feedback` keeps BaseAction's method names** (`getText`, `addActionError`, ...) instead of new ones, so call sites
  change only their parameter type and upstream patches keep applying. Cost if wrong: a rename refactor later.
- **English fallback per key.** IPT 3 showed the raw key when a translation lacked it (there is no base bundle).
  Cost if wrong: one extra bundle lookup on misses.
- **Logging control is a hook.** IPT 3's Log4j reconfiguration in `ConfigManagerImpl` became the injectable
  `LoggingControl`, implemented for Logback in the app. Cost if wrong: none known (see the start-up debug issue below).
- **Secrets get a fresh IV on every save**, so `resource.json` changes on each save even without edits. Security over
  byte-stable files. Cost if wrong: noisier diffs of data directories.
- **Model additions for JSON.** `Resource.getAllDataPackageMappings()` and a private `VersionHistory()` constructor
  (XStream bypassed constructors). Cost if wrong: two small additions to carry on upstream merges.
- **Missing extensions as in IPT 3.** A resource mapping an extension that is not installed keeps the row type, and
  `ResourceLoader` then fails that resource with `INVALID_EXTENSION`, exactly as IPT 3 did. Cost if wrong: none.
- **Registry outage at start-up** is a start-up warning, not a failed start (IPT 3 aborted), and installed vocabularies
  are loaded anyway (IPT 3 loaded none and could not publish). Cost if wrong: an IPT may run with stale vocabularies,
  which the warning shows; vocabularies with an unknown URI cannot be uninstalled until the registry is back.
- **Installed schemas before the registry.** `DataPackageSchemaManager.getSchemaIdentifier` looks at installed schemas
  first (IPT 3 asked the registry and failed every resource creation without it). Cost if wrong: an installed older
  schema identifier wins over the registry's latest (identifiers carry no version).
- **`JobService` owned by the publication manager**, as IPT 3 owned its executor, rather than a shared bean. Cost if
  wrong: an MDT integration needs it exposed as a bean (small refactor).
- **No read lock around a publication.** Holding a read lock for a whole publication would block editing for hours;
  files a publication copies are written atomically instead. Cost if wrong: an edit during a publication can still
  race, as in IPT 3.
- **A registry failure during publication fails the publication**, as in IPT 3. Cost if wrong: a registered resource
  cannot publish while the registry is down, as in IPT 3.
- **Bulk publishing answers once all publications finished** (IPT 3's design; the page polls the report). Cost if
  wrong: a long request on large IPTs, as in IPT 3.
- **SMTP password stays plain in `ipt.properties`** (IPT 3's format, kept unchanged; the key would sit in the same
  directory). Never rendered or logged. Cost if wrong: readable by anyone with data directory access, as in IPT 3.
- **Organisation and DOI agency passwords** stay encrypted in `registration.json`, are not shown in the form and are
  kept when posted empty. Cost if wrong: an admin cannot see a stored password.

### Migration

- **No generated legacy model.** The importer reads IPT 3's XML with IPT 3's XStream mapping onto the core model,
  validated by `JsonFixturesTest` against fixtures converted with IPT 3's own code. Far less code. Cost if wrong:
  renames in the core model need XStream aliases in `LegacyXmlReader`; the test flags them.
- **XStream may create only `org.gbif.**` and JDK value types** (IPT 3 allowed any type), because uploaded resource
  folders are read the same way. Cost if wrong: an exotic legacy file using another JDK type fails to import and is
  reported.
- **IPT 3's v1 registration and PBE passwords are handled by `migrate` only**; core has no PBE code. A copied IPT 3
  directory is refused at start. Cost if wrong: none.
- **Old `dwca.zip` names** (before IPT 2.2.1) are renamed by `ResourceLoader` on first start, not by the migrator.
  Cost if wrong: none.
- **Archive equality** with IPT 3 is checked against the expectations of IPT 3's own `GenerateDwcaTest` for the same
  resource and source, because IPT 3 does not build on JDK 25. Cost if wrong: differences outside what that test
  checks go unnoticed.

### Web layer

- **Struts emulation instead of a rewrite.** IPT 3's actions became page beans run by `ActionRunner`, keeping their
  code, binding annotations and result names, so the port stayed close to IPT 3 and upstream changes still apply. Cost
  if wrong: the emulation layer (`ActionRunner`, `@StrutsParameter`, `StrutsConversions`, `StrutsJson`) has to be
  maintained.
- **Binding rules check the objects actually there**, not only declared types: a declared `Source` or `Term` can hide a
  file or an installed extension. Below the top level a request never binds into shared objects (users, organisations,
  extensions, vocabularies, schemas, resources, the IPT, networks, sources). Cost if wrong: a legitimate nested field
  into such a type is refused (shows as a missing value).
- **A GET binds top-level parameters only.** Pages that read ids on GET (`mappingPeek` and others) get them through
  setters, as in IPT 3. Cost if wrong: a top-level setter with side effects on GET (the two in `SourcePage` are
  guarded).
- **Templates resolve model, then page, then request/session/application** (the Struts value stack order; Spring puts
  session attributes first). Cost if wrong: a template relying on a request attribute shadowing a page property (none
  found by the tests or the crawler).
- **Converters as IPT 3 intended.** IPT 3's conversion files for EML dates and coordinates were dead after a package
  move; IPT 4 applies them (year-only dates as year + 1 ms, comma decimals, coordinate ranges). Comma decimals must
  parse completely (`45,5x` is an error). Conversion files for classes the baseline lacks were not ported. Cost if
  wrong: parsing differs from the IPT 3 baseline's Struts defaults.
- **`StrutsConversions` is a plain `ConversionService`**: Spring's `GenericConversionService` caches by type and ignores
  the owning class, which switched rules off app-wide. Checkbox arrays convert to their first value for Boolean
  targets. Cost if wrong: other scalar arrays use Spring's default (joined) conversion.
- **No form objects for admin pages.** IPT 3 already clones the user and organisation in `prepare()`, and the shared
  rule blocks shared objects below the top level. Cost if wrong: a later binding finding on those pages.
- **State changes only by POST.** `resource-*`, publish, cancel, delete source/mapping/version, network and co-manager
  links are POST, sent by the same links through `csrf.js`. Cost if wrong: an old bookmarked GET link answers 405.
- **Result mappings follow IPT 3** where it redirected somewhere odd: registration success to the portal home, logo
  upload to the UI management page; the config save goes back to the configuration page (IPT 3 sent it to the portal
  home). `changePublishingOrganization` (on IPT 3's allow-list, no method) is not mapped. Cost if wrong: one mapping
  each.
- **Form post size 10 MB** (the plan said unlimited): unbounded form bodies are a denial-of-service vector; uploads use
  the multipart limits. Cost if wrong: an enormous form is rejected.
- **`Last-Modified` rounded down** (IPT 3: to the nearest second), needed with ignoring future `If-Modified-Since`.
  Cost if wrong: the header differs by less than a second from IPT 3.
- **Stale ids in manage links.** Index and number errors in a page method become the manage 404 page. Cost if wrong: a
  real bug of that kind also shows as 404.

### Security

- **Relative login redirect** (`Location: /login`, Spring Security 7's default), proxy-safe. Cost if wrong: very old
  clients needing absolute `Location` headers.
- **Session cookie `SameSite=Lax` and `HttpOnly`**; `Secure` by setting, since TLS usually ends at a proxy. Cost if
  wrong: none for same-site use; cross-site links still carry the cookie (Lax allows top-level GETs).
- **Kept as in IPT 3, managers are trusted:** server-side requests to any URL for URL sources and `urlMetadata`
  (managers can probe internal hosts), `validate=false` (managers skip their own form validation), per-user session
  translations. Cost if wrong: as in IPT 3.
- **Plain text with `<` is still refused** (IPT 3's deliberate, tested choice), and entity-encoded markup too. Cost if
  wrong: users avoid `<` in plain fields, as on IPT 3.
- **DocBook fields pass the sanitiser** when they hold only the editors' DocBook elements and web or mail links (IPT 3
  rejected every structured description). Cost if wrong: DocBook-level markup reaches templates (the schema limits it).
- **`?no_esc` in `portal/resource_new.ftl` left as in IPT 3.** New values with markup are refused at input, but data
  imported from older EML could still hold markup. Cost if wrong: stored markup from imported EML renders on the public
  resource page.

### Metadata editors and admin

- **Byte identity of an unchanged save** is measured from the second save: the first normalises an imported EML to
  IPT's EML 2.2 form, as IPT 3 does. Cost if wrong: none.
- **ColDP metadata has no editor** (none in IPT 3, whose page threw); IPT 4 answers 404 and keeps the Frictionless
  editor. Cost if wrong: a ColDP editor would be new scope.
- **Camtrap DP contributor roles** are stored as the vocabulary value, not IPT 3's key (IPT 3 lost roles after one
  save). Cost if wrong: one converter rule.
- **Validation messages** come from the ported `EmlValidator` and bundle; there was no separate comparison run with
  IPT 3. Cost if wrong: message drift unnoticed.
- **Schema updates** are checked against the registry only when it publishes a newer compatible version (IPT 3 offers
  no schema install in the UI). Cost if wrong: a schema update regression unnoticed until a new version is registered.
- **The organisations list** shows only once the IPT is registered (IPT 3's template). Cost if wrong: none.

### Testing

- **Fixtures.** The installed-configuration fixture (2.7 MB of public GBIF files from the production registry) is
  committed, so offline tests have real extensions and schemas. The DwC-DP fixture uses schema `1.0_DEV` from UAT: the
  bundled validator validates against it, so a `0.1` package could never publish (the IPT 3 baseline behaves the
  same). Cost if wrong: repository size; a schema version the baseline does not install by default.
- **Feature tests instead of one long end-to-end test** (create from DwC-A, EML and data packages, sources, mapping,
  publishing DwC-A and DwC-DP, cancel, history); archive equality relies on `MigrateAndPublishTest`. Cost if wrong: a
  difference in published archives that only the web path causes.
- **Allowed dead links** in the crawler, as users have them in IPT 3: downloads of never-published data packages
  (`cam1`, `col1`) and `/admin/None` (a registry property with `dc:relation='None'`). Cost if wrong: none.
- **DataCite is stubbed** from the gbif-doi client's paths, statefully (draft, registered, findable). Cost if wrong: a
  real response difference shows only in the `uat` test.
- **The `uat` test is written but has not been run**: it creates records in GBIF UAT and DataCite test. Cost if wrong:
  an integration difference stays unnoticed until it is run.

## Known issues

Checked against the code on 2026-10-13. The ones that block 4.0 are in [parity-checklist.md](parity-checklist.md):
resource folders naming any file for a URL source, DataCite failing part-way, concurrent EML section saves, and the
integration tests that never run. Marked *(IPT 3)* where IPT 3 behaves the same.

### Data and concurrency

- The metadata working copy is put on the shared resource while it is validated; readers take no lock, so they can
  see an invalid section briefly.
- Publication completion race: `ResourcePublicationManagerImpl` checks, reads and removes a finished job in three
  steps, so two callers can both finish one publication (`publishEnd` and DOI work twice). The admin's bulk
  publication page clears all reports on GET, which can make a finishing failed publication throw. *(IPT 3)*
- The last-admin check (demotion or deletion) is not atomic.
- `RegistryManagerImpl` shares a SAX parser, its handler and a feedback object between threads. *(IPT 3)*
- Long-lived `CollectingFeedback` objects in the extension, vocabulary, schema and registry managers and the
  publishing monitor grow without bound and are shared between threads.
- A resource's lock is not removed when the resource is deleted.
- `AtomicFiles` creates owner-only (0600) files, so JSON files in the data directory become owner-only; stray `.tmp`
  files outside `tmp/` are not cleaned up.
- `RegistrationManagerImpl` restores the network association on load (IPT 3 dropped it); not listed as a deviation.

### Input and errors

- An unknown `rdbms` value for an SQL source throws (500). *(IPT 3)*
- A page result mapped to a view for `error` answers 500 (publish error, late cancel).
- The source form binds onto the live source before validation, so a rejected rename stays until the next save.
  *(IPT 3)*
- URL sources and `urlMetadata` answer 500 on `file:`, `ftp:` and malformed URLs and echo the exception text.
- A refused resource logo shows no message (the upload iframe consumes it).
- `"on"` and `"1"` convert to `false` for Boolean fields (`Boolean.valueOf`).
- A POST to a `datapackage-metadata-*` section on a Camtrap DP resource throws (500) instead of 404.
- The UI management page saves colours without validation; a crafted non-hex colour breaks every page until fixed in
  the data directory.
- Admin-only crafted posts: `validate=false` skips user validation; an organisation post whose id and key differ takes
  one organisation's password for the other; deleting a schema without a name leaves its files.
- Adding or removing a network swallows registry errors. *(IPT 3)*
- Deleting a DOI goes on after a missing-account error; a DOI agency misconfiguration during resource deletion is a
  500. *(IPT 3)*
- Junk `id` or `mid` values still throw on some manage pages (the authorization matrices allow it).
- The first stored upload is not deleted if storing a second one fails.
- An over-limit AJAX upload answers 403 (CSRF) instead of a size error.
- `migrate` shows a stack trace when `users.xml` or `registration.xml` cannot be read.
- `DataDirFormat` edge cases: a regular file counts as an empty directory, `lost+found` makes a directory unknown, an
  interrupted setup is not detected when the key comes from `IPT_SECRET_KEY`.
- A DwC-A that contains a `resource.xml` is treated as a resource folder; `saveInferredMetadata` does not catch
  Jackson's runtime exceptions; `UserStore` holds a cipher with an all-zero key (unused for users).

### Security hardening

- Every page view creates a session (the CSRF token is stored in it).
- `csrf.js` `postLinkForm` does not check that the link is same-origin (links come from templates).
- The XStream allow-list could be narrower.
- `IptHttp`: per-request proxy clients are not closed; HTTP/2 upgrade is attempted on `http://`;
  `IptHttp.Credentials.toString()` includes the password. `ExtensionFactory` decodes responses as UTF-8 regardless of
  their charset.

### Operations

- The debug setting from `ipt.properties` is not applied at start-up (only when changed).
- Logs roll by size only; IPT 3 also rolled at start-up and set parsers and `TermFactory` to ERROR.
- `.dockerignore` does not exclude `ipt-data/` and `logs/`.

### Tests and tooling

- The authorization matrices do not cover a manager on their own resource, the manage area without CSRF, `/login`,
  `/logout`, the actuator, or logout without CSRF.
- No test for: a Frictionless editor round trip, metadata saves while publishing or concurrent section saves, a
  rejected organisation edit changing nothing. `ConcurrentUseTest` uses MockMvc without a start barrier and no
  registry or DOI path. The log capture for secrets uses the default level only.
- `UatRegistrationTest`: cleanup not in `finally`, the UAT installation is not removed, the registry type not checked.
- `CoreBoundaryTest` allows an empty rule (could pass vacuously).
- `tools/ftl-migrate` leaves `<@s.form>` for manual migration (reported); `tools/assets/manifest.json` has no URLs or
  checksums; `docs/audits/get-mutators.md` has false positives and misses `$.get`/`$.getJSON`.
- Release workflow: actions not pinned to commits; `node --test` uses the runner's Node.
