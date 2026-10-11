# IPT4 M7 (part): remove the Struts tag shim, Bootstrap 5.3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The templates no longer use the Struts tag shim `s`; its Java directives are deleted. Bootstrap goes from 5.0.1 to 5.3.8, the latest stable release on 2026-10-11 (6.0.0 is an alpha). Pages look and behave as before.

**Architecture:**
- **One text function.** A FreeMarker method `t(key, args...)`, registered as a shared variable, replaces `<@s.text>`/`<@s.param>`.
  - It returns HTML markup, like `s.text`: the message itself is not escaped (some messages contain links).
  - Plain string arguments are HTML-escaped, matching the old `<@s.param>${x}</@s.param>`, whose body was auto-escaped.
  - Markup arguments (a captured `<#assign x>…</#assign>`, or another `t(...)`) are inserted as they are.
  - Numbers are formatted as `${n}` formats them.
  - `escapeHtml=true` becomes `${t(k)?markup_string}`; `var=` becomes `<#assign>`.
- **Form tags become plain HTML.** `s.submit`, `s.hidden`, `s.file`, `s.checkbox`, `s.select`, `s.fielderror` and `s.property` become the markup they produced, or FreeMarker expressions. `fielderror` becomes a macro in `macros/form_field_error.ftl`.
- **Automated conversion.** `tools/ftl-migrate/FtlMigrate.java` gets the conversion, so upstream IPT 3 templates can be converted the same way. Constructs it cannot convert safely are reported and converted by hand.
- **Proof of equivalence.** The crawler can save every page it visits as normalised HTML:
  - normalisation: attributes sorted, entities normalised by jsoup, CSRF tokens and dates masked;
  - a snapshot is taken before and after the conversion and diffed;
  - every remaining difference is listed and explained in the ledger.

**Tech Stack:** as M6; FreeMarker 2.3.x `TemplateMethodModelEx`, jsoup (test).

**Spec:** `docs/design.md` §5 (runtime shim, removed in M7) and §7 M7.

## Global Constraints

- No visible change to pages except the ones listed in the ledger. Message keys and bundles stay unchanged.
- No new XSS: values that were escaped stay escaped; only message texts are emitted unescaped, as with `s.text`.
- `PageFallbackHash` (page-bean variables), `csrfField` and the Java action layer (`ActionPage`, `ActionRunner`) stay. They are not part of the tag shim.
- Local commits on main only; no push.

## Review Focus

1. **Escaping of arguments:** a resource title such as `<script>` used as a message argument stays escaped. Test: Task 2 `stringArgumentsAreEscaped`, plus the snapshot diff.
2. **Markup arguments** (links in messages such as the registration page's) render as links, not as escaped text. Test: Task 2 `markupArgumentsAreInserted`.
3. **JavaScript strings:** texts inside `<script>` and `'...'` keep their quoting (`escapeJavaScript`, DataTables cells). Covered by the snapshot diff (scripts kept in the snapshot).
4. **Form field names and ids** (`id` derived from `name` like Struts) still match the JavaScript and labels that refer to them. Covered by the snapshot diff and the existing form tests.
5. **Bootstrap 5.2+ CSS-variable components** (buttons, tables, nav, dropdowns) under the IPT's colour overrides. Screenshots before and after (Task 5).

---

### Task 1: Page snapshots from the crawler

- `CrawlerTest`: when the system property `ipt.snapshots` names a directory, write every HTML page it visits to `<dir>/<logged-out|admin>/<sanitised url>.html`.
- Normalisation (`PageSnapshot.normalise(String html)` in test code):
  - attributes sorted per element, and the jsoup `outerHtml()` with pretty printing;
  - values of `_csrf` inputs and `meta[name=_csrf]` masked;
  - text matching `\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}(:\d{2})?` masked.
- Test `PageSnapshotTest`: equivalent markup gives equal output (attribute order, `&#39;` vs `'`, `&amp;` in href); different text does not.
- Run the crawler with snapshots on the current tree into `<scratchpad>/snap-before`.

### Task 2: The `t` text function

- `TextFunction implements TemplateMethodModelEx` in `web.support.freemarker`, registered as `t` in `IptTemplateVariables` (next to `s` for now).
- Tests (`TextFunctionTest`, FreeMarker `Configuration` as the app configures it, HTML output format):
  - `rawMessage`: a message containing `<a>` renders unescaped;
  - `stringArgumentsAreEscaped`: `t("k", "<b>")` gives `&lt;b&gt;` inside the message;
  - `markupArgumentsAreInserted`: `<#assign a><a href="x">y</a></#assign>${t("k", a)}`;
  - `numbersFormattedLikeInterpolation`: `t("k", 1234)` equals `${1234}`;
  - `markupStringEscapes`: `${t("k")?markup_string}` escapes;
  - `missingKey`: same fallback as `s.text` (the key itself);
  - `locale`: the environment's locale.

### Task 3: Converter in FtlMigrate

- `FtlMigrate.migrate` converts:
  - `s.text` (with `name`, `escapeHtml`, `escapeJavaScript`, `var`, and `s.param` children);
  - `s.submit`, `s.hidden` (only with an explicit `value`), `s.file`, `s.fielderror` (to `<@fieldError>`);
  - `s.property` (simple paths).
- Constructs it cannot convert stay unchanged and are reported, among them: `s.select`, `s.checkbox`, `s.hidden` without `value`, OGNL `#` and `%{}`, and param bodies with directives other than `s.text`.
- Test cases in `tools/ftl-migrate/test/{input,expected}/shim.ftl` cover each rule, square and angle syntax, nesting, and idempotency. `test.sh` checks the report lines.

### Task 4: Convert the templates, delete the shim

- Run the converter over `ipt-app/src/main/resources/templates` in place, and hand-convert the reported leftovers.
- Delete `IptTagLibrary` and its directives (`Text`, `Param`, `ParamStack`, `Submit`, `Hidden`, `File`, `Checkbox`, `Select`, `FieldError`, `Property`, `TagAttributes`, `HtmlTag` if unused), plus the shim probe templates and `TemplateShimTest`. Probe tests for `t` replace them.
- `TemplatesParseTest` and the crawler (no `<@s`/`[@s`) stay green; the full verify is green.
- Snapshot again into `<scratchpad>/snap-after`, then `diff -r`. Every difference is explained in the ledger (expected: entity-level escaping differences only, which the normalisation already removes).

### Task 5: Bootstrap 5.3.8

- Vendor `bootstrap.bundle.min.js` and `bootstrap.min.css` 5.3.8 (from the npm tarball; record sha256 in the manifest).
- Search templates, CSS and JS for classes and APIs deprecated or changed between 5.0 and 5.3, and adapt them where the look changes.
- Screenshots of about 12 key pages (home, resource, login, manage home, overview, metadata basic, mapping, admin home, config, users, extensions, registration) at 1280 px, taken before and after with headless Chrome. Regressions are fixed and the remaining differences recorded.

### Task 6: Docs and exit

- `docs/design.md` and `docs/parity-checklist.md` (M7 part done), the asset manifest, and `docs/deviations.md` if anything visible changed.
- Full verify, node tests, ftl-migrate test, Docker smoke.
- Final review by a fresh reviewer; one fix pass.
