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
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.UnknownTerm;
import org.gbif.ipt.config.JdbcSupport;
import org.gbif.ipt.config.TestBeanProvider;
import org.gbif.ipt.model.DataPackageField;
import org.gbif.ipt.model.DataPackageFieldMapping;
import org.gbif.ipt.model.DataPackageMapping;
import org.gbif.ipt.model.DataPackageSchema;
import org.gbif.ipt.model.DataPackageTableSchema;
import org.gbif.ipt.model.DataPackageTableSchemaName;
import org.gbif.ipt.model.ExcelFileSource;
import org.gbif.ipt.model.Extension;
import org.gbif.ipt.model.ExtensionMapping;
import org.gbif.ipt.model.ExtensionProperty;
import org.gbif.ipt.model.InferredEmlMetadata;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.PropertyMapping;
import org.gbif.ipt.model.RecordFilter;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.SqlSource;
import org.gbif.ipt.model.TextFileSource;
import org.gbif.ipt.model.UrlSource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.VersionHistory;
import org.gbif.ipt.model.voc.IdentifierStatus;
import org.gbif.ipt.model.voc.PublicationStatus;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceJsonTest {

  static final String ORG_KEY = "f3e8e9a9-df60-40ca-bb71-7f49313b3150";
  static final String OCCURRENCE = "http://rs.tdwg.org/dwc/terms/Occurrence";
  static final String SCHEMA_ID = "http://rs.gbif.org/sandbox/data-packages/dwc-dp/0.1";

  final User creator = user("kbraak@gbif.org");
  final User manager = user("jcuadra@gbif.org");
  final Organisation org = organisation();
  final Extension occurrence = occurrenceExtension();
  final DataPackageSchema schema = schema();
  final JdbcSupport jdbc = TestBeanProvider.provideJdbcSupport();
  final SecretCipher cipher = new SecretCipher(new byte[32]);

  final ReferenceLookup lookup = new ReferenceLookup() {
    @Override
    public User user(String email) {
      return Map.of(creator.getEmail(), creator, manager.getEmail(), manager).get(email);
    }

    @Override
    public Organisation organisation(String key) {
      return ORG_KEY.equals(key) ? org : null;
    }

    @Override
    public Extension extension(String rowType) {
      return OCCURRENCE.equals(rowType) ? occurrence : null;
    }

    @Override
    public DataPackageSchema dataPackageSchema(String identifier) {
      return SCHEMA_ID.equals(identifier) ? schema : null;
    }
  };

  final JsonResourcePersister persister = new JsonResourcePersister(cipher, jdbc, lookup);

  static User user(String email) {
    User u = new User();
    u.setEmail(email);
    u.setRole(User.Role.Manager);
    return u;
  }

  static Organisation organisation() {
    Organisation o = new Organisation();
    o.setKey(ORG_KEY);
    o.setName("GBIF");
    return o;
  }

  static Extension occurrenceExtension() {
    Extension e = new Extension();
    e.setRowType(OCCURRENCE);
    e.setTitle("Darwin Core Occurrence");
    for (DwcTerm t : List.of(DwcTerm.occurrenceID, DwcTerm.scientificName, DwcTerm.basisOfRecord)) {
      ExtensionProperty p = new ExtensionProperty(t.qualifiedName());
      e.addProperty(p);
    }
    return e;
  }

  static DataPackageSchema schema() {
    DataPackageSchema s = DataPackageSchema.builder()
        .identifier(SCHEMA_ID)
        .name("dwc-dp")
        .tableSchemas(new java.util.LinkedHashSet<>())
        .build();
    DataPackageTableSchema table = new DataPackageTableSchema();
    table.setName("event");
    table.setFields(new java.util.ArrayList<>());
    DataPackageField id = new DataPackageField();
    id.setName("eventID");
    table.getFields().add(id);
    s.getTableSchemas().add(table);
    return s;
  }

  Resource resource() throws Exception {
    Resource r = new Resource();
    r.setShortname("res1");
    r.setCoreType("Occurrence");
    r.setStatus(PublicationStatus.REGISTERED);
    r.setEmlVersion(new BigDecimal("3.0"));
    r.setLastPublished(new Date(1345123516892L));
    r.setRecordsPublished(2);
    r.setKey(UUID.fromString("54b58d1a-a982-445a-b4c2-5f4e49c899be"));
    r.setOrganisation(org);
    r.setCreator(creator);
    r.setCreated(new Date(1344965509546L));
    r.setModifier(creator);
    r.addManager(manager);
    r.setIdentifierStatus(IdentifierStatus.PUBLIC);
    r.setDoi(new DOI("10.15468/abcd"));
    r.setRecordsByExtension(new HashMap<>(Map.of(OCCURRENCE, 2, "http://x/y", 5)));

    TextFileSource text = new TextFileSource();
    text.setName("occurrence");
    text.setColumns(18);
    text.setReadable(true);
    text.setFieldsTerminatedBy("\t");
    text.setIgnoreHeaderLines(1);
    text.setFile(new File("/data/resources/res1/sources/occurrence.txt"));
    r.addSource(text, false);

    ExcelFileSource excel = new ExcelFileSource();
    excel.setName("sheet");
    excel.setSheetIdx(2);
    r.addSource(excel, false);

    SqlSource sql = new SqlSource();
    sql.setName("db");
    sql.setRdbms(jdbc.get("pgsql"));
    sql.setHost("localhost");
    sql.setDatabase("ipt");
    sql.setUsername("ipt");
    sql.setPassword("p4ssw0rd");
    sql.setSql("select * from occurrence");
    r.addSource(sql, false);

    UrlSource url = new UrlSource();
    url.setName("remote");
    url.setUrl(URI.create("https://example.org/occurrence.txt"));
    r.addSource(url, false);

    ExtensionMapping mapping = new ExtensionMapping();
    mapping.setSource(text);
    mapping.setExtension(occurrence);
    Set<PropertyMapping> fields = new TreeSet<>();
    PropertyMapping id = new PropertyMapping();
    id.setTerm(occurrence.getProperty(DwcTerm.occurrenceID.qualifiedName()));
    id.setIndex(0);
    fields.add(id);
    PropertyMapping bor = new PropertyMapping();
    bor.setTerm(occurrence.getProperty(DwcTerm.basisOfRecord.qualifiedName()));
    bor.setDefaultValue("PreservedSpecimen");
    bor.setTranslation(new HashMap<>(Map.of("PS", "PreservedSpecimen")));
    fields.add(bor);
    PropertyMapping unknown = new PropertyMapping();
    unknown.setTerm(UnknownTerm.build("http://example.org/custom"));
    unknown.setIndex(5);
    fields.add(unknown);
    mapping.setFields(fields);
    mapping.setIdColumn(0);
    RecordFilter filter = new RecordFilter();
    filter.setColumn(3);
    filter.setParam("x");
    filter.setComparator(RecordFilter.Comparator.Equals);
    mapping.setFilter(filter);
    r.addMapping(mapping);

    DataPackageMapping dpMapping = new DataPackageMapping();
    dpMapping.setSource(sql);
    dpMapping.setDataPackageSchema(schema);
    DataPackageTableSchemaName table = new DataPackageTableSchemaName();
    table.setName("event");
    dpMapping.setDataPackageTableSchemaName(table);
    DataPackageFieldMapping fm = new DataPackageFieldMapping();
    fm.setIndex(1);
    fm.setField(schema.getTableSchemas().iterator().next().getFields().get(0));
    dpMapping.getFields().add(fm);
    r.setDataPackageIdentifier(SCHEMA_ID);
    r.addDataPackageMapping(dpMapping);

    VersionHistory vh = new VersionHistory(new BigDecimal("3.0"), new Date(1345123516892L), PublicationStatus.REGISTERED);
    vh.setModifiedBy(creator);
    vh.setDoi(new DOI("10.15468/abcd"));
    vh.setStatus(IdentifierStatus.PUBLIC);
    r.addVersionHistory(vh);
    return r;
  }

  private String save(Resource r) {
    StringWriter w = new StringWriter();
    persister.save(r, w);
    return w.toString();
  }

  private Resource load(String json) {
    return persister.load(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
  }

  private static String maskSecrets(String json) {
    return json.replaceAll("\\{aesgcm\\}[^\"]+", "{aesgcm}...");
  }

  @Test
  void roundTripIsStable() throws Exception {
    String json = save(resource());
    Resource loaded = load(json);
    // secrets get a fresh IV on every save
    assertThat(maskSecrets(save(loaded))).isEqualTo(maskSecrets(json));
  }

  @Test
  void referencesAreStoredAsKeysAndRelinked() throws Exception {
    String json = save(resource());
    assertThat(json)
        .contains("\"creator\" : \"kbraak@gbif.org\"")
        .contains("\"organisation\" : \"" + ORG_KEY + "\"")
        .contains("\"extension\" : \"" + OCCURRENCE + "\"")
        .contains("\"term\" : \"http://rs.tdwg.org/dwc/terms/basisOfRecord\"")
        .contains("\"dataPackageSchema\" : \"" + SCHEMA_ID + "\"")
        .contains("\"rdbms\" : \"pgsql\"")
        .contains("\"doi\" : \"10.15468/abcd\"");

    Resource loaded = load(json);
    assertThat(loaded.getCreator()).isSameAs(creator);
    assertThat(loaded.getModifier()).isSameAs(creator);
    assertThat(loaded.getManagers()).containsExactly(manager);
    assertThat(loaded.getOrganisation()).isSameAs(org);
    assertThat(loaded.getVersionHistory().get(0).getModifiedBy()).isSameAs(creator);
    assertThat(loaded.getDoi()).isEqualTo(new DOI("10.15468/abcd"));

    ExtensionMapping mapping = loaded.getMappings().get(0);
    assertThat(mapping.getExtension()).isSameAs(occurrence);
    assertThat(mapping.getSource()).isSameAs(loaded.getSource("occurrence"));
    assertThat(mapping.getField(DwcTerm.basisOfRecord.qualifiedName()).getTerm())
        .isSameAs(occurrence.getProperty(DwcTerm.basisOfRecord.qualifiedName()));
    assertThat(mapping.getField("http://example.org/custom").getTerm()).isInstanceOf(UnknownTerm.class);
    assertThat(mapping.getFilter().getColumn()).isEqualTo(3);

    DataPackageMapping dp = loaded.getAllDataPackageMappings().get(0);
    assertThat(dp.getSource()).isSameAs(loaded.getSource("db"));
    assertThat(dp.getDataPackageSchema()).isSameAs(schema);
    assertThat(dp.getDataPackageTableSchemaName().getName()).isEqualTo("event");
    assertThat(dp.getFields().get(0).getField()).isSameAs(schema.getTableSchemas().iterator().next().getFields().get(0));

    for (var src : loaded.getSources()) {
      assertThat(src.getResource()).isSameAs(loaded);
    }
    SqlSource sql = (SqlSource) loaded.getSource("db");
    assertThat(sql.getRdbms()).isSameAs(jdbc.get("pgsql"));
    assertThat(sql.getPassword()).isEqualTo("p4ssw0rd");
  }

  @Test
  void sqlPasswordIsEncrypted() throws Exception {
    String json = save(resource());
    assertThat(json).doesNotContain("p4ssw0rd").contains("{aesgcm}");
  }

  @Test
  void derivedAndSeparatelyStoredFieldsAreLeftOut() throws Exception {
    String json = save(resource());
    assertThat(json)
        .doesNotContain("\"shortname\"")
        .doesNotContain("\"eml\"")
        .doesNotContain("\"dataPackageMetadata\"")
        .doesNotContain("\"inferredMetadata\"")
        .doesNotContain("/data/resources/res1/sources/occurrence.txt")
        .contains("\"schemaVersion\" : 1");
  }

  @Test
  void sourcesKeepTheirType() throws Exception {
    Resource loaded = load(save(resource()));
    assertThat(loaded.getSource("occurrence")).isInstanceOf(TextFileSource.class);
    assertThat(loaded.getSource("sheet")).isInstanceOf(ExcelFileSource.class);
    assertThat(((ExcelFileSource) loaded.getSource("sheet")).getSheetIdx()).isEqualTo(2);
    assertThat(loaded.getSource("db")).isInstanceOf(SqlSource.class);
    assertThat(((UrlSource) loaded.getSource("remote")).getUrl()).isEqualTo(URI.create("https://example.org/occurrence.txt"));
  }

  @Test
  void missingReferencesBecomeNullAsInIpt3() throws Exception {
    String json = save(resource())
        .replace(OCCURRENCE + "\"", "http://rs.tdwg.org/dwc/terms/Missing\"")
        .replace("jcuadra@gbif.org", "gone@gbif.org")
        .replace(SCHEMA_ID, "http://unknown/schema");
    Resource loaded = load(json);
    ExtensionMapping mapping = loaded.getMappings().get(0);
    assertThat(mapping.getExtension()).isNull();
    assertThat(mapping.getExtensionVerbatim()).isEqualTo("http://rs.tdwg.org/dwc/terms/Missing");
    assertThat(loaded.getManagers()).containsExactly((User) null);
    assertThat(loaded.getAllDataPackageMappings().get(0).getDataPackageSchema()).isNull();
    assertThat(loaded.getAllDataPackageMappings().get(0).getFields().get(0).getField()).isNull();
  }

  @Test
  void inferredMetadataRoundTrips() {
    InferredEmlMetadata inferred = new InferredEmlMetadata();
    inferred.setLastModified(new Date(1345123516892L));
    StringWriter w = new StringWriter();
    persister.saveInferredMetadata(inferred, w);
    InferredEmlMetadata loaded = persister.loadInferredEmlMetadata(
        new ByteArrayInputStream(w.toString().getBytes(StandardCharsets.UTF_8)));
    assertThat(loaded.getLastModified()).isEqualTo(new Date(1345123516892L));
  }
}
