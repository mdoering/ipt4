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

import org.gbif.api.model.common.DOI;
import org.gbif.dwc.terms.Term;
import org.gbif.ipt.config.JdbcSupport;
import org.gbif.ipt.legacy.v3.PBEEncrypt;
import org.gbif.ipt.legacy.v3.model.LegacyIpt;
import org.gbif.ipt.legacy.v3.model.LegacyOrganisation;
import org.gbif.ipt.legacy.v3.model.LegacyRegistration;
import org.gbif.ipt.legacy.v3.xstream.SafeTreeMapConverter;
import org.gbif.ipt.legacy.v3.xstream.SafeTreeSetConverter;
import org.gbif.ipt.legacy.v3.xstream.StubConverters;
import org.gbif.ipt.model.DataPackageField;
import org.gbif.ipt.model.DataPackageFieldConstraints;
import org.gbif.ipt.model.DataPackageFieldMapping;
import org.gbif.ipt.model.DataPackageFieldReference;
import org.gbif.ipt.model.DataPackageMapping;
import org.gbif.ipt.model.DataPackageTableSchema;
import org.gbif.ipt.model.DataPackageTableSchemaForeignKey;
import org.gbif.ipt.model.ExcelFileSource;
import org.gbif.ipt.model.ExtensionMapping;
import org.gbif.ipt.model.ExtensionProperty;
import org.gbif.ipt.model.InferredCamtrapGeographicScope;
import org.gbif.ipt.model.InferredCamtrapMetadata;
import org.gbif.ipt.model.InferredCamtrapTaxonomicScope;
import org.gbif.ipt.model.InferredCamtrapTemporalScope;
import org.gbif.ipt.model.InferredEmlGeographicCoverage;
import org.gbif.ipt.model.InferredEmlMetadata;
import org.gbif.ipt.model.InferredEmlTaxonomicCoverage;
import org.gbif.ipt.model.InferredEmlTemporalCoverage;
import org.gbif.ipt.model.InferredMetadata;
import org.gbif.ipt.model.Ipt;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.PropertyMapping;
import org.gbif.ipt.model.Registration;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.SqlSource;
import org.gbif.metadata.eml.ipt.model.TaxonKeyword;
import org.gbif.ipt.model.TextFileSource;
import org.gbif.ipt.model.UrlSource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.VersionHistory;
import org.gbif.ipt.model.metadata.OrganizedTaxonomicKeywords;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.ArrayTypePermission;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;

/**
 * Reads the XML files of IPT 3 data dirs ({@code resource.xml}, {@code inferredMetadata.xml}, {@code users.xml},
 * {@code registration.xml} and {@code registration2.xml}) with IPT 3's XStream configuration, onto the IPT4 model.
 * References come back as stand-ins holding their key; secrets come back in clear, decrypted with IPT 3's built-in key.
 * <p>
 * Unlike IPT 3, XStream only instantiates the IPT's own classes and plain JDK value types: these files can come from
 * uploaded resource folders.
 */
public class LegacyXmlReader {

  private static final byte[] PBE_SALT = {0x00, 0x05, 0x02, 0x05, 0x04, 0x25, 0x06, 0x17};
  private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(12);

  /** The users of {@code users.xml}; {@code rehashed} counts passwords IPT 3 still kept PBE-encrypted. */
  public record Users(List<User> users, int rehashed) {
  }

  private final List<String> problems = new ArrayList<>();
  private final XStream resources;
  private final XStream users;
  private final XStream registrationV1;
  private final XStream registrationV2;

  public LegacyXmlReader() {
    this(jdbcFromClasspath());
  }

  private static JdbcSupport jdbcFromClasspath() {
    try {
      return JdbcSupport.fromClasspath();
    } catch (IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  public LegacyXmlReader(JdbcSupport jdbc) {
    PBEEncrypt pbe = legacyPbe();
    this.resources = resourceXStream(jdbc, pbe);
    this.users = usersXStream();
    this.registrationV1 = registrationV1XStream();
    this.registrationV2 = registrationV2XStream(pbe);
  }

  /** The cipher IPT 3 used for stored secrets; its key was the same in every installation. */
  public static PBEEncrypt legacyPbe() {
    try {
      return new PBEEncrypt("Carla Maria Luise", PBE_SALT, 9);
    } catch (PBEEncrypt.EncryptionException e) {
      throw new IllegalStateException("Cannot create the IPT 3 password cipher", e);
    }
  }

  /** Problems found while reading, e.g. secrets that could not be decrypted. Cleared by {@link #takeProblems()}. */
  public List<String> takeProblems() {
    List<String> copy = List.copyOf(problems);
    problems.clear();
    return copy;
  }

  public Resource readResource(InputStream in) throws IOException {
    return (Resource) read(resources, in);
  }

  public InferredMetadata readInferredMetadata(InputStream in) throws IOException {
    return (InferredMetadata) read(resources, in);
  }

  public Users readUsers(InputStream in) throws IOException {
    List<User> list = new ArrayList<>();
    int rehashed = 0;
    try (ObjectInputStream ois = users.createObjectInputStream(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      while (true) {
        User u = (User) ois.readObject();
        String pass = u.getPassword();
        if (pass != null && !pass.startsWith("$2a$")) {
          try {
            u.setPassword(BCRYPT.encode(legacyPbe().decrypt(pass)));
            rehashed++;
          } catch (PBEEncrypt.EncryptionException e) {
            problems.add("The password of user " + u.getEmail() + " could not be decrypted; it must be reset");
            u.setPassword(null);
          }
        }
        list.add(u);
      }
    } catch (EOFException e) {
      // end of users
    } catch (ClassNotFoundException e) {
      throw new IOException("Unexpected content in users.xml", e);
    }
    return new Users(list, rehashed);
  }

  /** Reads {@code registration2.xml}, the registration of IPT 2.2 and later. */
  public Registration readRegistration(InputStream in) throws IOException {
    Registration result = new Registration();
    try (ObjectInputStream ois = registrationV2.createObjectInputStream(
        new InputStreamReader(in, StandardCharsets.UTF_8))) {
      Registration reg = (Registration) ois.readObject();
      result.setHostingOrganisation(reg.getHostingOrganisation());
      result.setIpt(reg.getIpt());
      result.setNetwork(reg.getNetwork());
      while (true) {
        Organisation org = (Organisation) ois.readObject();
        result.getAssociatedOrganisations().put(org.getKey().toString(), org);
      }
    } catch (EOFException e) {
      // end of organisations
    } catch (ClassNotFoundException e) {
      throw new IOException("Unexpected content in registration2.xml", e);
    }
    return result;
  }

  /** Reads {@code registration.xml}, the unencrypted registration of IPT versions before 2.2. */
  public Registration readLegacyRegistration(InputStream in) throws IOException {
    Registration result = new Registration();
    try (ObjectInputStream ois = registrationV1.createObjectInputStream(
        new InputStreamReader(in, StandardCharsets.UTF_8))) {
      LegacyRegistration reg = (LegacyRegistration) ois.readObject();
      result.setHostingOrganisation(organisation(reg.getHostingOrganisation()));
      result.setIpt(ipt(reg.getIpt()));
      while (true) {
        Organisation org = organisation((LegacyOrganisation) ois.readObject());
        if (org != null && org.getKey() != null) {
          result.getAssociatedOrganisations().put(org.getKey().toString(), org);
        }
      }
    } catch (EOFException e) {
      // end of organisations
    } catch (ClassNotFoundException e) {
      throw new IOException("Unexpected content in registration.xml", e);
    }
    return result;
  }

  private static Object read(XStream xstream, InputStream in) throws IOException {
    try {
      return xstream.fromXML(new InputStreamReader(in, StandardCharsets.UTF_8));
    } catch (RuntimeException e) {
      throw new IOException("Cannot read IPT 3 XML: " + e.getMessage(), e);
    }
  }

  /** IPT 3's RegistrationManagerImpl.createIptFromLegacyIpt. */
  private static Ipt ipt(LegacyIpt ipt) {
    if (ipt == null) {
      return null;
    }
    Ipt i = new Ipt();
    if (ipt.getKey() != null) {
      i.setKey(ipt.getKey().toString());
    }
    i.setDescription(StringUtils.trimToNull(ipt.getDescription()));
    i.setWsPassword(StringUtils.trimToNull(ipt.getWsPassword()));
    i.setName(StringUtils.trimToNull(ipt.getName()));
    i.setCreated(ipt.getCreated());
    i.setLanguage(StringUtils.trimToNull(ipt.getLanguage()));
    i.setLogoUrl(StringUtils.trimToNull(ipt.getLogoUrl()));
    i.setHomepageURL(StringUtils.trimToNull(ipt.getHomepageURL()));
    if (ipt.getOrganisationKey() != null) {
      i.setOrganisationKey(ipt.getOrganisationKey().toString());
    }
    i.setPrimaryContactType(StringUtils.trimToNull(ipt.getPrimaryContactType()));
    i.setPrimaryContactPhone(StringUtils.trimToNull(ipt.getPrimaryContactPhone()));
    i.setPrimaryContactLastName(StringUtils.trimToNull(ipt.getPrimaryContactLastName()));
    i.setPrimaryContactFirstName(StringUtils.trimToNull(ipt.getPrimaryContactFirstName()));
    i.setPrimaryContactAddress(StringUtils.trimToNull(ipt.getPrimaryContactAddress()));
    i.setPrimaryContactEmail(StringUtils.trimToNull(ipt.getPrimaryContactEmail()));
    i.setPrimaryContactDescription(StringUtils.trimToNull(ipt.getPrimaryContactDescription()));
    i.setPrimaryContactName(StringUtils.trimToNull(ipt.getPrimaryContactName()));
    return i;
  }

  /** IPT 3's RegistrationManagerImpl.createOrganisationFromLegacyOrganisation. */
  private static Organisation organisation(LegacyOrganisation organisation) {
    if (organisation == null) {
      return null;
    }
    Organisation o = new Organisation();
    if (organisation.getKey() != null) {
      o.setKey(organisation.getKey().toString());
    }
    o.setName(organisation.getName());
    o.setAlias(organisation.getAlias());
    o.setCanHost(organisation.isCanHost());
    o.setPassword(organisation.getPassword());
    return o;
  }

  private static XStream secured() {
    XStream x = new XStream();
    x.addPermission(NoTypePermission.NONE);
    x.addPermission(NullPermission.NULL);
    x.addPermission(PrimitiveTypePermission.PRIMITIVES);
    x.addPermission(ArrayTypePermission.ARRAYS);
    x.allowTypesByWildcard(new String[] {"org.gbif.**", "java.util.*", "java.math.*"});
    x.allowTypes(new Class[] {String.class, Integer.class, Long.class, Double.class, Float.class, Boolean.class,
        Short.class, Byte.class, Character.class, java.net.URI.class, java.net.URL.class, java.io.File.class,
        java.sql.Timestamp.class, java.util.Date.class});
    x.ignoreUnknownElements();
    return x;
  }

  /** IPT 3's ResourceXStreamFactory, with stand-ins for references. */
  private XStream resourceXStream(JdbcSupport jdbc, PBEEncrypt pbe) {
    XStream x = secured();
    x.alias("resource", Resource.class);
    x.alias("user", User.class);
    x.alias("inferredMetadata", InferredEmlMetadata.class);
    x.alias("inferredMetadataCamtrap", InferredCamtrapMetadata.class);
    x.alias("inferredGeographicCoverage", InferredEmlGeographicCoverage.class);
    x.alias("inferredGeographicScope", InferredCamtrapGeographicScope.class);
    x.alias("inferredTaxonomicCoverage", InferredEmlTaxonomicCoverage.class);
    x.alias("inferredTaxonomicScope", InferredCamtrapTaxonomicScope.class);
    x.alias("inferredTemporalCoverage", InferredEmlTemporalCoverage.class);
    x.alias("inferredTemporalScope", InferredCamtrapTemporalScope.class);
    x.alias("taxonKeyword", TaxonKeyword.class);
    x.alias("organizedTaxonomicKeywords", OrganizedTaxonomicKeywords.class);
    x.alias("filesource", TextFileSource.class);
    x.alias("excelsource", ExcelFileSource.class);
    x.alias("sqlsource", SqlSource.class);
    x.alias("urlsource", UrlSource.class);
    x.alias("mapping", ExtensionMapping.class);
    x.alias("field", PropertyMapping.class);
    x.alias("dataPackageMapping", DataPackageMapping.class);
    x.alias("dataPackageFieldMapping", DataPackageFieldMapping.class);
    x.alias("tableSchema", DataPackageTableSchema.class);
    x.alias("dataPackageField", DataPackageField.class);
    x.alias("dataPackageForeignKey", DataPackageTableSchemaForeignKey.class);
    x.alias("dataPackageFieldReference", DataPackageFieldReference.class);
    x.alias("constraints", DataPackageFieldConstraints.class);
    x.alias("versionhistory", VersionHistory.class);
    x.alias("doi", DOI.class);
    x.omitField(Resource.class, "shortname");
    x.omitField(Resource.class, "eml");
    x.omitField(Resource.class, "dataPackageMetadata");
    x.omitField(Resource.class, "type");
    x.omitField(Resource.class, "inferredMetadata");
    x.omitField(TextFileSource.class, "file");
    x.registerConverter(new SafeTreeMapConverter(), 10000);
    x.registerConverter(new SafeTreeSetConverter(), 10000);
    x.registerConverter(StubConverters.users());
    x.registerConverter(StubConverters.extensions());
    x.registerConverter(StubConverters.terms());
    x.registerConverter(StubConverters.schemas());
    x.registerConverter(StubConverters.tableNames());
    x.registerConverter(StubConverters.fields());
    x.registerConverter(StubConverters.pbePasswords(pbe, problems::add));
    x.addDefaultImplementation(ExtensionProperty.class, Term.class);
    x.registerConverter(StubConverters.organisations());
    x.registerConverter(StubConverters.jdbc(jdbc));
    return x;
  }

  /** IPT 3's UserAccountManagerImpl mapping. */
  private static XStream usersXStream() {
    XStream x = secured();
    x.alias("user", User.class);
    x.useAttributeFor(User.class, "email");
    x.useAttributeFor(User.class, "password");
    x.useAttributeFor(User.class, "firstname");
    x.useAttributeFor(User.class, "lastname");
    x.useAttributeFor(User.class, "role");
    x.useAttributeFor(User.class, "lastLogin");
    return x;
  }

  private static XStream registrationV1XStream() {
    XStream x = secured();
    x.omitField(LegacyRegistration.class, "associatedOrganisations");
    x.alias("organisation", LegacyOrganisation.class);
    x.alias("registry", LegacyRegistration.class);
    return x;
  }

  private XStream registrationV2XStream(PBEEncrypt pbe) {
    XStream x = secured();
    x.omitField(Registration.class, "associatedOrganisations");
    x.alias("organisation", Organisation.class);
    x.alias("registry", Registration.class);
    x.registerConverter(StubConverters.pbePasswords(pbe, problems::add));
    return x;
  }
}
