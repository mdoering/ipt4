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
package org.gbif.ipt.web.support;

import java.util.TimeZone;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

/** IPT 3's Struts JSON plugin results: bean properties in name order, {@code noCache}, optionally without nulls. */
public final class StrutsJson {

  private static final MediaType JSON_UTF8 = MediaType.parseMediaType("application/json;charset=UTF-8");

  private static final JsonMapper WITH_NULLS = JsonMapper.builder()
      .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
      .defaultTimeZone(TimeZone.getDefault())
      .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.ALWAYS))
      .build();

  private static final JsonMapper WITHOUT_NULLS = WITH_NULLS.rebuild()
      .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
      .build();

  private StrutsJson() {
  }

  public static ResponseEntity<byte[]> json(Object root) {
    return json(root, false);
  }

  /** @param excludeNulls Struts' {@code excludeNullProperties} */
  public static ResponseEntity<byte[]> json(Object root, boolean excludeNulls) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(JSON_UTF8);
    // Struts' noCache
    headers.setCacheControl("no-cache");
    headers.setPragma("No-cache");
    headers.setExpires(0);
    byte[] body = (excludeNulls ? WITHOUT_NULLS : WITH_NULLS).writeValueAsBytes(root);
    return new ResponseEntity<>(body, headers, HttpStatus.OK);
  }
}
