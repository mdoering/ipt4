# Intentional differences from IPT 3

Each entry says what changed and why. Keep this list current: it becomes the IPT 4 release notes.

| Area | IPT 3 | IPT 4 | Why | Since |
|---|---|---|---|---|
| Packaging | WAR for Tomcat/Jetty, RPM | Executable jar and Docker image | One supported runtime; no servlet-container setup | M0 |
| Data directory | Chosen in setup step 1 or `WEB-INF/datadir.location` | `IPT_DATA_DIR` / `ipt.data-dir` only | A jar has no `WEB-INF`; containers configure by environment | M0 |
| Translations | Messages formatted twice, dropping apostrophes (fr: "na pas") | Formatted once ("n'a pas") | Bug fix | M0 |
| Missing translations | Raw message key shown when a translation lacks a key | English text | The English bundle is the fallback per key, not only per locale | M0 |
| Login redirect | Absolute URL | Relative `Location: /login` (context path kept) | Spring Security 7 default; correct behind reverse proxies | M0 |
| Logout | GET link `logout.do` | POST form | CSRF protection of state changes | M0 |
| CSRF | Login form only | Every POST form and AJAX request | Security | M0 |
| Template errors | HTML debug output inside the page | Request fails (error page); debug output only with `ipt.dev-mode` | No stack traces shown to visitors | M0 |
| Accept-Language | Ignored (configured default) | Ignored (configured default) | Unchanged, listed for clarity | M0 |
| Error Prone | Compiled with Error Prone (motherpom) | Plain javac | Error Prone 2.15 in motherpom 61 does not run on JDK 25; re-enable with a current version later | M0 |
| CI | Jenkins | GitHub Actions; Jenkins release pipeline in M6 | M0 has nothing to deploy | M0 |
| Home page | Portal home | Redirect to `/about` until M2 | Portal home is ported in M2 | M0 (temporary) |
| Health check | `/api/health` (JSON) | `/actuator/health` for Docker; `/api/health` returns in M2 with its IPT 3 JSON shape | Container health check needed before the portal is ported | M0 (temporary) |
