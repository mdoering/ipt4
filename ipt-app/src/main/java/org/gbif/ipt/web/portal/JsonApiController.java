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
package org.gbif.ipt.web.portal;

import org.gbif.ipt.model.datatable.DatatableResult;
import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.web.support.action.ActionRunner;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * IPT 3's Struts JSON plugin results: bean properties in name order, {@code noCache}, and for inventory v2
 * {@code excludeNullProperties}. The JSON roots are spelled out here, as IPT 3 named them with {@code @JSON}.
 */
@RestController
public class JsonApiController {

  private static final MediaType JSON_UTF8 = MediaType.parseMediaType("application/json;charset=UTF-8");

  private static final JsonMapper STRUTS_JSON = JsonMapper.builder()
      .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
      .defaultTimeZone(TimeZone.getDefault())
      .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.ALWAYS))
      .build();

  private static final JsonMapper STRUTS_JSON_NO_NULLS = STRUTS_JSON.rebuild()
      .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
      .build();

  private final ActionRunner runner;
  private final ObjectProvider<InventoryPage> inventoryPages;
  private final ObjectProvider<InventoryV2Page> inventoryV2Pages;
  private final ObjectProvider<HomePage> homePages;
  private final ObjectProvider<HealthPage> healthPages;

  public JsonApiController(ActionRunner runner, ObjectProvider<InventoryPage> inventoryPages,
                           ObjectProvider<InventoryV2Page> inventoryV2Pages, ObjectProvider<HomePage> homePages,
                           ObjectProvider<HealthPage> healthPages) {
    this.runner = runner;
    this.inventoryPages = inventoryPages;
    this.inventoryV2Pages = inventoryV2Pages;
    this.homePages = homePages;
    this.healthPages = healthPages;
  }

  @GetMapping("/inventory/dataset")
  public ResponseEntity<byte[]> inventory() {
    InventoryPage page = inventoryPages.getObject();
    page.execute();
    return json(STRUTS_JSON, Map.of("registeredResources", page.getInventory()));
  }

  @GetMapping("/inventory/v2/dataset")
  public ResponseEntity<byte[]> inventoryV2(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String type) {
    InventoryV2Page page = inventoryV2Pages.getObject();
    page.setStatus(status);
    page.setType(type);
    page.execute();
    return json(STRUTS_JSON_NO_NULLS, Map.of("resources", page.getInventory()));
  }

  /** Paging parameters IPT 3 parsed with {@code Integer.parseInt}; garbage there is a bad request, not a 500. */
  @GetMapping("/api/resources")
  public ResponseEntity<byte[]> resources(HttpServletRequest request) throws Exception {
    HomePage page = homePages.getObject();
    String result;
    try {
      result = runner.run(page, request, "execute", page::execute);
    } catch (NumberFormatException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
    if (!ActionPage.SUCCESS.equals(result)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
    DatatableResult table = page.getResources();
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("aaData", table.getData());
    root.put("iTotalDisplayRecords", table.getTotalDisplayRecords());
    root.put("iTotalRecords", table.getTotalRecords());
    return json(STRUTS_JSON, root);
  }

  @GetMapping("/api/health")
  public ResponseEntity<byte[]> health(HttpServletRequest request) throws Exception {
    HealthPage page = healthPages.getObject();
    runner.run(page, request, "execute", page::execute);
    return json(STRUTS_JSON, Map.of("status", page.getStatus()));
  }

  private static ResponseEntity<byte[]> json(JsonMapper mapper, Object root) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(JSON_UTF8);
    // Struts' noCache
    headers.setCacheControl("no-cache");
    headers.setPragma("No-cache");
    headers.setExpires(0);
    return new ResponseEntity<>(mapper.writeValueAsBytes(root), headers, HttpStatus.OK);
  }
}
