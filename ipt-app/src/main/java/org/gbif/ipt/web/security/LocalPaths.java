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
package org.gbif.ipt.web.security;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/** Redirect targets within this IPT: a path with exactly one leading slash, never {@code //host} or {@code /\host}. */
public final class LocalPaths {

  private static final Pattern CONTROL_OR_SPACE = Pattern.compile("[\\p{Cntrl}\\s]");

  private LocalPaths() {
  }

  public static String of(String path) {
    // browsers drop tabs and line breaks from URLs, which would turn "/\t/host" into "//host"
    String cleaned = path == null ? "" : CONTROL_OR_SPACE.matcher(path).replaceAll("");
    return "/" + StringUtils.stripStart(cleaned, "/\\");
  }
}
