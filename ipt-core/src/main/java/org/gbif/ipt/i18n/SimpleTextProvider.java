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

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.annotation.Nullable;

/**
 * The IPT 3 text provider API, kept so ported code compiles unchanged, backed by {@link BundleMessages}.
 * Unlike IPT 3, texts are formatted once (see {@link BundleMessages}).
 */
public class SimpleTextProvider {

  private final Messages messages;

  public SimpleTextProvider() {
    this(new BundleMessages());
  }

  public SimpleTextProvider(Messages messages) {
    this.messages = messages;
  }

  public String getText(@Nullable Locale locale, String key, @Nullable String defaultValue, @Nullable List<?> args) {
    return getText(locale, key, defaultValue, args == null ? null : args.toArray());
  }

  public String getText(@Nullable Locale locale, String key, @Nullable String defaultValue, @Nullable Object[] args) {
    return messages.getOrDefault(locale == null ? Locale.ENGLISH : locale, key, defaultValue,
        args == null ? new Object[0] : args);
  }

  public ResourceBundle getTexts(@Nullable Locale locale) {
    return messages.bundle(locale == null ? Locale.ENGLISH : locale);
  }

  public ResourceBundle getTexts(String bundleName, @Nullable Locale locale) {
    return new BundleMessages(bundleName).bundle(locale == null ? Locale.ENGLISH : locale);
  }
}
