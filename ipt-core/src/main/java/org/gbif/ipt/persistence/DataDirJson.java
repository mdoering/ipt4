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

import org.gbif.api.model.common.DOI;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.UnknownTerm;
import org.gbif.ipt.config.JdbcSupport;
import org.gbif.ipt.model.DataPackageField;
import org.gbif.ipt.model.DataPackageSchema;
import org.gbif.ipt.model.DataPackageTableSchemaName;
import org.gbif.ipt.model.ExcelFileSource;
import org.gbif.ipt.model.Extension;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.Password;
import org.gbif.ipt.model.SourceBase;
import org.gbif.ipt.model.SqlSource;
import org.gbif.ipt.model.TextFileSource;
import org.gbif.ipt.model.UrlSource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.Source;

import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * The JSON mapping of the data dir. Objects are stored by their fields (like XStream did), sorted, without nulls and
 * with ISO dates. Objects managed elsewhere are stored as keys, the way IPT 3's XStream converters stored them:
 * <ul>
 *   <li>users by email, organisations by key, extensions by row type, terms by qualified name,</li>
 *   <li>data package schemas by identifier, tables and fields by name, JDBC drivers by name,</li>
 *   <li>the source of a mapping by source name.</li>
 * </ul>
 * Reading gives stand-ins that {@link ResourceLinker} replaces with the managed objects.
 */
public final class DataDirJson {

  public static final int SCHEMA_VERSION = 1;

  private DataDirJson() {
  }

  /** For {@code resource.json} and {@code inferred-metadata.json}. */
  public static JsonMapper resourceMapper(SecretCipher cipher, JdbcSupport jdbc) {
    SimpleModule refs = new SimpleModule("ipt-references");
    refs.addSerializer(User.class, new KeySerializer<>(User.class, User::getEmail));
    refs.addDeserializer(User.class, new KeyDeserializer<>(User.class, email -> {
      User u = new User();
      u.setEmail(email);
      return u;
    }));
    refs.addSerializer(Organisation.class, new KeySerializer<>(Organisation.class, o -> o.getKey() == null ? null : o.getKey().toString()));
    refs.addDeserializer(Organisation.class, new KeyDeserializer<>(Organisation.class, key -> {
      Organisation o = new Organisation();
      o.setKey(key);
      return o;
    }));
    refs.addSerializer(Extension.class, new KeySerializer<>(Extension.class, Extension::getRowType));
    refs.addDeserializer(Extension.class, new KeyDeserializer<>(Extension.class, rowType -> {
      Extension e = new Extension();
      e.setRowType(rowType);
      return e;
    }));
    refs.addSerializer(Term.class, new KeySerializer<>(Term.class, Term::qualifiedName));
    refs.addDeserializer(Term.class, new KeyDeserializer<>(Term.class, qn -> UnknownTerm.build(qn, qn)));
    refs.addSerializer(DataPackageSchema.class, new KeySerializer<>(DataPackageSchema.class, DataPackageSchema::getIdentifier));
    refs.addDeserializer(DataPackageSchema.class, new KeyDeserializer<>(DataPackageSchema.class,
        id -> DataPackageSchema.builder().identifier(id).build()));
    refs.addSerializer(DataPackageTableSchemaName.class,
        new KeySerializer<>(DataPackageTableSchemaName.class, DataPackageTableSchemaName::getName));
    refs.addDeserializer(DataPackageTableSchemaName.class, new KeyDeserializer<>(DataPackageTableSchemaName.class, name -> {
      DataPackageTableSchemaName n = new DataPackageTableSchemaName();
      n.setName(name);
      return n;
    }));
    refs.addSerializer(DataPackageField.class, new KeySerializer<>(DataPackageField.class, DataPackageField::getName));
    refs.addDeserializer(DataPackageField.class, new KeyDeserializer<>(DataPackageField.class, name -> {
      DataPackageField f = new DataPackageField();
      f.setName(name);
      return f;
    }));
    refs.addSerializer(JdbcSupport.JdbcInfo.class, new KeySerializer<>(JdbcSupport.JdbcInfo.class, JdbcSupport.JdbcInfo::getName));
    refs.addDeserializer(JdbcSupport.JdbcInfo.class, new KeyDeserializer<>(JdbcSupport.JdbcInfo.class, jdbc::get));
    return base(cipher, refs)
        .addMixIn(Source.class, SourceTypes.class)
        .addMixIn(org.gbif.ipt.model.Resource.class, ResourceMixin.class)
        .addMixIn(SourceBase.class, SourceBaseMixin.class)
        .addMixIn(TextFileSource.class, FileSourceMixin.class)
        .addMixIn(ExcelFileSource.class, FileSourceMixin.class)
        .addMixIn(org.gbif.ipt.model.ExtensionMapping.class, MappingSourceMixin.class)
        .addMixIn(org.gbif.ipt.model.DataPackageMapping.class, MappingSourceMixin.class)
        .build();
  }

  /** For {@code users.json} and {@code registration.json}. */
  public static JsonMapper configMapper(SecretCipher cipher) {
    return base(cipher, new SimpleModule("ipt-config")).build();
  }

  private static JsonMapper.Builder base(SecretCipher cipher, SimpleModule module) {
    module.addSerializer(Password.class, new KeySerializer<>(Password.class, p -> cipher.encrypt(p.password)));
    module.addDeserializer(Password.class, new KeyDeserializer<>(Password.class, stored -> {
      Password p = new Password();
      p.password = cipher.decrypt(stored);
      return p;
    }));
    module.addSerializer(DOI.class, new KeySerializer<>(DOI.class, DOI::getDoiName));
    module.addDeserializer(DOI.class, new KeyDeserializer<>(DOI.class, DOI::new));
    return JsonMapper.builder()
        .changeDefaultVisibility(v -> v
            .withVisibility(PropertyAccessor.ALL, Visibility.NONE)
            .withVisibility(PropertyAccessor.FIELD, Visibility.ANY))
        .changeDefaultPropertyInclusion(i -> i.withValueInclusion(JsonInclude.Include.NON_NULL))
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .addModule(module);
  }

  /** Writes an object as a single string key. */
  static final class KeySerializer<T> extends ValueSerializer<T> {
    private final Class<T> type;
    private final Function<T, String> key;

    KeySerializer(Class<T> type, Function<T, String> key) {
      this.type = type;
      this.key = key;
    }

    @Override
    public Class<?> handledType() {
      return type;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializationContext ctxt) {
      String k = key.apply(value);
      if (k == null) {
        gen.writeNull();
      } else {
        gen.writeString(k);
      }
    }

    @Override
    public void serializeWithType(T value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
      serialize(value, gen, ctxt);
    }
  }

  /** Reads a single string key into an object. */
  static final class KeyDeserializer<T> extends ValueDeserializer<T> {
    private final Class<T> type;
    private final Function<String, T> fromKey;

    KeyDeserializer(Class<T> type, Function<String, T> fromKey) {
      this.type = type;
      this.fromKey = fromKey;
    }

    @Override
    public Class<?> handledType() {
      return type;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) {
      String k = p.getValueAsString();
      return k == null ? null : fromKey.apply(k);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) {
      return deserialize(p, ctxt);
    }
  }

  /** A mapping's source, stored by name and replaced by the resource's source of that name after loading. */
  static final class SourceRef extends TextFileSource {
    SourceRef(String name) {
      setName(name);
    }
  }

  static final class SourceRefSerializer extends ValueSerializer<Source> {
    @Override
    public void serialize(Source value, JsonGenerator gen, SerializationContext ctxt) {
      gen.writeString(value.getName());
    }

    @Override
    public void serializeWithType(Source value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
      serialize(value, gen, ctxt);
    }
  }

  static final class SourceRefDeserializer extends ValueDeserializer<Source> {
    @Override
    public Source deserialize(JsonParser p, DeserializationContext ctxt) {
      String name = p.getValueAsString();
      return name == null ? null : new SourceRef(name);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) {
      return deserialize(p, ctxt);
    }
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = TextFileSource.class, name = "text"),
      @JsonSubTypes.Type(value = ExcelFileSource.class, name = "excel"),
      @JsonSubTypes.Type(value = SqlSource.class, name = "sql"),
      @JsonSubTypes.Type(value = UrlSource.class, name = "url")
  })
  interface SourceTypes {
  }

  abstract static class ResourceMixin {
    @JsonIgnore
    String shortname;
    @JsonIgnore
    Object eml;
    @JsonIgnore
    Object dataPackageMetadata;
    @JsonIgnore
    Object inferredMetadata;
  }

  abstract static class SourceBaseMixin {
    @JsonIgnore
    Object resource;
  }

  /** The data file of file sources is derived from the data dir on load. */
  abstract static class FileSourceMixin {
    @JsonIgnore
    Object file;
  }

  abstract static class MappingSourceMixin {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    @JsonSerialize(using = SourceRefSerializer.class)
    @JsonDeserialize(using = SourceRefDeserializer.class)
    Source source;
  }
}
