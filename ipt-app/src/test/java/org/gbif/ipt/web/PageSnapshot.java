/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.ipt.web;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.TextNode;

/**
 * Pages as normalised HTML, so that two renderings can be compared with {@code diff}: attributes sorted, entities
 * written one way, CSRF tokens and timestamps masked. Used to show that a template refactoring changes no page.
 */
final class PageSnapshot {

  /** Dates with times, clock times, temporary directories, free disk space, and CSRF tokens inside scripts. */
  private static final Pattern VOLATILE = Pattern.compile(
      "\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?|\\b\\d{1,2}:\\d{2}:\\d{2}\\b|\\d{10,}|\\b\\d+(\\.\\d+)? [KMGT]B\\b"
          + "|(?<=name=\"_csrf\" value=\")[^\"]+|(?<=\"_csrf\", \")[^\"]+");
  private static final String MASK = "#masked#";

  private PageSnapshot() {}

  static String normalise(String html) {
    Document doc = Jsoup.parse(html);
    doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml).charset("UTF-8").prettyPrint(true).indentAmount(1);
    for (Element e : doc.select("meta[name=_csrf]")) {
      e.attr("content", MASK);
    }
    for (Element e : doc.select("input[name=_csrf]")) {
      e.attr("value", MASK);
    }
    for (Element e : doc.getAllElements()) {
      List<Attribute> attributes = new ArrayList<>(e.attributes().asList());
      attributes.sort(Comparator.comparing(Attribute::getKey));
      attributes.forEach(a -> e.removeAttr(a.getKey()));
      attributes.forEach(a -> e.attr(a.getKey(), VOLATILE.matcher(a.getValue()).replaceAll(MASK)));
      for (TextNode t : e.textNodes()) {
        t.text(VOLATILE.matcher(t.getWholeText()).replaceAll(MASK));
      }
      for (DataNode d : e.dataNodes()) {
        d.setWholeData(VOLATILE.matcher(d.getWholeData()).replaceAll(MASK));
      }
    }
    return doc.outerHtml();
  }

  /** A file name for the page at this path and query. */
  static String fileName(String url) {
    String name = url.replaceFirst("^/", "").replaceAll("[^A-Za-z0-9._=-]", "_");
    return (name.isEmpty() ? "_" : name) + ".html";
  }
}
