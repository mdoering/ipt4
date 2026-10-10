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
package org.gbif.ipt.job;

import java.time.Instant;
import java.util.concurrent.Future;

/**
 * A background job: what it works on ({@code key}, e.g. a resource shortname), what kind of job it is and its result.
 */
public record JobHandle<T>(String key, String type, Instant submitted, Future<T> future) {

  public boolean isDone() {
    return future.isDone();
  }
}
