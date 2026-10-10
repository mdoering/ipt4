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
package org.gbif.ipt.app;

import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.config.ConfigWarnings;
import org.gbif.ipt.config.DataDir;
import org.gbif.ipt.config.ExtensionMonitor;
import org.gbif.ipt.config.JdbcSupport;
import org.gbif.ipt.config.LoggingControl;
import org.gbif.ipt.config.PublishingMonitor;
import org.gbif.ipt.http.IptHttp;
import org.gbif.ipt.i18n.BundleMessages;
import org.gbif.ipt.i18n.Messages;
import org.gbif.ipt.i18n.SimpleTextProvider;
import org.gbif.ipt.model.DataPackageSchema;
import org.gbif.ipt.model.Extension;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.factory.DataSchemaFactory;
import org.gbif.ipt.model.factory.ExtensionFactory;
import org.gbif.ipt.model.factory.ThesaurusHandlingRule;
import org.gbif.ipt.model.factory.VocabularyFactory;
import org.gbif.ipt.legacy.ResourceFolderImporter;
import org.gbif.ipt.persistence.JsonResourcePersister;
import org.gbif.ipt.persistence.ReferenceLookup;
import org.gbif.ipt.persistence.SecretCipher;
import org.gbif.ipt.service.admin.DataPackageSchemaManager;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.admin.impl.ConfigManagerImpl;
import org.gbif.ipt.service.admin.impl.DataPackageSchemaManagerImpl;
import org.gbif.ipt.service.admin.impl.ExtensionManagerImpl;
import org.gbif.ipt.service.admin.impl.ExtensionsHolder;
import org.gbif.ipt.service.admin.impl.RegistrationManagerImpl;
import org.gbif.ipt.service.admin.impl.UserAccountManagerImpl;
import org.gbif.ipt.service.admin.impl.VocabulariesManagerImpl;
import org.gbif.ipt.service.file.DownloadFileManager;
import org.gbif.ipt.service.file.FileStoreManager;
import org.gbif.ipt.service.manage.ResourceFolderImport;
import org.gbif.ipt.service.manage.ResourcePersister;
import org.gbif.ipt.service.manage.impl.JsonServiceImpl;
import org.gbif.ipt.service.manage.impl.MetadataReaderImpl;
import org.gbif.ipt.service.manage.impl.ResourceCoordinator;
import org.gbif.ipt.service.manage.impl.ResourceDataTableViewManagerImpl;
import org.gbif.ipt.service.manage.impl.ResourceImportServiceImpl;
import org.gbif.ipt.service.manage.impl.ResourceLoaderImpl;
import org.gbif.ipt.service.manage.impl.ResourceManagerImpl;
import org.gbif.ipt.service.manage.impl.ResourceMetadataInferringServiceImpl;
import org.gbif.ipt.service.manage.impl.ResourceMetadataLoaderImpl;
import org.gbif.ipt.service.manage.impl.ResourcePublicationManagerImpl;
import org.gbif.ipt.service.manage.impl.ResourceTypeServiceImpl;
import org.gbif.ipt.service.manage.impl.ResourceVersioningServiceImpl;
import org.gbif.ipt.service.manage.impl.SourceManagerImpl;
import org.gbif.ipt.service.manage.impl.YamlServiceImpl;
import org.gbif.ipt.service.registry.impl.RegistryManagerImpl;
import org.gbif.ipt.task.Eml2Rtf;
import org.gbif.ipt.task.GenerateDCAT;
import org.gbif.ipt.task.GenerateDarwinCoreDataPackageFactoryImpl;
import org.gbif.ipt.task.GenerateDataPackageFactoryImpl;
import org.gbif.ipt.task.GenerateDwcaFactoryImpl;
import org.gbif.ipt.utils.InputStreamUtils;
import org.gbif.ipt.validation.DataPackageMetadataValidator;
import org.gbif.ipt.validation.EmlValidator;
import org.gbif.ipt.validation.IptValidator;
import org.gbif.ipt.validation.OrganisationSupport;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Wires ipt-core, whose classes carry no Spring annotations of their own. The {@link Import} list is IPT 3's
 * {@code applicationContext.xml}; the bean methods replace its {@code IPTModule} factory methods.
 */
@Configuration(proxyBeanMethods = false)
@Import({
    ConfigWarnings.class,
    InputStreamUtils.class,
    ExtensionsHolder.class,
    RegistryManagerImpl.class,
    ExtensionManagerImpl.class,
    VocabulariesManagerImpl.class,
    DataPackageSchemaManagerImpl.class,
    UserAccountManagerImpl.class,
    RegistrationManagerImpl.class,
    ConfigManagerImpl.class,
    SourceManagerImpl.class,
    ResourceManagerImpl.class,
    ResourceCoordinator.class,
    ResourceLoaderImpl.class,
    ResourceImportServiceImpl.class,
    ResourceTypeServiceImpl.class,
    ResourceVersioningServiceImpl.class,
    ResourceMetadataLoaderImpl.class,
    ResourceMetadataInferringServiceImpl.class,
    ResourcePublicationManagerImpl.class,
    ResourceDataTableViewManagerImpl.class,
    JsonServiceImpl.class,
    YamlServiceImpl.class,
    MetadataReaderImpl.class,
    ThesaurusHandlingRule.class,
    ExtensionFactory.class,
    VocabularyFactory.class,
    DataSchemaFactory.class,
    Eml2Rtf.class,
    GenerateDwcaFactoryImpl.class,
    GenerateDataPackageFactoryImpl.class,
    GenerateDarwinCoreDataPackageFactoryImpl.class,
    GenerateDCAT.class,
    PublishingMonitor.class,
    ExtensionMonitor.class,
    DataPackageMetadataValidator.class,
    EmlValidator.class,
    OrganisationSupport.class,
    IptValidator.class,
    DownloadFileManager.class,
    FileStoreManager.class
})
public class CoreBeans {

  @Bean
  public Messages messages() {
    return new BundleMessages();
  }

  @Bean
  public SimpleTextProvider simpleTextProvider(Messages messages) {
    return new SimpleTextProvider(messages);
  }

  @Bean
  public DataDirStartup.Opened openedDataDir(IptProperties props, ObjectProvider<BuildProperties> build) {
    return DataDirStartup.open(props.dataDir(), version(build), System.getenv());
  }

  @Bean
  public DataDir dataDir(DataDirStartup.Opened opened) {
    return opened.dataDir();
  }

  @Bean
  public AppConfig appConfig(DataDir dataDir, IptProperties props) {
    AppConfig cfg = new AppConfig(dataDir);
    Map<String, String> defaults = new HashMap<>();
    defaults.put("dev.devmode", String.valueOf(props.devMode()));
    defaults.putAll(props.defaults());
    cfg.setDefaults(defaults);
    cfg.loadConfig();
    return cfg;
  }

  /** Verified against the data dir marker at start-up (see {@link DataDirStartup}). */
  @Bean
  public SecretCipher secretCipher(DataDirStartup.Opened opened) {
    return opened.cipher();
  }

  @Bean
  public IptHttp iptHttp(AppConfig cfg) {
    String userAgent = String.format("GBIF-IPT/%s (+https://www.gbif.org/ipt) Java/%s (%s)",
        cfg.getVersion(), System.getProperty("java.version", "?"), System.getProperty("os.name", "?"));
    return new IptHttp(userAgent, Duration.ofSeconds(30));
  }

  @Bean
  public SAXParserFactory saxParserFactory() {
    SAXParserFactory saxf = SAXParserFactory.newInstance();
    saxf.setValidating(false);
    saxf.setNamespaceAware(true);
    return saxf;
  }

  @Bean
  public JdbcSupport jdbcSupport() throws IOException {
    return JdbcSupport.fromClasspath();
  }

  /**
   * References in stored resources are resolved through the managers, looked up lazily: the managers themselves
   * depend on the persister.
   */
  @Bean
  public ResourcePersister resourcePersister(SecretCipher cipher, JdbcSupport jdbc,
                                             ObjectProvider<UserAccountManager> users,
                                             ObjectProvider<RegistrationManager> registrations,
                                             ExtensionsHolder extensions,
                                             ObjectProvider<DataPackageSchemaManager> schemas) {
    return new JsonResourcePersister(cipher, jdbc, new ReferenceLookup() {
      @Override
      public User user(String email) {
        return users.getObject().get(email);
      }

      @Override
      public Organisation organisation(String key) {
        return registrations.getObject().get(key);
      }

      @Override
      public Extension extension(String rowType) {
        return extensions.getExtensionsByRowtype().get(rowType);
      }

      @Override
      public DataPackageSchema dataPackageSchema(String identifier) {
        return schemas.getObject().get(identifier);
      }
    });
  }

  /** Resource folders copied from IPT 3 or from another IPT4 can be imported. */
  @Bean
  public ResourceFolderImport resourceFolderImport(SecretCipher cipher, JdbcSupport jdbc) {
    return new ResourceFolderImporter(cipher, jdbc);
  }

  @Bean
  public LoggingControl loggingControl(LoggingSystem loggingSystem) {
    return new LogbackLoggingControl(loggingSystem);
  }

  static String version(ObjectProvider<BuildProperties> build) {
    BuildProperties props = build.getIfAvailable();
    return props == null ? "development" : props.getVersion();
  }
}
