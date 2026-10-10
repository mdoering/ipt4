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
package org.gbif.ipt.utils;

import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON binding on fields, the way IPT 3 used Gson for registry responses and cached schema files: getters and setters
 * are ignored, nulls are left out, unknown properties are skipped and dates are {@code yyyy-MM-dd}. Parsing is lenient
 * like Gson's (unquoted names, single quotes, comments, trailing commas).
 * Jackson exceptions ({@link tools.jackson.core.JacksonException}) are unchecked, like Gson's.
 */
public final class FieldJson {

  private final JsonMapper mapper;

  public FieldJson() {
    this(false);
  }

  public FieldJson(boolean pretty) {
    SimpleDateFormat dates = new SimpleDateFormat("yyyy-MM-dd");
    dates.setTimeZone(TimeZone.getDefault());
    JsonMapper.Builder b = JsonMapper.builder()
        .changeDefaultVisibility(v -> v
            .withVisibility(PropertyAccessor.ALL, Visibility.NONE)
            .withVisibility(PropertyAccessor.FIELD, Visibility.ANY)
            .withVisibility(PropertyAccessor.CREATOR, Visibility.ANY))
        .changeDefaultPropertyInclusion(i -> i.withValueInclusion(JsonInclude.Include.NON_NULL))
        .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES, JsonReadFeature.ALLOW_SINGLE_QUOTES,
            JsonReadFeature.ALLOW_JAVA_COMMENTS, JsonReadFeature.ALLOW_TRAILING_COMMA)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .defaultDateFormat(dates);
    if (pretty) {
      b.enable(SerializationFeature.INDENT_OUTPUT);
    }
    this.mapper = b.build();
  }

  /** Like Gson, null or blank JSON gives null. */
  @Nullable
  public <T> T fromJson(@Nullable String json, Class<T> type) {
    return json == null || json.isBlank() ? null : mapper.readValue(json, type);
  }

  @Nullable
  public <T> T fromJson(@Nullable String json, TypeReference<T> type) {
    return json == null || json.isBlank() ? null : mapper.readValue(json, type);
  }

  public <T> T fromJson(Reader json, Class<T> type) {
    return mapper.readValue(json, type);
  }

  public String toJson(Object value) {
    return mapper.writeValueAsString(value);
  }

  public void toJson(Object value, Writer out) {
    mapper.writeValue(out, value);
  }
}
