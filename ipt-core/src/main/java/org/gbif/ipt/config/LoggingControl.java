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
package org.gbif.ipt.config;

import java.io.File;

/**
 * Re-points logging at the data dir and switches debug logging, when the IPT configuration changes.
 * IPT 3 rebuilt its Log4j configuration directly; IPT4's web application supplies the implementation.
 */
public interface LoggingControl {

  LoggingControl NONE = (logDir, debug) -> { };

  void apply(File logDir, boolean debug);
}
