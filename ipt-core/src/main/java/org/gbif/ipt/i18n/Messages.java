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

import java.util.Locale;
import java.util.ResourceBundle;

/** Localised UI texts from the {@code ApplicationResources} bundles. */
public interface Messages {

  /** The text for {@code key} formatted with {@code args}, or the key itself if no bundle has it. */
  String get(Locale locale, String key, Object... args);

  /** The text for {@code key} formatted with {@code args}, or {@code defaultValue} (formatted) if no bundle has it. */
  String getOrDefault(Locale locale, String key, String defaultValue, Object... args);

  /** The bundle for {@code locale}, falling back to English. */
  ResourceBundle bundle(Locale locale);
}
