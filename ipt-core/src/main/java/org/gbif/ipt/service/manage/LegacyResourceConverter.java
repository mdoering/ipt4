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
package org.gbif.ipt.service.manage;

import java.io.File;
import java.io.IOException;

/**
 * Converts, in place, a resource folder written by IPT 3 ({@code resource.xml}) into IPT4's format, so resource
 * folders exported from IPT 3 installations can still be imported. Provided by ipt-legacy-import.
 */
public interface LegacyResourceConverter {

  LegacyResourceConverter NONE = dir -> false;

  /**
   * @return true if the folder held an IPT 3 {@code resource.xml} and was converted
   */
  boolean convert(File resourceDir) throws IOException;
}
