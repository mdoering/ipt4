import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Converts IPT 3 (Struts) FreeMarker templates for IPT4. Re-runnable and idempotent, so templates changed upstream can
 * be converted again. See docs/design.md, section 5.
 *
 * <p>Struts tags become the text function {@code t(key, args...)} ({@code s.text}, {@code s.param}), plain HTML
 * ({@code s.submit}, {@code s.hidden}, {@code s.file}), the {@code fieldError} macro of macros/forms.ftl
 * ({@code s.fielderror}) or interpolations ({@code s.property}). Tags it cannot convert safely stay and are reported.
 *
 * <p>Usage: {@code java tools/ftl-migrate/FtlMigrate.java <sourceDir> <targetDir>}
 */
public class FtlMigrate {

  private static final String FORM = "<form\\b[^>]*\\bmethod\\s*=\\s*[\"']post[\"'][^>]*>";
  private static final Pattern POST_FORM = Pattern.compile(FORM, Pattern.CASE_INSENSITIVE);
  private static final Pattern POST_FORM_AT_LINE_START =
      Pattern.compile("(?m)^[ \\t]*" + FORM, Pattern.CASE_INSENSITIVE);
  private static final Pattern FIRST_FTL_TAG = Pattern.compile("([<\\[])[#@]");
  private static final Pattern PARAMETERS = Pattern.compile("(?<![\\w.])Parameters\\.");
  private static final List<String> LEFTOVERS = List.of("%{", "#attr", "stack.", "#parameters", "#session");
  private static final Pattern T_VARIABLE =
      Pattern.compile("[<\\[]#(list\\s[^>\\]]*\\sas\\s+t\\b|(assign|local|global)\\s+t\\b)");
  private static final Pattern STRUTS_TAG = Pattern.compile("[<\\[]@s\\.(\\w+)");

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println("Usage: java FtlMigrate.java <sourceDir> <targetDir>");
      System.exit(2);
    }
    Path source = Path.of(args[0]);
    Path target = Path.of(args[1]);
    List<Path> templates;
    try (Stream<Path> files = Files.walk(source)) {
      templates = files.filter(p -> p.toString().endsWith(".ftl")).sorted().toList();
    }
    for (Path file : templates) {
      Path relative = source.relativize(file);
      String migrated = migrate(Files.readString(file, StandardCharsets.UTF_8));
      Path out = target.resolve(relative.toString());
      Files.createDirectories(out.getParent());
      Files.writeString(out, migrated, StandardCharsets.UTF_8);
      report(relative, migrated);
    }
    System.err.println("Migrated " + templates.size() + " templates into " + target);
  }

  static String migrate(String template) {
    String out = template.replace("/WEB-INF/pages/", "/");
    out = out.replace("Session.curr_user", "currentUser");
    out = PARAMETERS.matcher(out).replaceAll("RequestParameters.");
    out = new StrutsTags(out).convert();
    return insertCsrfFields(out, csrfField(out));
  }

  static String csrfField(String template) {
    Matcher m = FIRST_FTL_TAG.matcher(template);
    return m.find() && m.group(1).equals("[") ? "[@csrfField/]" : "<@csrfField/>";
  }

  static String insertCsrfFields(String template, String field) {
    Matcher m = POST_FORM_AT_LINE_START.matcher(template);
    StringBuilder out = new StringBuilder();
    int last = 0;
    while (m.find()) {
      out.append(template, last, m.end());
      String rest = template.substring(m.end()).stripLeading();
      if (!rest.startsWith("<@csrfField") && !rest.startsWith("[@csrfField")) {
        out.append('\n').append(field);
      }
      last = m.end();
    }
    return out.append(template.substring(last)).toString();
  }

  static void report(Path file, String template) {
    Matcher m = POST_FORM.matcher(template);
    while (m.find()) {
      int lineStart = template.lastIndexOf('\n', m.start()) + 1;
      if (!template.substring(lineStart, m.start()).isBlank()) {
        System.err.printf(
            "WARN %s:%d: POST form not at line start, add the CSRF field manually%n", file, lineOf(template, m.start()));
      }
    }
    String[] lines = template.split("\n", -1);
    for (int i = 0; i < lines.length; i++) {
      if (T_VARIABLE.matcher(lines[i]).find()) {
        System.err.printf("WARN %s:%d: variable t hides the text function t()%n", file, i + 1);
      }
      Matcher tag = STRUTS_TAG.matcher(lines[i]);
      while (tag.find()) {
        if (!tag.group(1).equals("param")) {
          System.err.printf("WARN %s:%d: Struts tag s.%s needs manual migration%n", file, i + 1, tag.group(1));
        }
      }
      for (String marker : LEFTOVERS) {
        if (lines[i].contains(marker)) {
          System.err.printf("WARN %s:%d: Struts/OGNL construct '%s' needs manual migration%n", file, i + 1, marker);
        }
      }
    }
  }

  private static int lineOf(String text, int index) {
    int line = 1;
    for (int i = 0; i < index; i++) {
      if (text.charAt(i) == '\n') {
        line++;
      }
    }
    return line;
  }

  /** Converts the Struts tags of one template; tags it cannot convert are kept as they are. */
  static final class StrutsTags {

    private final String src;
    private int captures;

    StrutsTags(String src) {
      this.src = src;
    }

    String convert() {
      return convert(src, true);
    }

    private String convert(String text) {
      return convert(text, false);
    }

    private String convert(String text, boolean topLevel) {
      StringBuilder out = new StringBuilder();
      Matcher m = STRUTS_TAG.matcher(text);
      int last = 0;
      while (m.find(last)) {
        Tag tag = Tag.parse(text, m.start());
        out.append(text, last, m.start());
        String converted = tag == null ? null : convert(tag);
        if (tag == null) {
          out.append(m.group());
          last = m.end();
        } else {
          if (converted != null && topLevel) {
            converted += keepWhitespaceStripping(tag, converted);
          }
          out.append(converted != null ? converted : tag.body == null ? text.substring(tag.start, tag.end)
              // kept, but the tags inside its body are converted (e.g. s.text in s.param)
              : text.substring(tag.start, tag.bodyStart) + convert(tag.body) + text.substring(tag.bodyEnd, tag.end));
          last = tag.end;
        }
      }
      return out.append(text.substring(last)).toString();
    }

    /**
     * FreeMarker drops the indentation and line break of lines holding only FTL tags, as lines with a Struts tag often
     * did; a line with the replacement (an interpolation or HTML) needs {@code <#t>}, {@code <#lt>} or {@code <#rt>}
     * to render the same.
     */
    private String keepWhitespaceStripping(Tag tag, String converted) {
      if (ftlOnly(converted).isEmpty()) {
        return "";
      }
      int firstLine = lineNumber(tag.start);
      int lastLine = lineNumber(tag.end - 1);
      boolean leading = ftlOnlyLine(firstLine) && !trimmed.contains(firstLine);
      boolean trailing = ftlOnlyLine(lastLine) && !trimmed.contains(lastLine);
      trimmed.add(firstLine);
      trimmed.add(lastLine);
      String directive = leading && trailing ? "#t" : leading ? "#lt" : trailing ? "#rt" : null;
      return directive == null ? "" : tag.directive(directive);
    }

    private final java.util.Set<Integer> trimmed = new java.util.HashSet<>();

    private int lineNumber(int index) {
      int line = 0;
      for (int i = 0; i < index; i++) {
        if (src.charAt(i) == '\n') {
          line++;
        }
      }
      return line;
    }

    private boolean ftlOnlyLine(int line) {
      String[] lines = src.split("\n", -1);
      return ftlOnly(lines[line]).isEmpty() && !lines[line].isBlank();
    }

    /** What remains of a line without its FTL tags and comments, stripped. */
    private static String ftlOnly(String line) {
      return line.replaceAll("<#--.*?-->|\\[#--.*?--]|</?[#@][^>]*>|\\[/?[#@][^\\]]*]", "").strip();
    }

    private String convert(Tag tag) {
      return switch (tag.name) {
        case "text" -> text(tag);
        case "submit" -> submit(tag);
        case "hidden" -> hidden(tag);
        case "file" -> file(tag);
        case "fielderror" -> fieldError(tag);
        case "property" -> property(tag);
        default -> null;
      };
    }

    // s.text and its s.param children -> ${t(key, args...)}

    private String text(Tag tag) {
      if (!tag.only("name", "escapeHtml", "escapeJavaScript", "var") || tag.attr("name") == null) {
        return null;
      }
      Boolean escapeHtml = tag.flag("escapeHtml");
      Boolean escapeJs = tag.flag("escapeJavaScript");
      if (escapeHtml == null || escapeJs == null) {
        return null;
      }
      StringBuilder assigns = new StringBuilder();
      List<String> args = new ArrayList<>();
      args.add(tag.attr("name").expression());
      if (tag.body != null) {
        List<String> params = params(convert(tag.body), tag.open);
        if (params == null) {
          return null;
        }
        for (String param : params) {
          String arg = argument(param, tag, assigns);
          if (arg == null) {
            return null;
          }
          args.add(arg);
        }
      }
      String call = "t(" + String.join(", ", args) + ")";
      Attr var = tag.attr("var");
      if (var != null) {
        if (escapeHtml || escapeJs || !var.quoted() || !var.value.matches("\\w+")) {
          return null;
        }
        return assigns + tag.directive("#assign " + var.value + " = " + call + "?markup_string");
      }
      String suffix = escapeJs ? "?markup_string?js_string?no_esc" : escapeHtml ? "?markup_string" : "";
      return assigns + "${" + call + suffix + "}";
    }

    /** The bodies of the s.param tags making up an s.text body, or null if it has other content (a default text). */
    private static List<String> params(String body, char open) {
      char close = open == '<' ? '>' : ']';
      String start = open + "@s.param" + close;
      String end = open + "/@s.param" + close;
      List<String> params = new ArrayList<>();
      int i = 0;
      while (true) {
        while (i < body.length() && Character.isWhitespace(body.charAt(i))) {
          i++;
        }
        if (i == body.length()) {
          return params;
        }
        if (!body.startsWith(start, i)) {
          return null;
        }
        int bodyStart = i + start.length();
        int bodyEnd = body.indexOf(end, bodyStart);
        if (bodyEnd < 0 || body.substring(bodyStart, bodyEnd).contains(start)) {
          return null;
        }
        params.add(body.substring(bodyStart, bodyEnd));
        i = bodyEnd + end.length();
      }
    }

    /** An s.param body as an argument of t(): an expression, a string literal, or a captured block of markup. */
    private String argument(String param, Tag tag, StringBuilder assigns) {
      String trimmed = param.strip();
      if (trimmed.startsWith("${") && Tag.interpolationEnd(trimmed, 0) == trimmed.length()) {
        return trimmed.substring(2, trimmed.length() - 1).strip();
      }
      boolean markup = trimmed.contains("<") || trimmed.contains("[#") || trimmed.contains("[@")
          || trimmed.contains("[/");
      if (!markup) {
        return stringLiteral(trimmed);
      }
      String name = "textArg" + ++captures;
      assigns.append(tag.directive("#assign " + name)).append(trimmed).append(tag.directive("/#assign"));
      return name;
    }

    /** Text with ${...} interpolations as a FreeMarker string literal, or null if no quote character fits. */
    private static String stringLiteral(String text) {
      List<String[]> parts = new ArrayList<>();
      int i = 0;
      while (i < text.length()) {
        int start = text.indexOf("${", i);
        if (start < 0) {
          parts.add(new String[] {"text", text.substring(i)});
          break;
        }
        parts.add(new String[] {"text", text.substring(i, start)});
        int end = Tag.interpolationEnd(text, start);
        if (end < 0) {
          return null;
        }
        parts.add(new String[] {"expr", text.substring(start, end)});
        i = end;
      }
      String exprs = parts.stream().filter(p -> p[0].equals("expr")).map(p -> p[1]).reduce("", String::concat);
      char quote = !exprs.contains("\"") ? '"' : !exprs.contains("'") ? '\'' : 0;
      if (quote == 0) {
        return null;
      }
      StringBuilder literal = new StringBuilder().append(quote);
      for (String[] part : parts) {
        literal.append(part[0].equals("expr") ? part[1]
            : part[1].replace("\\", "\\\\").replace(String.valueOf(quote), "\\" + quote));
      }
      return literal.append(quote).toString();
    }

    // form tags -> the HTML the Struts simple theme wrote

    private String submit(Tag tag) {
      if (tag.body != null || tag.attr("name") == null || tag.attr("type") != null) {
        return null;
      }
      String label = tag.attr("value") != null ? tag.attr("value").html()
          : tag.attr("key") != null ? "${t(" + tag.attr("key").expression() + ")?markup_string}" : null;
      String id = tag.attr("id") != null ? tag.attr("id").html() : idFor(tag.attr("name"));
      String disabled = disabled(tag.attr("disabled"));
      if (id == null || disabled == null) {
        return null;
      }
      Html html = new Html("input").attr("type", "submit").attr("value", label).attr("id", id)
          .attr("name", tag.attr("name").html()).raw(disabled).attr("class", tag.html("cssClass"))
          .attr("style", tag.html("cssStyle")).attr("title", tag.html("title")).attr("tabindex", tag.html("tabindex"));
      return dynamic(tag, html, "name", "key", "value", "id", "cssClass", "cssStyle", "disabled", "type", "title",
          "tabindex");
    }

    private String hidden(Tag tag) {
      Attr name = tag.attr("name");
      if (tag.body != null || name == null || !name.quoted() || name.value.contains("${")) {
        return null;
      }
      String value;
      if (tag.attr("value") != null) {
        value = tag.attr("value").html();
      } else if (name.value.matches("[A-Za-z_][\\w.\\[\\]]*")) {
        value = "${(" + name.value + ")!}";
      } else {
        return null;
      }
      String id = tag.attr("id") != null ? tag.attr("id").html() : idFor(name);
      String disabled = disabled(tag.attr("disabled"));
      if (disabled == null) {
        return null;
      }
      Html html = new Html("input").attr("type", "hidden").attr("name", name.html()).attr("value", value)
          .attr("id", id).attr("class", tag.html("cssClass")).attr("style", tag.html("cssStyle")).raw(disabled);
      return dynamic(tag, html, "name", "id", "value", "cssClass", "cssStyle", "disabled", "required");
    }

    private String file(Tag tag) {
      // like Struts, key stands in for a missing name
      Attr name = tag.attr("name") != null ? tag.attr("name") : tag.attr("key");
      String id = tag.attr("id") != null ? tag.attr("id").html() : name != null ? idFor(name) : "";
      String disabled = disabled(tag.attr("disabled"));
      if (tag.body != null || id == null || disabled == null) {
        return null;
      }
      Html html = new Html("input").attr("type", "file").always("name", name == null ? "" : name.html())
          .attr("size", tag.html("size")).raw(disabled).attr("accept", tag.html("accept"))
          .attr("tabindex", tag.html("tabindex")).attr("id", id).attr("class", tag.html("cssClass"))
          .attr("style", tag.html("cssStyle")).attr("title", tag.html("title"));
      return dynamic(tag, html, "name", "id", "key", "accept", "size", "cssClass", "cssStyle", "disabled", "title",
          "tabindex");
    }

    private static String fieldError(Tag tag) {
      if (tag.body != null || !tag.only("fieldName", "id", "cssClass", "cssStyle")) {
        return null;
      }
      return tag.open + "@fieldError" + tag.source.substring(tag.start + 2 + "s.fielderror".length(), tag.end);
    }

    private static String property(Tag tag) {
      Attr value = tag.attr("value");
      Boolean escape = tag.attr("escapeHtml") == null ? Boolean.TRUE : tag.flag("escapeHtml");
      if (tag.body != null || !tag.only("value", "escapeHtml") || value == null || !value.quoted()
          || escape == null) {
        return null;
      }
      String path = value.value.strip();
      if (path.startsWith("%{") && path.endsWith("}")) {
        path = path.substring(2, path.length() - 1).strip();
      }
      if (!path.matches("[A-Za-z_][\\w.\\[\\]]*")) {
        return null;
      }
      return escape ? "${(" + path + ")!}" : "${((" + path + ")!)?no_esc}";
    }

    /** The markup of an HTML tag with the Struts tag's other attributes (e.g. form) appended, as Struts did. */
    private static String dynamic(Tag tag, Html html, String... known) {
      for (Map.Entry<String, Attr> a : tag.attrs.entrySet()) {
        if (!List.of(known).contains(a.getKey())) {
          html.attr(a.getKey(), a.getValue().html());
        }
      }
      return html.selfClosing();
    }

    /** Struts' id for a field name: every character outside [a-zA-Z0-9_] replaced by '_'; null for dynamic names. */
    private static String idFor(Attr name) {
      if (name == null || !name.quoted() || name.value.contains("${")) {
        return null;
      }
      return name.value.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /** The disabled attribute (possibly inside #if), "" for none, or null if the value is not understood. */
    private static String disabled(Attr disabled) {
      if (disabled == null || disabled.value.equals("false")) {
        return "";
      }
      if (disabled.value.equals("true")) {
        return " disabled=\"disabled\"";
      }
      String condition = disabled.expression();
      if (condition.startsWith("\"") || condition.startsWith("'")) {
        return null;
      }
      condition = condition.endsWith("?string") ? condition.substring(0, condition.length() - 7) : condition;
      return "<#if " + condition + "> disabled=\"disabled\"</#if>";
    }
  }

  /** An HTML start tag built attribute by attribute; empty values are left out, as Struts' simple theme did. */
  static final class Html {

    private final StringBuilder sb;

    Html(String name) {
      sb = new StringBuilder("<").append(name);
    }

    Html attr(String key, String value) {
      return value == null || value.isEmpty() ? this : always(key, value);
    }

    Html always(String key, String value) {
      sb.append(' ').append(key).append("=\"").append(value).append('"');
      return this;
    }

    Html raw(String markup) {
      sb.append(markup);
      return this;
    }

    String selfClosing() {
      return sb + "/>";
    }
  }

  /** An attribute of a Struts tag: a quoted string (which may contain ${...}) or an expression. */
  record Attr(String value, char quote) {

    boolean quoted() {
      return quote != 0;
    }

    /** As a FreeMarker expression: a string literal, the expression inside a lone ${...}, or the expression. */
    String expression() {
      if (!quoted()) {
        return value;
      }
      if (value.startsWith("${") && Tag.interpolationEnd(value, 0) == value.length()) {
        return value.substring(2, value.length() - 1).strip();
      }
      return quote + value + quote;
    }

    /** As the content of a double-quoted HTML attribute. */
    String html() {
      if (!quoted()) {
        return "${" + value + "}";
      }
      return quote == '\'' ? value.replace("\"", "&quot;") : value;
    }
  }

  /** A Struts tag in a template: its attributes and, unless self-closing, its body. */
  static final class Tag {

    final String source;
    final int start;
    int end;
    final char open;
    final char close;
    final String name;
    final Map<String, Attr> attrs = new LinkedHashMap<>();
    String body;
    int bodyStart;
    int bodyEnd;

    private Tag(String source, int start, String name) {
      this.source = source;
      this.start = start;
      this.open = source.charAt(start);
      this.close = open == '<' ? '>' : ']';
      this.name = name;
    }

    /** The tag starting at {@code start} ({@code <@s.name} or {@code [@s.name}), or null if it cannot be parsed. */
    static Tag parse(String s, int start) {
      Matcher m = Pattern.compile("[<\\[]@s\\.(\\w+)").matcher(s);
      if (!m.find(start) || m.start() != start) {
        return null;
      }
      Tag tag = new Tag(s, start, m.group(1));
      int i = m.end();
      while (true) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
          i++;
        }
        if (i >= s.length()) {
          return null;
        }
        if (s.startsWith("/" + tag.close, i)) {
          tag.end = i + 2;
          return tag;
        }
        if (s.charAt(i) == tag.close) {
          return tag.withBody(i + 1);
        }
        Matcher attr = Pattern.compile("([A-Za-z_][\\w-]*)\\s*=\\s*").matcher(s);
        if (!attr.find(i) || attr.start() != i) {
          return null;
        }
        i = attr.end();
        char c = s.charAt(i);
        int valueEnd;
        if (c == '"' || c == '\'') {
          valueEnd = stringEnd(s, i);
          if (valueEnd < 0) {
            return null;
          }
          tag.attrs.put(attr.group(1), new Attr(s.substring(i + 1, valueEnd - 1), c));
        } else {
          valueEnd = expressionEnd(s, i, tag.close);
          if (valueEnd < 0 || valueEnd == i) {
            return null;
          }
          tag.attrs.put(attr.group(1), new Attr(s.substring(i, valueEnd), (char) 0));
        }
        i = valueEnd;
      }
    }

    private Tag withBody(int bodyStart) {
      String opening = open + "@s." + name;
      String closing = open + "/@s." + name + close;
      int depth = 1;
      int i = bodyStart;
      while (depth > 0) {
        int nextOpen = source.indexOf(opening, i);
        int nextClose = source.indexOf(closing, i);
        if (nextClose < 0) {
          return null;
        }
        if (nextOpen >= 0 && nextOpen < nextClose && !Character.isLetterOrDigit(source.charAt(nextOpen + opening.length()))) {
          Tag inner = parse(source, nextOpen);
          if (inner == null) {
            return null;
          }
          i = inner.end;
          continue;
        }
        depth--;
        if (depth == 0) {
          body = source.substring(bodyStart, nextClose);
          this.bodyStart = bodyStart;
          this.bodyEnd = nextClose;
          end = nextClose + closing.length();
          return this;
        }
        i = nextClose + closing.length();
      }
      return null;
    }

    Attr attr(String key) {
      return attrs.get(key);
    }

    /** The attribute as HTML attribute content, or null when absent. */
    String html(String key) {
      return attrs.containsKey(key) ? attrs.get(key).html() : null;
    }

    boolean only(String... keys) {
      return List.of(keys).containsAll(attrs.keySet());
    }

    /** A boolean attribute: false when absent, null when not a literal true or false. */
    Boolean flag(String key) {
      Attr a = attrs.get(key);
      if (a == null || a.value().equals("false")) {
        return Boolean.FALSE;
      }
      return a.value().equals("true") ? Boolean.TRUE : null;
    }

    /** A FreeMarker directive in this tag's syntax, e.g. {@code <#assign x>} or {@code [#assign x]}. */
    String directive(String content) {
      return open + content + close;
    }

    /** The index after a string literal starting at {@code i}, skipping ${...} inside it; -1 if unterminated. */
    static int stringEnd(String s, int i) {
      char quote = s.charAt(i);
      int j = i + 1;
      while (j < s.length()) {
        char c = s.charAt(j);
        if (c == '\\') {
          j += 2;
        } else if (c == quote) {
          return j + 1;
        } else if (s.startsWith("${", j)) {
          j = interpolationEnd(s, j);
          if (j < 0) {
            return -1;
          }
        } else {
          j++;
        }
      }
      return -1;
    }

    /** The index after the ${...} starting at {@code i}, or -1 if unterminated. */
    static int interpolationEnd(String s, int i) {
      int depth = 0;
      int j = i + 1;
      while (j < s.length()) {
        char c = s.charAt(j);
        if (c == '"' || c == '\'') {
          j = stringEnd(s, j);
          if (j < 0) {
            return -1;
          }
          continue;
        }
        if (c == '{') {
          depth++;
        } else if (c == '}' && --depth == 0) {
          return j + 1;
        }
        j++;
      }
      return -1;
    }

    /** The index after an unquoted attribute expression starting at {@code i}. */
    static int expressionEnd(String s, int i, char close) {
      int depth = 0;
      int j = i;
      while (j < s.length()) {
        char c = s.charAt(j);
        if (depth == 0 && (Character.isWhitespace(c) || c == close || (c == '/' && j + 1 < s.length()
            && s.charAt(j + 1) == close))) {
          return j;
        }
        if (c == '"' || c == '\'') {
          j = stringEnd(s, j);
          if (j < 0) {
            return -1;
          }
          continue;
        }
        if (c == '(' || c == '[' || c == '{') {
          depth++;
        } else if (c == ')' || c == ']' || c == '}') {
          depth--;
        }
        j++;
      }
      return -1;
    }
  }
}
