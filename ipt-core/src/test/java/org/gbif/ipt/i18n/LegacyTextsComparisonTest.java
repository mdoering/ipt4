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
package org.gbif.ipt.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every text of every bundle comes out as in IPT 3, except where IPT 3's second {@link MessageFormat} pass ate
 * apostrophes (French n''a became "na"). IPT 3's algorithm is reproduced here from {@code struts2/SimpleTextProvider}.
 */
class LegacyTextsComparisonTest {

  private final SimpleTextProvider texts = new SimpleTextProvider();

  private static String ipt3(String pattern) {
    String once;
    try {
      once = MessageFormat.format(pattern, new Object[0]);
    } catch (IllegalArgumentException e) {
      once = pattern;
    }
    try {
      return MessageFormat.format(once, new Object[0]);
    } catch (IllegalArgumentException e) {
      return once;
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"en", "es", "fa", "fr", "ja", "pt", "ru", "tr", "zh"})
  void everyKeyMatchesIpt3ExceptLostApostrophes(String lang) throws IOException {
    Properties bundle = new Properties();
    try (InputStream in = getClass().getResourceAsStream("/ApplicationResources_" + lang + ".properties")) {
      bundle.load(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
    List<String> different = new ArrayList<>();
    int apostrophesKept = 0;
    for (String key : bundle.stringPropertyNames()) {
      String legacy = ipt3(bundle.getProperty(key));
      String now = texts.getText(Locale.forLanguageTag(lang), key, null, new Object[0]);
      if (now.equals(legacy)) {
        continue;
      }
      if (now.replace("'", "").equals(legacy.replace("'", ""))) {
        apostrophesKept++;
      } else {
        different.add(key + ": IPT 3 [" + legacy + "] IPT4 [" + now + "]");
      }
    }
    assertThat(different).as(lang).isEmpty();
    if ("fr".equals(lang)) {
      assertThat(apostrophesKept).as("French texts whose apostrophes IPT 3 lost").isPositive();
    }
  }
}
