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
package org.gbif.ipt.legacy.v3.xstream;

import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.UnknownTerm;
import org.gbif.ipt.config.JdbcSupport;
import org.gbif.ipt.legacy.v3.PBEEncrypt;
import org.gbif.ipt.model.DataPackageField;
import org.gbif.ipt.model.DataPackageSchema;
import org.gbif.ipt.model.DataPackageTableSchemaName;
import org.gbif.ipt.model.Extension;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.Password;
import org.gbif.ipt.model.User;

import java.util.function.Consumer;
import java.util.function.Function;

import com.thoughtworks.xstream.converters.SingleValueConverter;

/**
 * IPT 3 stored references to managed objects (users, organisations, extensions, ...) as keys through XStream
 * converters that looked the objects up. The importer reads them into stand-ins holding just the key, which is all
 * the IPT4 JSON stores.
 */
public final class StubConverters {

  private StubConverters() {
  }

  /** Reads and writes a value of {@code type} as its key. */
  public static final class Stub<T> implements SingleValueConverter {
    private final Class<T> type;
    private final boolean subtypes;
    private final Function<T, String> toKey;
    private final Function<String, T> fromKey;

    public Stub(Class<T> type, boolean subtypes, Function<T, String> toKey, Function<String, T> fromKey) {
      this.type = type;
      this.subtypes = subtypes;
      this.toKey = toKey;
      this.fromKey = fromKey;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean canConvert(Class clazz) {
      return clazz != null && (subtypes ? type.isAssignableFrom(clazz) : type.equals(clazz));
    }

    @Override
    public String toString(Object obj) {
      return obj == null ? null : toKey.apply(type.cast(obj));
    }

    @Override
    public Object fromString(String str) {
      return str == null ? null : fromKey.apply(str);
    }
  }

  public static Stub<User> users() {
    return new Stub<>(User.class, false, User::getEmail, email -> {
      User u = new User();
      u.setEmail(email);
      return u;
    });
  }

  public static Stub<Organisation> organisations() {
    return new Stub<>(Organisation.class, false, o -> String.valueOf(o.getKey()), key -> {
      Organisation o = new Organisation();
      o.setKey(key);
      return o;
    });
  }

  public static Stub<Extension> extensions() {
    return new Stub<>(Extension.class, false, Extension::getRowType, rowType -> {
      Extension e = new Extension();
      e.setRowType(rowType);
      return e;
    });
  }

  public static Stub<Term> terms() {
    return new Stub<>(Term.class, true, Term::qualifiedName, qn -> UnknownTerm.build(qn, qn));
  }

  public static Stub<DataPackageSchema> schemas() {
    return new Stub<>(DataPackageSchema.class, false, DataPackageSchema::getIdentifier,
        id -> DataPackageSchema.builder().identifier(id).build());
  }

  public static Stub<DataPackageTableSchemaName> tableNames() {
    return new Stub<>(DataPackageTableSchemaName.class, false, DataPackageTableSchemaName::getName, name -> {
      DataPackageTableSchemaName n = new DataPackageTableSchemaName();
      n.setName(name);
      return n;
    });
  }

  public static Stub<DataPackageField> fields() {
    return new Stub<>(DataPackageField.class, false, DataPackageField::getName, name -> {
      DataPackageField f = new DataPackageField();
      f.setName(name);
      return f;
    });
  }

  public static Stub<JdbcSupport.JdbcInfo> jdbc(JdbcSupport jdbc) {
    return new Stub<>(JdbcSupport.JdbcInfo.class, false, JdbcSupport.JdbcInfo::getName, jdbc::get);
  }

  /** Secrets IPT 3 encrypted with its built-in PBE key; read back in clear. Failures are reported, not thrown. */
  public static Stub<Password> pbePasswords(PBEEncrypt pbe, Consumer<String> problems) {
    return new Stub<>(Password.class, false, p -> {
      throw new UnsupportedOperationException("The importer only reads IPT 3 files");
    }, stored -> {
      Password p = new Password();
      try {
        p.password = pbe.decrypt(stored);
      } catch (PBEEncrypt.EncryptionException e) {
        problems.accept("A stored password could not be decrypted and was dropped");
      }
      return p;
    });
  }
}
