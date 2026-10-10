import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Converts IPT 3 (Struts) FreeMarker templates for IPT4. Re-runnable and idempotent, so templates changed upstream can
 * be converted again. See docs/design.md, section 5.
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
}
