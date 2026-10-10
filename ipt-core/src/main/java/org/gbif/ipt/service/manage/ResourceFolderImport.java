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
import java.util.List;

/**
 * Makes an uploaded resource folder (copied from another IPT's data dir) loadable here: converts IPT 3's
 * {@code resource.xml}, and drops stored secrets this installation's key cannot read. Provided by ipt-legacy-import.
 */
public interface ResourceFolderImport {

  ResourceFolderImport NONE = dir -> List.of();

  /**
   * @return the names of the sources whose stored password was dropped and must be entered again
   */
  List<String> prepare(File resourceDir) throws IOException;
}
