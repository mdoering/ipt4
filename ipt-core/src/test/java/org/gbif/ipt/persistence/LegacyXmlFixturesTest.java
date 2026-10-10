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
import org.gbif.ipt.config.TestBeanProvider;
import org.gbif.ipt.model.Extension;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.converter.ConceptTermConverter;
import org.gbif.ipt.model.converter.DataPackageFieldConverter;
import org.gbif.ipt.model.converter.DataPackageIdentifierConverter;
import org.gbif.ipt.model.converter.ExtensionMappingConverter;
import org.gbif.ipt.model.converter.ExtensionRowTypeConverter;
import org.gbif.ipt.model.converter.JdbcInfoConverter;
import org.gbif.ipt.model.converter.OrganisationKeyConverter;
import org.gbif.ipt.model.converter.PasswordEncrypter;
import org.gbif.ipt.model.converter.TableSchemaNameConverter;
import org.gbif.ipt.model.converter.UserEmailConverter;
import org.gbif.ipt.model.factory.ExtensionFactory;
import org.gbif.ipt.model.factory.ThesaurusHandlingRule;
import org.gbif.ipt.service.admin.DataPackageSchemaManager;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.admin.VocabulariesManager;
import org.gbif.ipt.service.admin.impl.ExtensionsHolder;
import org.gbif.ipt.service.manage.impl.ResourceConvertersManager;
import org.gbif.ipt.service.manage.impl.XStreamResourcePersister;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Loads every legacy resource.xml test fixture with IPT 3's XStream setup, stores it as JSON, loads the JSON and checks
 * that both give the same objects. With {@code -Dipt.writeJsonFixtures=true} it also writes the JSON
 * next to each XML fixture; the ported tests read those. Removed once XStream leaves ipt-core (ipt-legacy-import
 * takes over the check).
 */
class LegacyXmlFixturesTest {

  static Stream<String> fixtures() throws Exception {
    Path dir = Paths.get(LegacyXmlFixturesTest.class.getResource("/resources").toURI());
    try (Stream<Path> files = Files.walk(dir)) {
      return files.filter(p -> p.getFileName().toString().matches("resource.*\\.xml"))
          .map(p -> dir.relativize(p).toString()).sorted().toList().stream();
    }
  }

  private final Map<String, User> users = new HashMap<>();
  private final Map<String, Organisation> orgs = new HashMap<>();
  private final Map<String, Extension> extensions = new HashMap<>();

  LegacyXmlFixturesTest() throws Exception {
    ExtensionFactory factory = new ExtensionFactory(new ThesaurusHandlingRule(mock(VocabulariesManager.class)),
        TestBeanProvider.provideNsAwareSaxParserFactory(), TestBeanProvider.provideHttpClient());
    for (String f : List.of("dwc_occurrence_2015-04-24.xml", "dwc_event_2015-04-24.xml", "dwc_taxon_2015-04-24.xml",
        "simple_image.xml")) {
      try (InputStream in = getClass().getResourceAsStream("/extensions/" + f)) {
        Extension e = factory.build(in);
        extensions.put(e.getRowType(), e);
      }
    }
  }

  private User user(String email) {
    return users.computeIfAbsent(email, e -> {
      User u = new User();
      u.setEmail(e);
      return u;
    });
  }

  private Organisation org(String key) {
    return orgs.computeIfAbsent(key, k -> {
      Organisation o = new Organisation();
      o.setKey(k);
      return o;
    });
  }

  private XStreamResourcePersister xstream(JdbcSupport jdbc) {
    UserAccountManager userManager = mock(UserAccountManager.class);
    when(userManager.get(anyString())).thenAnswer(i -> user(i.getArgument(0)));
    RegistrationManager registrationManager = mock(RegistrationManager.class);
    when(registrationManager.get(anyString())).thenAnswer(i -> org(i.getArgument(0)));
    ExtensionsHolder holder = mock(ExtensionsHolder.class);
    when(holder.getExtensionsByRowtype()).thenReturn(extensions);
    ExtensionRowTypeConverter extConverter = new ExtensionRowTypeConverter(holder);
    DataPackageIdentifierConverter schemaConverter = new DataPackageIdentifierConverter(mock(DataPackageSchemaManager.class));
    TableSchemaNameConverter tableConverter = new TableSchemaNameConverter();
    ResourceConvertersManager converters = new ResourceConvertersManager(
        new UserEmailConverter(userManager), new OrganisationKeyConverter(registrationManager),
        mock(ExtensionMappingConverter.class), extConverter, new ConceptTermConverter(extConverter),
        schemaConverter, tableConverter, new DataPackageFieldConverter(schemaConverter, tableConverter),
        new JdbcInfoConverter(jdbc));
    return new XStreamResourcePersister(converters, new PasswordEncrypter(TestBeanProvider.providePasswordEncryption()));
  }

  @ParameterizedTest
  @MethodSource("fixtures")
  void jsonKeepsEverythingXStreamKept(String fixture) throws Exception {
    JdbcSupport jdbc = TestBeanProvider.provideJdbcSupport();
    XStreamResourcePersister xml = xstream(jdbc);
    ReferenceLookup lookup = new ReferenceLookup() {
      @Override
      public User user(String email) {
        return LegacyXmlFixturesTest.this.user(email);
      }

      @Override
      public Organisation organisation(String key) {
        return org(key);
      }

      @Override
      public Extension extension(String rowType) {
        return extensions.get(rowType);
      }

      @Override
      public org.gbif.ipt.model.DataPackageSchema dataPackageSchema(String identifier) {
        return null;
      }
    };
    JsonResourcePersister json = new JsonResourcePersister(new SecretCipher(new byte[32]), jdbc, lookup);

    Resource fromXml;
    try (InputStream in = getClass().getResourceAsStream("/resources/" + fixture)) {
      fromXml = xml.load(in);
    }
    StringWriter jsonOut = new StringWriter();
    json.save(fromXml, jsonOut);
    Resource fromJson = json.load(new ByteArrayInputStream(jsonOut.toString().getBytes(StandardCharsets.UTF_8)));

    // XStream bypassed constructors, so collections it did not find stay null where JSON gives empty ones;
    // terms resolve by qualified name (IPT 3 resolved terms of unknown extensions against the previous mapping's)
    assertThat(fromJson).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .ignoringCollectionOrder()
        .withEqualsForType((x, y) -> x.qualifiedName().equals(y.qualifiedName()), org.gbif.dwc.terms.Term.class)
        .isEqualTo(fromXml);

    if (Boolean.getBoolean("ipt.writeJsonFixtures")) {
      Path target = Paths.get("src/test/resources/resources", fixture.replaceAll("\\.xml$", ".json"));
      Files.writeString(target, jsonOut.toString());
    }
  }
}
