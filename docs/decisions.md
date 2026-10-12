# IPT4 implementation decisions

The calls made while implementing the milestones without the user present (rulings, each with what it costs if
wrong) and the review findings deferred as minor. Collected from the milestone ledgers on 2026-10-11; M2's were
reported at the time. The code and `docs/deviations.md` are the current state; this is the record of why.

## M0 skeleton

### Rulings

- Setup: repo did not exist; git init -b main, plan committed on main (e68d396), all work on branch m0-skeleton — gives a merge-base without implementing on main — cost if wrong: rebase onto a different root.
- Setup: commit messages carry no Claude-Session/attribution line — user's global CLAUDE.md forbids any Claude attribution — cost if wrong: none.
- Task 2: plan's BundleMessages only falls back to English when no bundle exists for the locale; with no base bundle a translation missing a key returned the raw key (IPT 3 does the same) — added a per-key English lookup to satisfy the spec/Review Focus #4; listed for docs/deviations.md in Task 16 — cost if wrong: one extra bundle lookup on misses.
- Task 4: build-info.properties sits at META-INF/ in the jar root (Boot 4 layout), not BOOT-INF/classes/META-INF as the plan's check said; content has build.version=4.0.0-SNAPSHOT — plan path was wrong, no code change — cost if wrong: none.
- Task 6: @SpringBootTest in org.gbif.ipt.web.* could not find org.gbif.ipt.app.IptApplication (search goes upwards only) — moved IptApplication to org.gbif.ipt (Boot convention), mainClass updated; IptProperties/CoreBeans stay in .app — cost if wrong: one class move.
- Task 7: plan's FriendlyMapModel extended SimpleMapModel, which breaks map.get(key) (used by ~15 IPT 3 templates, caught by the probe test); decompiled struts2-core 7.3.0 shows it extends MapModel and overrides isEmpty/keySet/values — ported that exactly — cost if wrong: none, matches Struts.
- Task 8: Spring Security 7's LoginUrlAuthenticationEntryPoint redirects to the relative '/login', not 'http://localhost/login' as the plan expected — kept the relative redirect (proxy-safe), changed assertions, added loginRedirectKeepsContextPath (/ipt/admin/ → /ipt/login) — cost if wrong: very old clients needing absolute Location headers.
- Task 14: clean-repo Docker build failed resolving GeoTools (tableschema fork, test scope) — added the osgeo.org release repo to the parent pom with the fork's own repo id so local mirrors still apply — cost if wrong: one more remote repo in resolution.
- Task 14: plan's docker-smoke.sh used `curl | grep -q` under pipefail → curl exit 23 when grep closes early — replaced with `grep … > /dev/null` — cost if wrong: none.
- Task 16: added deviations.md rows for the Task 2 per-key English fallback and the Task 8 relative login redirect — both are user-visible differences — cost if wrong: two doc rows.
- re-graded Minor "README spring-boot:run fails" to Important — it is the documented dev run path — reproduced the failure, fixed, verified the new command serves /about — cost if wrong: none.
- re-graded Minor "IDE metadata committed" to Important — my own mistake shipping Eclipse config to every clone — untracked + .gitignore — cost if wrong: none.
- reviewer's "Declined to judge" items (Struts 7.3 FreeMarker config, IPT 3 empty-locale behaviour, api.gbif.org CORS, /api/v1 404, later-milestone security items, upstream absolute paths, streaming truncation, SNAPSHOT dependency) — all stand as set aside: either verified by my decompilation, judged by the plan's wording, or scheduled for later milestones per spec — cost if wrong: revisit in M2/M3.

### Deferred minor findings

- template lookup order — request/session attributes win over the page bean (fix before M3 ports session keys)
- every anonymous page view creates an HTTP session (CSRF token); consider CookieCsrfTokenRepository
- ftl-migrate ignores <@s.form method="post"> (publishAll form)
- manifest.json lacks per-library URL + sha256 required by spec §5
- get-mutators.md has false positives and misses $.get/$.getJSON
- CoreBoundaryTest allowEmptyShould(true) can pass vacuously
- logging lacks IPT 3 roll-on-startup and ERROR overrides for parsers/TermFactory
- .dockerignore misses ipt-data/, logs/, .superpowers/; document container UID for bind-mounted data dirs
- no test that markup in s.param bodies is escaped

## M1 core, persistence, importer

### Rulings

- Setup: work directly on main with local commits — user instruction 2026-10-10 — cost if wrong: none (local only)
- Setup: plan executed without a separate plan-review stop — user asked to keep working through all milestones — cost if wrong: rework of a task the user would have shaped differently
- Task 1: Feedback keeps BaseAction's method names (getText/addActionError/...) instead of the design's info/warn/error — call sites change only their parameter type, so upstream patches keep applying — cost if wrong: a rename refactor later
- Task 1: Jackson 2 stays for the ported data-package metadata/YAML code (Jackson 2 databind annotations); Jackson 3 only for new data-dir JSON — verbatim port, smaller diff — cost if wrong: a later annotation-package migration
- Task 1: Struts @Element list-type hints replaced by comments; Spring binding of those polymorphic lists is an M4 concern — cost if wrong: M4 binder work
- Task 1: ConfigManagerImpl's Log4j reconfiguration became an injectable LoggingControl hook (app implements it in Task 4) — cost if wrong: debug toggle no-op until then
- Task 1: committed as a verbatim import (8dc8e17, does not compile alone) followed by the edits (9c3274b) so the IPT 3 diff is reviewable — cost if wrong: one non-compiling commit in history
- Task 2: manager tests keep mocking IptHttp with Mockito (as IPT 3 mocked HttpClient) instead of a local HTTP server; IptHttp itself is tested against a real JDK HttpServer — cost if wrong: HTTP-level regressions in managers caught later by parity tests
- Task 2: FieldJson is Jackson 3 with field visibility and Gson-like leniency (unquoted names etc.) — the legacy organisation fixture uses unquoted names — cost if wrong: none known
- Task 3: plan's "missingExtensionIsReportedNotFatal" corrected to IPT 3 behaviour — a missing extension becomes null + extensionVerbatim, and ResourceLoader then fails that resource with INVALID_EXTENSION exactly as IPT 3 did (test missingReferencesBecomeNullAsInIpt3) — cost if wrong: none, matches legacy
- Task 3: secrets get a fresh random IV on every save, so resource.json changes on each save even without edits — security over byte-stable files — cost if wrong: noisier diffs of data dirs
- Task 3: added Resource.getAllDataPackageMappings() and a private VersionHistory() constructor — JSON needs them (XStream bypassed constructors) — cost if wrong: two small model additions to carry on upstream merges
- Task 3: legacy fixtures converted once through IPT 3's own XStream setup (LegacyXmlFixturesTest, in history at 66f0c70) with a deep object comparison; IPT 3 resource-folder zips kept as *-ipt3.zip — cost if wrong: none; Task 6 re-checks via the importer
- Task 3: IPT 3 v1 registration upgrade (encryptRegistration) and legacy PBE user passwords removed from core; ipt migrate handles them — cost if wrong: an IPT 3 dir copied without migrate would fail to start (DataDirFormat refuses it in Task 4)
- Task 4: core services are registered with @Import in ipt-app (a Java mirror of applicationContext.xml) instead of @Service annotations in core — keeps ported classes annotation-free and upstream-diffable — cost if wrong: one list to keep in sync with new services
- Task 4: IPT 3's classpath application.properties renamed ipt-defaults.properties (clashed with Spring Boot's config file) with ipt.defaults.* overrides — cost if wrong: none
- Task 4: registry outage during start-up becomes a ConfigWarnings start-up error instead of aborting start (IPT 3 aborted) — deviation recorded — cost if wrong: an IPT may run with stale default vocabularies, which the warning shows
- Task 4: app tests run on target/test-data-dir with registry URLs pointed at a dead local port (src/test/resources/config/application.yml) so no test reaches gbif.org — cost if wrong: none
- Task 5: ResourcePublicationManagerImpl owns its JobService (as IPT 3 owned its executor) rather than a shared JobService bean — no constructor change, tests untouched — cost if wrong: MDT jobs later need the service exposed as a bean (small refactor)
- Task 5: publication "snapshot under read lock" deferred to M3 (publishing UI); M1 gives ResourceLocks and takes the write lock on save — holding a read lock for a whole publication would block editing for hours — cost if wrong: an edit during publication can still race, as in IPT 3
- Task 6: no generated frozen IPT 3 model; the importer reads IPT 3 XML with IPT 3's XStream mapping onto the core model (validated by JsonFixturesTest against the fixtures converted with IPT 3's own converters) — far less code — cost if wrong: future core-model renames need XStream aliases in LegacyXmlReader; JsonFixturesTest will flag them
- Task 6: XStream permissions restricted to org.gbif.** and JDK value types (IPT 3 allowed any type) because uploaded resource-folder zips are read too — cost if wrong: an exotic legacy file using another JDK type fails to import and is reported
- Task 6: resource_nonexistent_ext.json fixture now keeps the unknown extension's row type, as the importer does (IPT 3's converter turned it into null) — cost if wrong: none; the resource still fails to load with INVALID_EXTENSION
- Task 6: pre-2.2.1 dwca.zip renaming left to ResourceLoader on first IPT4 start (it already does it) instead of the migrator — cost if wrong: none
- Task 7: "equal to IPT3's output" is checked against IPT 3's own GenerateDwcaTest expectations for the same resource and source, not against archives produced by building IPT 3 (IPT 3 does not build on JDK 25: Error Prone 2.15) — cost if wrong: differences outside what IPT 3's test checks go unnoticed until the M2+ parity harness
- Task 7: headless GenerateDataPackage/GenerateDarwinCoreDataPackage end-to-end deferred to M3 (no IPT 3 tests or fixtures for data-package publishing exist to compare against) — cost if wrong: data-package publication regressions found later
- Task 7: installed vocabularies load when the registry is unreachable (IPT 3 loaded none and could not publish) — deviation recorded — cost if wrong: vocabularies with unknown resolvable URI cannot be uninstalled until the registry is back
- I3 data-package smoke publish stays deferred to M3 (DP resources are created through the manage UI there; ResourceJsonTest covers DP mapping relinking) — cost if wrong: DP JSON relinking bugs found in M3
- dangling-reference parity, --link-artifacts with running IPT 3, processFailures thread safety, single scheduler thread, @Element, HTTP timeouts, background English texts, upload limits — all stand as IPT 3 parity or later-milestone scope — cost if wrong: revisit in M3/M6

### Deferred minor findings

- debug setting from ipt.properties not applied at start-up (LoggingControl setter injected after constructor load)
- long-lived CollectingFeedback(null) singletons grow and are shared across threads; use LoggingFeedback
- ResourceLocks adds nothing over synchronized save yet; read-lock upgrade/deadlock trap for M3; locks not removed on delete
- AtomicFiles temp files are 0600 so data dir JSON becomes owner-only; stale .tmp files not cleaned
- RegistrationManagerImpl now restores the network association on load (IPT 3 dropped it) — undocumented
- ledger lacks rulings for: app XStream ban via ArchUnit only, IptProperties missing upload/publishing/registry settings, JobHandle without StatusReport, locks in ResourceManagerImpl, IptHttp API shape
- UrlSource.file from uploaded folders trusted (could point at config/.secret.key) — reset on import
- XStream allow-list could be narrowed further
- MigrateCommand lets XStream RuntimeExceptions escape as stack traces
- IptHttp per-request clients not closed; h2c upgrade on http://; ExtensionFactory decodes body as UTF-8
- DataDirFormat edge cases (crash between setup and marker → IPT3 message; regular file → EMPTY; lost+found → UNKNOWN; INCOMPLETE undetected with IPT_SECRET_KEY)
- UserStore mapper with zero-key cipher; saveInferredMetadata doesn't catch Jackson runtime exceptions; DwC-A containing resource.xml treated as resource folder

## M3 manage

### Rulings

- Setup: work on main, local commits; user said 'do M3 and all following milestones without my intervention' — plan review stop skipped — cost if wrong: none
- Task 2: managerCannotTouchOthersResources over all manage URLs moves to Task 9 (authorization matrix); Task 2 tests ManageGuard through a probe page — the URLs do not exist before Tasks 3-8 — cost if wrong: none
- Task 2: shared-object binding rule by declared types (User, Organisation, Extension(Property), Vocabulary(Concept), DataPackage(Schema/TableSchema/Field), Resource, Ipt, Network) below the page's top-level properties, instead of per-page allow lists — covers IPT 3's depth-n getters generically — cost if wrong: a legit nested bind into such a type is refused (shows as a missing value)
- Task 2: max-http-form-post-size 50MB (design said -1) — unbounded form bodies are a DoS vector; uploads use multipart limits — cost if wrong: an enormous form is rejected
- Task 2: Last-Modified rounded down (IPT 3 nearest) — needed with the M2 future-IMS rule, a test raced on it — cost if wrong: header differs by <1s from IPT 3
- Task 3: locked/report/cancel move to Task 4 — they are OverviewPage methods and need publishing to test — cost if wrong: none
- Task 3: DataPackageSchemaManager.getSchemaIdentifier uses installed schemas first, registry second (null on failure) — IPT 3 failed every creation without registry — cost if wrong: an installed older schema identifier wins over the registry's latest (identifiers are version-less)
- Task 3: installed-config fixture captured from production registry 2026-10-11 and committed (2.7 MB, public GBIF files) — offline tests need real schemas — cost if wrong: repo size
- Task 4: registry/DOI resource-* actions (registerResource, addNetwork, deleteNetwork, reserveDoi, deleteDoi) are mapped now, tested in M6 — cost if wrong: untested buttons until M6
- Task 4: resource-* only as POST (IPT 3 any method) — design: no state changes on GET — cost if wrong: an old GET link 405s
- Task 4: changePublishingOrganization (on IPT 3's allow-list, no method in baseline) not mapped — cost if wrong: none
- Task 4: urlMetadata (manager-triggered server-side HEAD of any URL) kept as in IPT 3 — managers may add URL sources anyway — cost if wrong: SSRF by managers to internal hosts
- Task 5: URL sources not tested end-to-end (need a local HTTP server); add path covered by M3 crawler/E2E later if time — cost if wrong: URL source regressions undetected
- Task 6: Source added to never-bound-through types (mapping.source.*) — a mapping shares the resource's Source objects — cost if wrong: none (forms never post it)
- Task 7: fixture DwC-DP schema 1.0_DEV from UAT, not the baseline's supported 0.1 — the bundled dwc-dp validator validates against 1.0_DEV, so a 0.1 package can never publish (IPT 3 baseline behaves the same) — cost if wrong: test covers a schema version the baseline does not install by default
- Task 7: PageContext holds the RequestFeedback target, not the scoped proxy — jobs call the page's feedback off-thread — cost if wrong: job messages written to a finished request's feedback are lost (as in IPT 3)
- Task 9: E2E covered by per-feature tests (create DwC-A/EML/DP, sources, mapping, publish DwC-A + DwC-DP, cancel, history) rather than one long test; archive equality vs IPT 3 relies on M1's MigrateAndPublishTest (same core generators) — cost if wrong: a web-path-only difference in published archives
- Task 9: junk id/mid from authorized managers still 500 on some IPT 3 pages (NPE in prepare); IndexOutOfBounds/NumberFormat → 404, missing INPUT view → overview — cost if wrong: error page for stale links on the remaining pages
- GET binds top-level setters only (reviewer suggested none) — mappingPeek and other GET pages read mid etc. via setters as in IPT 3 — cost if wrong: a top-level setter with side effects on GET (SourcePage's two guarded)
- SSRF via URL sources/urlMetadata kept (IPT 3 parity, managers trusted) — cost if wrong: managers can probe internal hosts
- validate=false bindable by managers (IPT 3 parity) — cost if wrong: managers skip their own form validation
- race between lock check and starting publication, unsynchronised live Resource mutation (IPT 3 parity; M4 working copy covers metadata) — cost if wrong: rare concurrent-edit inconsistencies
- publication settings may change organisation of registered/DOI resources (verbatim port; M6 territory) — cost if wrong: registry mismatch until M6
- session-wide DwC translation per user, fields[99999] auto-grow self-harm — IPT 3 parity, self only — cost if wrong: none for others

### Deferred minor findings

- unknown rdbms value NPEs → 500 (IPT 3 parity)
- ERROR mapped to a page still answers 500 (publish error, late cancel)
- live objects bound before validation (rejected source.name renames until next save) — IPT 3 parity
- IndexOutOfBounds/NumberFormat anywhere in a page method become 404 (e.g. AutoPublish parseInt)
- csrf.js postLinkForm has no same-origin check (hrefs are template-relative)
- first stored upload not deleted if storing a second fails
- over-limit XHR upload answers 403 CSRF instead of a size error; send X-CSRF-TOKEN header
- urlMetadata/URL source 500 on file:/ftp:/malformed URLs and echo exception text
- Final: minor (deferred, from Task 5/9): unknown rdbms NPE 500; junk id/mid NPE 500 on some pages

## M4 metadata editors

### Rulings

- Setup: started Task 1 while the M3 final review runs (independent area); M3 fix pass comes first after the review — cost if wrong: merge friction in ActionRunner
- Task 2: apply the intended EML converters (pubDate, temporal dates, Point lat/long) although IPT 3's conversion files for them were dead (wrong package after the metadata library move) — the converters encode IPT's date protocol (year-only + 1 ms) that templates and EmlFactory rely on — cost if wrong: date/number parsing differs from IPT 3 baseline's Struts defaults
- Task 2: conversion files for classes the baseline lacks (Contributor, DataPackageLicense, DataPackageSource) not ported — they never applied — cost if wrong: none
- Task 2: comma-decimal coordinates must parse completely (IPT 3's DecimalFormat accepted trailing junk) — cost if wrong: '45,5x' is now an error
- Task 3: byte-identity measured from the second unchanged save — the first save normalises an imported EML to IPT's EML 2.2 form, as IPT 3 does — cost if wrong: none
- Task 3: XSSUtils keeps flagging plain text with '<' (IPT 3's deliberate, tested choice); the test fixture's '&lt;50' reworded — cost if wrong: users must avoid '<' in plain fields, as on IPT 3
- Task 3: validation messages = ported EmlValidator + bundle (no separate IPT 3 comparison run); lockedDuringPublication covered by ManageGuard (all manage pages) — cost if wrong: message drift undetected
- Task 3: EML DocBook fields pass the sanitising filter unchanged (schema-validated), links must be http(s)/mailto/ftp — IPT 3 rejected every structured description — cost if wrong: DocBook-level markup reaches templates (schema limits it)
- Task 5: ColDP metadata has no editor in IPT 3 (overview is view-only; its datapackage-metadata-* page threw ClassCastException) — IPT4 answers 404 for ColDP and keeps the Frictionless editor for FrictionlessMetadata — cost if wrong: a ColDP editor would be new scope.
- Task 5: Camtrap contributor role stored as vocabulary value instead of IPT 3's key — IPT 3 lost roles after one save — cost if wrong: one converter rule.
- Task 5: StrutsConversions no longer a GenericConversionService — its TypeDescriptor cache ignores the owning class and disabled rules app-wide after a lookalike property — cost if wrong: none (behaviour per property as intended).
- Task 5: checkbox array ["true","false"] → first value for Boolean targets (Struts) — forms.ftl sends both — cost if wrong: other scalar arrays still use Spring's default (joined) conversion.
- Task 6: crawler accepts 404 for archive.do/metadata.do of the never-published cam1/col1 fixtures — IPT 3's data package previews link to downloads before the first publication (parity) — cost if wrong: a dead link users already have in IPT 3.
- ?no_esc sinks in portal/resource_new.ftl left as IPT 3 — new values with markup are now refused at input; existing imported data could still hold markup — cost if wrong: stored markup from imported EML renders on the public page.

### Deferred minor findings

- lost update when two people save different EML sections concurrently (clone in prepare without lock).
- working copy visible to lock-free readers while validated.
- refused resource logo gives no message (iframe consumes the flash).
- String[]→Boolean uses Boolean.valueOf ("on"/"1" → false, empty → false).
- POST datapackage-metadata-*.do on a Camtrap resource → NPE 500 (should be 404).
- test gaps — Frictionless editor round trip, metadata lockedDuringPublication / concurrent save.

## M5 admin

### Rulings

- Task 1: no form-object mechanism — IPT 3 already clones the user/organisation in prepare() and the SHARED rule only blocks shared objects below the top level — cost if wrong: a later binding finding on those pages.
- Task 1: bulk_publication.ftl <@s.form> replaced by a plain form + csrfField instead of a shim directive — the only s.form use — cost if wrong: none.
- Task 2: config save redirects to /admin/config.do (IPT 3: /config.do, answered by the portal home) — cost if wrong: one result mapping.
- Task 2: SMTP password stays plain in ipt.properties (design keeps ipt.properties unchanged; key would sit in the same data dir) — never rendered; empty keeps stored — cost if wrong: encrypting later needs a format step.
- Task 2: appLogoUpload answers with a redirect to uiManagement (IPT 3 results pointed into manage; its JS ignores the answer) — cost if wrong: none.
- Task 3: last-manager deletion tested through the creator rule (IPT 3 fixture: res1 created by an admin, who counts as manager) — same check path — cost if wrong: LAST_RESOURCE_MANAGER message untested.
- Task 4: network test updates an older installed core (seeded from rs.gbif.org) instead of installing an older version over a newer one — the IPT keeps the newer one, and seeding is the real upgrade situation — cost if wrong: none.
- Task 4: schema install/update not exercised offline (IPT 3 offers no schema install in the UI; the registry has no newer compatible schema); list/detail/404 offline and a real-registry list check — cost if wrong: a schema update regression goes unnoticed until a new schema version is registered.
- Task 4: tests excluded by tag via the excludedGroups property (network); run with -DexcludedGroups= -Dgroups=network — cost if wrong: none.
- Task 5: registration success keeps IPT 3's redirect to the portal home (/home.do) — parity — cost if wrong: one result mapping.
- Task 5: the organisations list shows only once the IPT is registered (IPT 3 template) — parity — cost if wrong: none.
- Task 5: organisation and DOI agency passwords stay encrypted in registration.json as M1 designed; not rendered in the form, kept when posted empty — cost if wrong: an admin cannot see a stored password (they could not meaningfully before either).
- Task 6: bulk publishing still answers only once all publications finished (IPT 3 design; the page polls bulkReport) — cost if wrong: a long request for large IPTs, as in IPT 3.
- Task 6: bulk publication tests use a resource with complete metadata (IPT 3's fixture res1 is refused by the same validator, asserted) — cost if wrong: none.
- Task 7: templates resolve model > page > request/session/application (Struts value stack order; Spring's AllHttpScopesHashModel put session attributes before the page) — cost if wrong: a template relying on a request attribute shadowing a page property (none found by the suite or crawler).
- Task 7: crawler accepts 404 for /admin/None (registry data: a property with dc:relation='None', linked by IPT 3's extension page) — cost if wrong: one dead link users have in IPT 3.
- SMTP password stays plain in ipt.properties (Task 2 ruling stands; reviewer asked for confirmation) — IPT 3 format, never rendered or logged — cost if wrong: readable by anyone with data-dir access, like IPT 3.

### Deferred minor findings

- UI management binds the live colour scheme without validation; a crafted non-hex colour breaks every page until fixed on disk.
- GET /admin/bulk-publication clears all publication reports; a report cleared before the monitor completes a failed publication can NPE in isLocked (IPT 3 parity).
- isLocked completion race (containsKey/get/remove) — two callers may finish one publication; the bulk wait loop adds a caller.
- admin-only crafted posts — validate=false skips user validation; organisation id/key mismatch takes A's password for B; schema delete without schemaName leaves files.
- last-admin check not atomic (two concurrent demotions).
- test gap — rejectedOrganisationFormChangesNothing for edits.

## M6 registry/DOI, hardening, release

### Rulings

- Task 1: a registry failure during publication fails the publication (IPT 3 publishEnd throws; plan text corrected) — parity — cost if wrong: a registered resource cannot publish while the registry is down, as in IPT 3.
- Task 1: tests publish res1 through ResourcePublicationManager (IPT 3's fixture has incomplete metadata for the publish button) and give it a CC-BY licence (registration needs a GBIF licence) — cost if wrong: none.
- Task 2: DataCite stubbed statefully (draft/registered/findable via events, Basic auth) from the gbif-doi client's paths (/dois, /dois/{id}) — cost if wrong: a real DataCite response shape difference shows only in the uat test.
- Task 3: the uat test is written and compiled but not run — it creates records in GBIF UAT and DataCite test (user's instruction: no outward-facing actions) — cost if wrong: an integration difference stays unnoticed until the user runs it.
- Task 4: session cookie SameSite=Lax and HttpOnly set explicitly (Secure follows HTTPS at the proxy/Tomcat) — cost if wrong: none for same-site use; cross-site links still carry the cookie (Lax allows top-level GETs).
- Task 5: docs say what exists (size-based log rolling; no data-dir upgrader yet, only the version marker refusing newer formats) — cost if wrong: none.
- Task 6: release pipeline prepared but not triggered; Jenkins credential id (gbif-dockerhub; since then docker.gbif.org dropped and the Docker Hub push disabled — releases go to ghcr.io/mdoering/ipt4 until GBIF approves IPT4) and tool names are assumptions to check — cost if wrong: the first Jenkins release fails until adjusted.
- I-1 DataCite failing part-way (new major version, deleting a registered resource with DOIs) not fixed — inherited from IPT 3, needs compensating steps designed with the user; listed as open in docs/parity-checklist.md — cost if wrong: IPT, GBIF and DataCite out of step after a DataCite outage during those two operations, as in IPT 3.

### Deferred minor findings

- isLocked completion race (duplicate publishEnd / NPE) — now with external effects.
- network add/remove registry errors swallowed (IPT 3 parity).
- deleteDoi continues after no-account error; InvalidConfigException from getDoiService in delete → 500 (IPT 3 parity).
- RegistryManagerImpl shared SAXParser/handler and singleton feedback not thread-safe (IPT 3 parity).
- ConcurrentUseTest uses MockMvc, no start barrier, no registry/DOI path.
- ApplicationAuthorizationMatrixTest only anonymous/admin; filter endpoints (/login, /logout) and actuator not enumerated; logout without CSRF not asserted.
- release workflow — actions not SHA-pinned, no setup-node. (Since fixed: started by a pushed tag v<version>, version
  validated by tools/set-version.sh, permissions block.)
- Jenkinsfile — (since fixed: version validated and passed via env, latest on both registries for final versions,
  decided by the version as on GitHub, via tools/push-image.sh.)
- UatRegistrationTest cleanup not in finally, UAT IPT installation not removed, registry type not asserted.
- docs — ipt.base-url takes precedence over the admin setting for page links only; jar install has no published artifact; Docker migration needs the IPT 3 dir readable by the container user.
- IptHttp.Credentials record toString includes the password.
- secretsAreNotLogged captures console at default level only.

