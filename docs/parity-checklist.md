# IPT4 parity checklist (4.0.0-RC1)

Each IPT 3 feature area, the tests that cover it on IPT4, and what is left. Intentional differences are in
[deviations.md](deviations.md). Tests run with `mvn -B verify` unless marked otherwise.

| Area | IPT 3 | IPT4 tests | Status |
|---|---|---|---|
| Data directory, migration from IPT 3 | WAR data dir chooser | `DataDirStartupTest`, `MigrateAndPublishTest`, ipt-legacy-import tests (48) | Done |
| Publishing DwC-A, data packages, DwC-DP | Struts actions + core tasks | ipt-core tests (538), `OverviewTest`, `MigrateAndPublishTest`, `AdminPublishingTest` | Done |
| Public portal, downloads, RSS, DCAT, inventory | portal actions | `web/portal/*`, `RegistryContractTest`, `CrawlerTest` | Done |
| Login, account, setup wizard | | `LoginTest` (incl. the account page), `LoginOverHttpTest`, `SetupWizardTest`, `SecurityConfigTest` | Done |
| Manage: create, sources, mappings, translations, settings, versions | manage actions | `ManageResourcesTest`, `SourcesTest`, `MappingTest`, `DataPackageMappingTest`, `ResourceSettingsTest`, `OverviewTest` | Done |
| Metadata editors (EML, Camtrap DP, Frictionless) | | `EmlEditorTest`, `DataPackageMetadataEditorTest`, `LargeFormsTest`, `SanitizeHtmlFilterTest` | Done; ColDP metadata is replaced by upload (no editor, as IPT 3) |
| Admin: configuration, UI, logs, users | admin actions | `AdminConfigTest`, `AdminUsersTest` | Done |
| Admin: extensions, vocabularies, schemas | | `AdminExtensionsTest`; `RegistryExtensionsNetworkTest` (`-Dgroups=network`, passed against rs.gbif.org) | Done; schema updates only checked when the registry publishes a newer compatible version |
| Admin: organisations, IPT registration | | `AdminRegistrationTest` (stub registry) | Done offline; real UAT run pending (below) |
| Bulk publication, status, version deletion | | `AdminPublishingTest` | Done |
| Resource registration, registry updates, networks | overview | `ResourceRegistrationTest` (stub registry) | Done offline; real UAT run pending |
| DOIs (DataCite) | overview, publishing | `ResourceDoiTest` (stub DataCite) | Done offline; DataCite test run pending |
| Access control and CSRF | interceptors | `ApplicationAuthorizationMatrixTest`, `ManageAuthorizationMatrixTest`, `AdminAuthorizationMatrixTest`, `SecurityHeadersTest` | Done |
| Concurrency | | `ConcurrentUseTest`, `ResourceLockOrderTest` | Done |
| Templates and assets | Struts tags | `TemplatesParseTest`, `TextFunctionTest`, `PageVariablesTest`, `CrawlerTest` (all areas, page snapshots), `VendoredAssetsManifestTest`, `tools/ftl-migrate/test.sh` | Done; no Struts tags left |
| Docker image | | `tools/docker-smoke.sh` | Done |

## Waiting for the user

- **GBIF UAT and DataCite test round trip:** `UatRegistrationTest` (`-Dgroups=uat`, credentials in environment
  variables). It registers a test IPT and resource in UAT and reserves, registers and deactivates a DOI on DataCite
  test, so it creates real records.
- **Release:** pushing the tag `v4.0.0-RC1` runs the `release` GitHub workflow, which pushes
  `ghcr.io/mdoering/ipt4` (a final version is also tagged `latest`); no secrets needed. Docker Hub (`gbif/ipt`) is off
  until GBIF approves IPT4: the `Jenkinsfile` push stage is disabled; check its credential id before turning it on.
- **Visual review:** a person compares key pages with IPT 3. Not automated.

## Open before 4.0

- **Files named in uploaded resource folders.** A URL source's local `file` path is taken from an uploaded IPT 3 or
  IPT 4 resource folder as it is, and deleting that source deletes the file, which could be any file the IPT can
  write, such as `config/.secret.key`. The importer must reset it. Managers only, but it reaches the whole data
  directory.
- **DataCite failing part-way** (inherited from IPT 3). Publishing a new major version (the new DOI is registered, then
  updating the replaced DOI fails) or deleting a registered resource with DOIs (deregistered from GBIF, then
  deactivating a DOI fails) leaves the IPT, GBIF and DataCite out of step. Needs compensating steps and stub tests for
  both paths.
- **Concurrent EML edits.** Two people saving different metadata sections of one resource at the same time: the later
  save puts back the earlier section's old values (each page copies the whole EML before taking the lock).
- **Integration tests not run.** The four `*IT` classes in ipt-core are excluded by motherpom's surefire setup and no
  failsafe execution is configured.

The smaller known issues are listed in [decisions.md](decisions.md#known-issues).

## Optional

A layout macro instead of the header, menu and footer includes; an `/api/v1` OpenAPI skeleton; Summernote vendored
instead of loaded from cdnjs; multi-arch images are built, but only the `amd64` one is smoke-tested in CI.
