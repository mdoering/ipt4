# Intentional differences from IPT 3

Each entry says what changed and why. Keep this list current: it becomes the IPT 4 release notes.

| Area | IPT 3 | IPT 4 | Why | Since |
|---|---|---|---|---|
| Packaging | WAR for Tomcat/Jetty, RPM | Executable jar and Docker image | One supported runtime; no servlet-container setup | M0 |
| Data directory | Chosen in setup step 1 or `WEB-INF/datadir.location` | `IPT_DATA_DIR` / `ipt.data-dir` only | A jar has no `WEB-INF`; containers configure by environment | M0 |
| Translations | Messages formatted twice, dropping apostrophes (fr: "na pas") | Formatted once ("n'a pas") | Bug fix | M0 |
| Missing translations | Raw message key shown when a translation lacks a key | English text | The English bundle is the fallback per key, not only per locale | M0 |
| Login redirect | Absolute URL | Relative `Location: /login` (context path kept) | Spring Security 7 default; correct behind reverse proxies | M0 |
| Forwarded headers | Not used; base URL always configured | Derived from the request when `ipt.base-url` is blank; `X-Forwarded-*` honoured only from `ipt.trusted-proxies` (default: loopback and private networks) | Works behind proxies and sub-paths without letting clients forge links | M0 |
| Logout | GET link `logout.do` | POST form | CSRF protection of state changes | M0 |
| CSRF | Login form only | Every POST form and AJAX request | Security | M0 |
| Template errors | HTML debug output inside the page | Request fails (error page); debug output only with `ipt.dev-mode` | No stack traces shown to visitors | M0 |
| Accept-Language | Ignored (configured default) | Ignored (configured default) | Unchanged, listed for clarity | M0 |
| Error Prone | Compiled with Error Prone (motherpom) | Plain javac | Error Prone 2.15 in motherpom 61 does not run on JDK 25; re-enable with a current version later | M0 |
| CI | Jenkins | GitHub Actions; Jenkins release pipeline in M6 | M0 has nothing to deploy | M0 |
| Home page | Portal home | Redirect to `/about` until M2 | Portal home is ported in M2 | M0 (temporary) |
| Health check | `/api/health` (JSON) | `/actuator/health` for Docker; `/api/health` returns in M2 with its IPT 3 JSON shape | Container health check needed before the portal is ported | M0 (temporary) |
| Data dir storage | XStream XML (`resource.xml`, `users.xml`, `registration2.xml`) | JSON (`resource.json`, `users.json`, `registration.json`), written atomically | Simple, safe, readable format without reflection hacks | M1 |
| Stored secrets | PBE with a key compiled into the IPT | AES-256-GCM with a per-installation key (`config/.secret.key` or `IPT_SECRET_KEY`) | The IPT 3 key is public | M1 |
| Data dir location | Chosen in setup step 1, stored in `WEB-INF/datadir.location` | `IPT_DATA_DIR` / `ipt.data-dir` only; IPT 3 data dirs must be converted with `ipt migrate` | No WAR; one-time migration | M1 |
| Registry unreachable at start-up | IPT fails to start | IPT starts and shows a start-up warning | An outage of the GBIF registry should not take IPTs down | M1 |
| Vocabularies without registry | Installed vocabularies are only loaded when the GBIF registry answers; publishing then fails | Installed vocabularies are loaded anyway | An outage of the GBIF registry should not stop publishing | M1 |
| Uploaded IPT 3 resource folders | Read with XStream allowing any Java type | Read with XStream limited to the IPT's own classes and JDK value types | Uploaded files must not be able to instantiate arbitrary classes | M1 |
| Action messages | Rendered with `?interpret`, so FreeMarker code inside a message ran | Rendered as HTML without evaluation | Messages can contain user input (resource titles); no server-side template injection | M2 |
| Secret key at start-up | n/a (one built-in key) | A data dir without its key, or with another key, refuses to start; the marker holds a key check | Stored passwords must never be silently lost | M1 |
| Resource folders from another IPT | Imported with SQL passwords (same built-in key everywhere) | Imported; SQL passwords encrypted by the other installation are dropped with a warning | Each installation has its own key | M1 |
| Data dirs of newer IPT versions | No check | Refused at start-up (and files of a newer format are not read) | Rolling back must not silently lose data | M1 |
| `inventory/v2/dataset?status=` | `private` and `deleted` listed those resources (titles, record counts, links) | Only `public` and `registered` are honoured; other values give the default list of published public resources | The inventory is anonymous; private resources must stay hidden | M2 |
| `api/resources` paging parameters | Non-numeric `start`, `length` or `order[0][column]` gave a 500 error page | 400 Bad Request | Bad input is the client's error | M2 |
| `Cache-Control` | Not sent | `no-cache, no-store` on HTML pages (Spring Security default); JSON keeps IPT 3's `no-cache`, downloads and static assets send none | Pages show user-specific content | M2 |
| Login | Struts action with its own CSRF cookie; session id kept | Spring Security form login on the same URL and fields; CSRF token on every form; session id changes at login | Session fixation protection | M2 |
| Failed login | Form shown again with the email and the rejected password filled in | Email kept, password field empty | Passwords do not belong in HTML | M2 |
| Password change | New password kept in memory until users were next saved (lost on restart) | Saved at once | Bug fix | M2 |
| Rights of a logged-in user | Session held the user object: role changes applied at once, a deleted user stayed logged in | Read from the user record on every request: role changes apply at once, deleted users lose access | Removing an account must take effect | M2 |
| Setup login | Step 2 and the last step put the new admin into the session; the last step used a one-time token shared by all visitors | The new admin is logged in (new session id) in the browser that created the account; the last step logs no one in | Whoever reached the last step first could become admin | M2 |
| Pages before setup is complete | Redirected to setup step 1 | Redirected to setup step 2; static files, login and the health checks stay available | Step 1 (data dir) is gone; container health checks must work during setup | M2 |
| Startup warnings | Listed before the page's own warnings, also on pages reached by a redirect | Listed after the page's own warnings, only on rendered pages | The page shows three warnings; the user's last action comes first | M2 |
| Unknown URLs | Unknown actions showed the home page of their area (Struts default action); other paths got the servlet container's page | `error/404.ftl` with status 404 | A missing page should say so | M2 |
| CSRF token missing or stale | n/a (only the login form had a token) | 403 with `error/error.ftl` | Every form is protected now | M2 |
| Last requested resource | Remembered in the session before the access check; later requests without `r` (`eml.do`, `archive.do`, ...) served it, private or not | Checked like `r`, and remembered only once access is granted | Private data must not be downloadable by visitors | M2 |
| Version parameter `v` | Any number, including exponents (`1e100000000` built a 100 MB page) | Plain versions only (`12`, `1.3`); anything else is 404 | Unauthenticated memory exhaustion | M2 |
| `If-Modified-Since` in the future | 304 | Ignored, file served (RFC 9110) | Clients with a wrong clock get the file | M2 |
| `adminEmail` request parameter on the login page | Replaced the admin contact in the account request and password reset links | Ignored | Phishing through links on the IPT's own domain | M2 |
| Setup steps 3 and 4 | Open to any visitor until setup was complete | Once the admin exists, only for the logged-in admin | A visitor could set the base URL during setup | M2 |
| Private resources (portal pages and downloads) | 401 (`error/401.ftl`) for visitors | Unchanged: 401 (the M2 plan's "404" was a slip; IPT 3 parity kept) | — | M2 |
| Creating resources while the GBIF registry is unreachable | Failed (the registry was asked for a data package schema identifier, for every resource type) | Works; installed schemas are looked up first | An outage of the GBIF registry should not stop managers | M3 |
| Uploaded file names | Used as sent by the browser, path parts included, for temporary files | Only the file name part is used; uploads are stored under generated names | Upload names must not choose where files go | M3 |
| Binding of manage forms | The live resource was bindable to depth 8 (`resource.creator.role=Admin` and the like were postable) | Only the fields a page lists; never into users, organisations, extensions, vocabularies, schemas or resources through another object | Privilege escalation through form fields | M3 |
| Remembered resource in the manage area | Remembered before the access check; pages without `r` used it unchecked | Checked like `r`, remembered only once access is granted | A manager could reach another manager's resource | M3 |
