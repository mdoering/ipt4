# IPT4 M6 (Registry/DOI, hardening, release) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resource registration with GBIF, networks and DOIs work on IPT4 as on IPT 3. Security and load behaviour are checked across the whole application. The documentation and the release pipeline for 4.0.0-RC1 are ready. Nothing is published, registered or released without the user's go-ahead.

**Architecture:**
- **Offline first.** Registry calls go to `StubRegistry` (M5), and DOI calls go to a new `StubDataCite`, a JDK `HttpServer` speaking the DataCite REST JSON API (`/dois`) and checking HTTP Basic credentials. `dev.datacitedev.url`/`dev.datacite.url` point at it per test class.
- **Real services only on request.** `@Tag("uat")` tests register a test resource with GBIF UAT and reserve and delete a DOI on DataCite test, using credentials from environment variables. They are excluded by default, like `network`, and are **not run** in this milestone: the user decides.
- **Release pipeline:** a GitHub Actions workflow with manual dispatch builds and pushes `gbif/ipt:<version>` to docker.gbif.org and Docker Hub, and a `Jenkinsfile` mirrors GBIF's release job. Neither is triggered here: no tag, no push.

**Tech Stack:** as M5.

**Spec:** `docs/design.md` milestone M6; §4 (security), §8 (verification).

## Global Constraints

- No outward-facing action: no requests to GBIF UAT, DataCite or Docker registries, no tags, no pushes (the user's standing instruction).
- Registry and DataCite behaviour as IPT 3: same requests (paths, form fields, JSON), same messages.
- Secrets (organisation and DataCite passwords, IPT ws password) are never logged or rendered.
- The authorization matrix covers every handler of the application.

## Review Focus

1. **DOI lifecycle consistency.** Reserve, publish (register), republish (update), delete (reserved: delete; registered: deactivate): the resource's DOI state and DataCite's must not diverge when DataCite fails midway. Test: Task 2 `dataCiteFailureLeavesTheResourceConsistent`.
2. **Registry updates on publish.** Publishing a registered resource updates the registry; when the registry update fails, the publication fails and the previous version stays published (IPT 3 behaviour), without leaving the resource locked. Test: Task 1 `registryDownDuringPublication`.
3. **Every POST needs CSRF, and every handler is authorised.** All mappings are enumerated: anonymous users, users, managers (own and others' resources) and admins, with and without the CSRF token. Test: Task 4 `ApplicationAuthorizationMatrixTest`.
4. **Concurrency.** Concurrent portal reads, manager saves and publications produce no errors or deadlocks (M4 C-1). Test: Task 4 `ConcurrentUseTest` (RANDOM_PORT).
5. **Credentials in DOI and registry calls.** Organisation and agency credentials go only to their own service (Basic auth) and never appear in logs or pages. Tests: Tasks 1–2 assert on stub requests and log output.

---

### Task 1: Resource registration, registry updates, networks

- Overview actions against StubRegistry:
  - `resource-registerResource` (IPT 3's POST `/registry/ipt/resource` with organisation credentials; the response key stored);
  - registry update after publishing a registered resource;
  - deregistration when a registered resource is deleted;
  - `resource-addNetwork`/`resource-deleteNetwork` and the networks list.
- Tests:
  - register (request fields, key stored, overview shows it);
  - publish a registered resource (update request);
  - `registryDownDuringPublication`;
  - delete a registered resource (deregister request);
  - add and remove a network.

### Task 2: DOIs

- `StubDataCite` test support.
- An organisation with a DataCite account (primary agency) and archival mode.
- `resource-reserveDoi`, `resource-deleteDoi`, publishing with a DOI (register, then update), and deleting a resource with a DOI.
- Tests:
  - reserve (draft created on the stub, DOI on the resource);
  - publish (DOI registered with metadata that includes the citation);
  - republish (metadata updated);
  - delete a reserved DOI;
  - `dataCiteFailureLeavesTheResourceConsistent`;
  - agency credentials sent only to the stub, never logged.

### Task 3: UAT and DataCite test (prepared, not run)

- `@Tag("uat")` tests with credentials from environment variables (`IPT_UAT_ORG_KEY`, `IPT_UAT_ORG_PASSWORD`, `IPT_DATACITE_TEST_USER`, `IPT_DATACITE_TEST_PASSWORD`, `IPT_DATACITE_TEST_PREFIX`): register an IPT and a resource in UAT, publish, reserve, register and delete a DOI on DataCite test, then clean up. The surefire setup excludes `uat`. A README section explains how to run them.

### Task 4: Hardening

- `ApplicationAuthorizationMatrixTest`: every mapping (portal, account, api, manage, admin), roles × CSRF; expectations per area.
- Security headers and session: frame options, `X-Content-Type-Options`, cookie flags (HttpOnly, SameSite, Secure behind HTTPS), session id rotated at login.
- `ConcurrentUseTest` (RANDOM_PORT): 4 threads × 50 portal and manage GETs, plus metadata saves and a publication. No 5xx, no deadlock, and p95 below a generous bound.

### Task 5: Documentation

- Antora component `docs/antora.yml` with the modules ROOT and pages: index, installation (jar, Docker), configuration (`ipt.*`, `IPT_DATA_DIR`, proxy, secrets key), migration from IPT 3 (`migrate` command, report, rollback), deviations (from `docs/deviations.md`), and running the tests (network/uat tags).
- `README.md` links them.

### Task 6: Release preparation

- `.github/workflows/release.yml` (workflow_dispatch: version input; builds, runs verify and smoke, pushes the image to docker.gbif.org and Docker Hub with credentials from secrets) and a `Jenkinsfile`.
- Version stays 4.0.0-SNAPSHOT in the POMs; the workflow sets the release version.
- `docs/parity-checklist.md`: each IPT 3 feature area with the test that covers it, the deviations, and what is left for M7 or the user (UAT run, publishing).

### Task 7: M6 exit checks

- Full verify, node tests, Docker smoke, crawler, authorization matrix. Then report to the user: what needs their go-ahead (UAT/DataCite test run, release workflow, image push).
