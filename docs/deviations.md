# Intentional differences from IPT 3

Each entry says what changed and why. Keep this list current: it becomes the IPT 4 release notes.

| Area | IPT 3 | IPT 4 | Why |
|---|---|---|---|
| Packaging | WAR for Tomcat/Jetty, RPM | Executable jar and Docker image | One supported runtime; no servlet-container setup |
| Data directory | Chosen in setup step 1 or `WEB-INF/datadir.location` | `IPT_DATA_DIR` / `ipt.data-dir` only; IPT 3 data directories are converted once with the `migrate` command | A jar has no `WEB-INF`; containers configure by environment |
| Translations | Messages formatted twice, dropping apostrophes (fr: "na pas") | Formatted once ("n'a pas") | Bug fix |
| Missing translations | Raw message key shown when a translation lacks a key | English text | The English bundle is the fallback per key, not only per locale |
| Login redirect | Absolute URL | Relative `Location: /login` (context path kept) | Spring Security 7 default; correct behind reverse proxies |
| Forwarded headers | Not used; base URL always configured | Derived from the request when `ipt.base-url` is blank; `X-Forwarded-*` honoured only from `ipt.trusted-proxies` (default: loopback and private networks) | Works behind proxies and sub-paths without letting clients forge links |
| Logout | GET link `logout.do` | POST form | CSRF protection of state changes |
| CSRF | Login form only | Every POST form and AJAX request | Security |
| Template errors | HTML debug output inside the page | Request fails (error page); debug output only with `ipt.dev-mode` | No stack traces shown to visitors |
| Accept-Language | Ignored (configured default) | Ignored (configured default) | Unchanged, listed for clarity |
| Error Prone | Compiled with Error Prone (motherpom) | Plain javac | Error Prone 2.15 in motherpom 61 does not run on JDK 25; re-enable with a current version later |
| Build and release | Jenkins | GitHub Actions builds and tests every push; images go to `ghcr.io/mdoering/ipt4` (`main` and every pushed tag `v<version>`), not to Docker Hub while IPT 4 is experimental | Public builds of the port; GBIF has not approved IPT 4 yet |
| Health check | `/api/health` (JSON) | `/api/health` with IPT 3's JSON, plus `/actuator/health` for the Docker health check | Container orchestration expects a standard health endpoint |
| Data dir storage | XStream XML (`resource.xml`, `users.xml`, `registration2.xml`) | JSON (`resource.json`, `users.json`, `registration.json`), written atomically | Simple, safe, readable format without reflection hacks |
| Stored secrets | PBE with a key compiled into the IPT | AES-256-GCM with a per-installation key (`config/.secret.key` or `IPT_SECRET_KEY`) | The IPT 3 key is public |
| Registry unreachable at start-up | IPT fails to start | IPT starts and shows a start-up warning | An outage of the GBIF registry should not take IPTs down |
| Vocabularies without registry | Installed vocabularies are only loaded when the GBIF registry answers; publishing then fails | Installed vocabularies are loaded anyway | An outage of the GBIF registry should not stop publishing |
| Uploaded IPT 3 resource folders | Read with XStream allowing any Java type | Read with XStream limited to the IPT's own classes and JDK value types | Uploaded files must not be able to instantiate arbitrary classes |
| Action messages | Rendered with `?interpret`, so FreeMarker code inside a message ran | Rendered as HTML without evaluation | Messages can contain user input (resource titles); no server-side template injection |
| Secret key at start-up | n/a (one built-in key) | A data dir without its key, or with another key, refuses to start; the marker holds a key check | Stored passwords must never be silently lost |
| Resource folders from another IPT | Imported with SQL passwords (same built-in key everywhere) | Imported; SQL passwords encrypted by the other installation are dropped with a warning | Each installation has its own key |
| Data dirs of newer IPT versions | No check | Refused at start-up (and files of a newer format are not read) | Rolling back must not silently lose data |
| `inventory/v2/dataset?status=` | `private` and `deleted` listed those resources (titles, record counts, links) | Only `public` and `registered` are honoured; other values give the default list of published public resources | The inventory is anonymous; private resources must stay hidden |
| `api/resources` paging parameters | Non-numeric `start`, `length` or `order[0][column]` gave a 500 error page | 400 Bad Request | Bad input is the client's error |
| `Cache-Control` | Not sent | `no-cache, no-store` on HTML pages (Spring Security default); JSON keeps IPT 3's `no-cache`, downloads and static assets send none | Pages show user-specific content |
| Login | Struts action with its own CSRF cookie; session id kept | Spring Security form login on the same URL and fields; CSRF token on every form; session id changes at login | Session fixation protection |
| Failed login | Form shown again with the email and the rejected password filled in | Email kept, password field empty | Passwords do not belong in HTML |
| Password change | New password kept in memory until users were next saved (lost on restart) | Saved at once | Bug fix |
| Rights of a logged-in user | Session held the user object: role changes applied at once, a deleted user stayed logged in | Read from the user record on every request: role changes apply at once, deleted users lose access | Removing an account must take effect |
| Setup login | Step 2 and the last step put the new admin into the session; the last step used a one-time token shared by all visitors | The new admin is logged in (new session id) in the browser that created the account; the last step logs no one in | Whoever reached the last step first could become admin |
| Pages before setup is complete | Redirected to setup step 1 | Redirected to setup step 2; static files, login and the health checks stay available | Step 1 (data dir) is gone; container health checks must work during setup |
| Startup warnings | Listed before the page's own warnings, also on pages reached by a redirect | Listed after the page's own warnings, only on rendered pages | The page shows three warnings; the user's last action comes first |
| Unknown URLs | Unknown actions showed the home page of their area (Struts default action); other paths got the servlet container's page | `error/404.ftl` with status 404 | A missing page should say so |
| CSRF token missing or stale | n/a (only the login form had a token) | 403 with `error/error.ftl` | Every form is protected now |
| Last requested resource | Remembered in the session before the access check; later requests without `r` (`eml.do`, `archive.do`, ...) served it, private or not | Checked like `r`, and remembered only once access is granted | Private data must not be downloadable by visitors |
| Version parameter `v` | Any number, including exponents (`1e100000000` built a 100 MB page) | Plain versions only (`12`, `1.3`); anything else is 404 | Unauthenticated memory exhaustion |
| `If-Modified-Since` in the future | 304 | Ignored, file served (RFC 9110) | Clients with a wrong clock get the file |
| `adminEmail` request parameter on the login page | Replaced the admin contact in the account request and password reset links | Ignored | Phishing through links on the IPT's own domain |
| Setup steps 3 and 4 | Open to any visitor until setup was complete | Once the admin exists, only for the logged-in admin | A visitor could set the base URL during setup |
| Creating resources while the GBIF registry is unreachable | Failed (the registry was asked for a data package schema identifier, for every resource type) | Works; installed schemas are looked up first | An outage of the GBIF registry should not stop managers |
| Uploaded file names | Used as sent by the browser, path parts included, for temporary files | Only the file name part is used; uploads are stored under generated names | Upload names must not choose where files go |
| Binding of manage forms | The live resource was bindable to depth 8 (`resource.creator.role=Admin` and the like were postable) | Only the fields a page lists; never into users, organisations, extensions, vocabularies, schemas or resources through another object | Privilege escalation through form fields |
| Remembered resource in the manage area | Remembered before the access check; pages without `r` used it unchecked | Checked like `r`, remembered only once access is granted | A manager could reach another manager's resource |
| `canceloverwrite.do` | Mapped to a method the action did not have (error) | Returns to the resource overview | The overwrite dialog's Cancel button works |
| Delete source / delete mapping links | GET links (state change on GET) | The confirmation dialog sends a POST with the CSRF token | No state changes on GET; CSRF protection |
| Stale mapping or source index in manage links | Error page (500) | Manage 404 page | A link from another tab should not look like a crash |
| Request parameters on GET | Bound like a POST's, nested paths included | A GET binds the page's own top-level parameters only | A GET carries no CSRF token; nested paths reach live objects |
| Binding depth | `@StrutsParameter(depth)` limited nesting | Same limit, and paths are also checked against the objects actually there (no files, no shared objects such as installed extensions) | A declared type (Source, Term) can hide a file or an installed extension |
| Cancel publication | GET link, no access check | POST (the links send one) for the resource's managers | State change without CSRF protection |
| Remove co-manager / network links | GET | POST from the same links | No state changes on GET |
| Remembered resource | Used by home and create too | Used only by pages working on a resource; one deleted meanwhile is forgotten | Home showed another resource's lock or error |
| Rejected metadata section | The invalid values stayed on the resource in memory (and were published or saved with the next change) | The form shows the invalid values; the resource keeps its previous metadata | A rejected form must not change data |
| DocBook text fields (description, acknowledgements, purpose, introduction, getting started) | The HTML filter removed every DocBook element the editors post, so these sections could not be saved with structured text | Passed as posted when they hold only DocBook elements, no attribute but a link's url, and only web or mail links; anything else is refused with the filter's message (whatever section posts the field) | The editors work, without markup the editors' script would make live |
| EML type conversions | The conversion files for EML dates and coordinates were no longer found (classes moved), Struts' defaults applied | IPT's converters as intended (year-only dates, comma decimals, coordinate ranges) | Dates and coordinates behave as IPT documents them |
| Resource logo upload | Any type, named after the type the browser declared | JPEG, GIF or PNG only | Only images are logos |
| ColDP metadata file on create | Only a `.yml` file name was recognised | `.yml` and `.yaml` | ColDP's own examples use `metadata.yaml` |
| `datapackage-metadata-*` on a ColDP resource | Error page (500); the overview offers no editor for ColDP | Manage 404 page | ColDP metadata is replaced by upload, not edited |
| Camtrap DP contributor role | Saved as the select's key (`PUBLISHER`), which the form and the JSON reader did not recognise again, so the role was lost on the next save or restart | Saved as the role value (`publisher`) | Roles survive editing |
| Passwords in forms (login, account, users, SQL sources) | Run through the HTML filter (`<`, entities changed) and written to the log | Taken as typed; the filter logs parameter names, never values | A password is what was typed; logs are readable by admins |
| Entity-encoded markup in plain-text fields (`&lt;img ...&gt;`) | Decoded into markup and saved | Refused with the filter's message | It was shown unescaped on the public resource page |
| Resource fields posted with a rejected metadata section (type, subtype, citation and infer flags, organisation) | Kept on the resource and saved with the next change | Put back | A rejected form must not change data |
| Form posts | Tomcat's form size; list indexes created every element up to them | At most 10 MB; a request may create at most 10 list elements per parameter (at least 10,000), more gives 400 | One request could exhaust the memory |
| Admin configuration saved | Redirected to `/config.do`, which the portal answered with the home page | Back to the configuration page with its message | The admin sees what was saved |
| UI management logo upload | Any file type, stored under the type the browser declared | JPEG, GIF or PNG only | Only images are logos |
| Editing a user account | A request could change the account's email or store a plain password (the form posts neither) | Email and password stay; passwords change only by reset or the account page | One account cannot overwrite another |
| Remove an installed extension | The button posted `delete`, which the action did not read: it tried to install a missing URL instead and removed nothing | Removes the extension (unless a resource maps it) | The button works |
| Synchronise extensions and vocabularies | Also on a GET with `?synchronise=true` | Only from the page's button (POST) | No state changes on GET |
| Install an extension by URL | Any URL, including `file:` | Web links (http, https) only | A file URL read a local file |
| Unknown data package schema page | Error page (500) | 404 | An old link should not look like a crash |
| Organisation passwords (GBIF and DOI agency) in the organisation form | Written into the page | Never shown; an empty field keeps the stored password | Secrets do not belong in pages |
| Registered IPT fields (`registeredIpt.*`) | Bindable on every page (any manager's form could change the IPT's registered name or contacts in memory) | Only the registration update form, applied after the registry accepted the update | Shared data changed only where it is edited |
| Organisation that cannot be removed | Error page while showing the message | The organisation form with the message | The reason is visible |
| Organisation login check failing | Logged the organisation's password | Logs the key only | No passwords in logs |
| Delete a published version | GET link; returned to `/resource` without the resource; an unknown resource failed (500) | POST from the same confirmed link; returns to the resource page; unknown resource or version 404 | No state changes on GET |
| Publication status JSON (`admin-api/publication-report(s)`) | Included the exception object as serialised by Struts | Its message and stack trace (the properties IPT 3 also had) without the object | Stable, bounded JSON |
| Bulk publication log popup | Messages inserted as HTML | Shown as text | Messages carry resource titles |
| Vocabulary pages with terms without a language | Template error | The term without the language | Registry vocabularies have such terms |
| Rejected configuration save | Settings before the invalid one stayed in effect (and were saved with the next change); a new base URL rewrote resource metadata and logged out even when a later setting failed | Nothing changes until every setting is valid and saved; then the base URL's consequences follow | A rejected form must not change data |
| Synchronise organisations button | Its flag was not bound, so the button ran the organisation form's validation and synchronised nothing | Synchronises the associated organisations with the registry | The button works |
| Session cookie | `HttpOnly` | `HttpOnly` and `SameSite=Lax` (`Secure` on request for HTTPS installations) | Not sent with other sites' cross-site posts |
| Registration and DOI double clicks | Two simultaneous requests could both register the resource (two GBIF datasets) or reserve two DOIs | The second request is refused while the first runs | One registry entry and one DOI per resource |
| Failed publication | Kept the failed archive's checksum, so "skip if not changed" later skipped data that was never published | The previous publication's checksum and record counts are kept | Auto-publishing publishes changed data after an outage |
| Organisation form, node key | The hidden node key field was filled with the contact phone number | Filled with the node key | Bug fix |
| Mapping: use the resource DOI as datasetID | The checkbox's value was not bound, so the choice was never saved | Saved when ticked and when unticked | Bug fix |
| Web font | Roboto loaded from Google Fonts (`fonts.googleapis.com`, `fonts.gstatic.com`) | Roboto served by the IPT itself (the same variable-font files, from `@fontsource-variable/roboto`) | Visitors' browsers make no requests to Google (privacy); works on networks without Internet access |
| Metadata files during publication | `eml.xml` and `datapackage.json` rewritten in place, so a publication running alongside a metadata save could copy a truncated file into the published version | Written to a temporary file and moved into place; the published copy too | Bug fix: a torn file broke that version's resource page for good |
| Resource page for a version whose EML file is missing | Error page (status 500) | Not found (404), as for a missing data package file; happens briefly for managers while a publication is writing the new version | A missing file is not a server error |
| Dates formatted during concurrent requests | Formatters shared between threads: the home page's resource table, DataCite and DCAT dates and the camtrap temporal scope could fail or show wrong dates under load | One formatter per thread | Bug fix |
