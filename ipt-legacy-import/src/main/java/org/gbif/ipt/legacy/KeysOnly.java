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
package org.gbif.ipt.legacy;

import org.gbif.ipt.model.DataPackageSchema;
import org.gbif.ipt.model.Extension;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.User;
import org.gbif.ipt.persistence.ReferenceLookup;

import java.util.LinkedHashSet;

/** Resolves references to stand-ins holding only the key: enough to read back what the importer wrote. */
final class KeysOnly implements ReferenceLookup {

  @Override
  public User user(String email) {
    User u = new User();
    u.setEmail(email);
    return u;
  }

  @Override
  public Organisation organisation(String key) {
    Organisation o = new Organisation();
    o.setKey(key);
    return o;
  }

  @Override
  public Extension extension(String rowType) {
    Extension e = new Extension();
    e.setRowType(rowType);
    return e;
  }

  @Override
  public DataPackageSchema dataPackageSchema(String identifier) {
    return DataPackageSchema.builder().identifier(identifier).tableSchemas(new LinkedHashSet<>()).build();
  }
}
