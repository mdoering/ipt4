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
package org.gbif.ipt.persistence;

import org.gbif.ipt.config.JdbcSupport;
import org.gbif.ipt.model.DataPackageSchema;
import org.gbif.ipt.model.Extension;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.DataPackageSchemaManager;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.admin.impl.ExtensionsHolder;
import org.gbif.ipt.service.manage.ResourcePersister;

/** Builds the JSON resource persister on top of (mocked) managers, as the ported IPT 3 tests did with XStream. */
public final class TestPersisters {

  public static final SecretCipher CIPHER = new SecretCipher(new byte[32]);

  private TestPersisters() {
  }

  public static ResourcePersister json(UserAccountManager users, RegistrationManager organisations,
                                       ExtensionsHolder extensions, DataPackageSchemaManager schemas,
                                       JdbcSupport jdbc) {
    return new JsonResourcePersister(CIPHER, jdbc, new ReferenceLookup() {
      @Override
      public User user(String email) {
        return users.get(email);
      }

      @Override
      public Organisation organisation(String key) {
        return organisations.get(key);
      }

      @Override
      public Extension extension(String rowType) {
        return extensions.getExtensionsByRowtype() == null ? null : extensions.getExtensionsByRowtype().get(rowType);
      }

      @Override
      public DataPackageSchema dataPackageSchema(String identifier) {
        return schemas == null ? null : schemas.get(identifier);
      }
    });
  }
}
