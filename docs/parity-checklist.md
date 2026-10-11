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
| Templates and assets | Struts tags | `TemplatesParseTest`, `TextFunctionTest`, `PageVariablesTest`, `CrawlerTest` (all areas, page snapshots), `VendoredAssetsManifestTest`, `tools/ftl-migrate/test.sh` | Done; Struts tag shim removed (M7) |
| Docker image | | `tools/docker-smoke.sh` | Done |

## Waiting for the user

- **GBIF UAT and DataCite test round trip:** `UatRegistrationTest` (`-Dgroups=uat`, credentials in environment
  variables). It registers a test IPT and resource in UAT and reserves, registers and deactivates a DOI on DataCite
  test, so it creates real records.
- **Release:** the `release` GitHub workflow (or the `Jenkinsfile`) with version `4.0.0-RC1` pushes
  `docker.gbif.org/ipt` and `gbif/ipt`. Check the Jenkins credential ids first.
- **Visual review:** a person compares key pages with IPT 3 (design §8). Not automated.

## Open before 4.0 (inherited from IPT 3)

- **DataCite failing part-way.** Publishing a new major version (the new DOI is registered, then updating the
  replaced DOI fails) or deleting a registered resource with DOIs (deregistered from GBIF, then deactivating a DOI
  fails) leaves the IPT, GBIF and DataCite out of step, as in IPT 3. Needs compensating steps and stub tests for both
  paths.

## Left for M7 (optional)

Done in M7: Bootstrap 5.3.8 and the removal of the `@s.*` tag shim. Still optional: self-hosted fonts, a layout macro, an `/api/v1` OpenAPI skeleton,
and the deferred minor review findings listed in the milestone ledgers.
