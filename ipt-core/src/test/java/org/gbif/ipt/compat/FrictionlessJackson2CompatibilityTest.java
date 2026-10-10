package org.gbif.ipt.compat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.frictionlessdata.tableschema.exception.InvalidCastException;
import io.frictionlessdata.tableschema.exception.TypeInferringException;
import io.frictionlessdata.tableschema.field.Field;
import io.frictionlessdata.tableschema.schema.Schema;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

/** Guards the Jackson 2 version used by the GBIF fork of tableschema-java under the Spring Boot BOM. */
class FrictionlessJackson2CompatibilityTest {

  private static final String SCHEMA =
      "{\"fields\":[{\"name\":\"id\",\"type\":\"integer\"},{\"name\":\"scientificName\",\"type\":\"string\"}]}";

  @Test
  void parsesValidatesAndWritesSchema() throws Exception {
    Schema schema = Schema.fromJson(SCHEMA, true);

    assertThat(schema.getFieldNames()).containsExactly("id", "scientificName");
    assertThat(schema.asJson()).contains("scientificName");
  }

  @Test
  void castsValuesThroughJackson2() throws Exception {
    Field<?> id = Schema.fromJson(SCHEMA, true).getField("id");

    assertThat(id.castValue("42")).isEqualTo(BigInteger.valueOf(42));
    assertThatThrownBy(() -> id.castValue("not-a-number"))
        .isInstanceOfAny(InvalidCastException.class, TypeInferringException.class);
  }

  @Test
  void jackson2ModulesConverge() {
    String databind = com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION.toString();

    assertThat(com.fasterxml.jackson.core.json.PackageVersion.VERSION.toString()).isEqualTo(databind);
    assertThat(com.fasterxml.jackson.dataformat.csv.PackageVersion.VERSION.toString()).isEqualTo(databind);
    assertThat(databind).startsWith("2.");
  }
}
