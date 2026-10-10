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
package org.gbif.ipt.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gbif.ipt.utils.InputStreamUtils;
import org.gbif.ipt.http.IptHttp;


import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TestBeanProvider {

  private static final Logger LOG = LogManager.getLogger(TestBeanProvider.class);

  protected static final int CONNECTION_TIMEOUT_MSEC = 30_000;

  public static IptHttp provideHttpClient() {
    // new threadsafe, multithreaded http client with support for HTTP and HTTPS.
    String version = "unknown";
    try (InputStream configStream = new InputStreamUtils().classpathStream(AppConfig.CLASSPATH_PROPFILE)) {
      Properties props = new Properties();
      if (configStream == null) {
        LOG.error("Could not load default configuration from application.properties in classpath");
      } else {
        props.load(configStream);
        LOG.debug("Loaded default configuration from application.properties in classpath");
        version = props.getProperty(AppConfig.DEV_VERSION);
      }
    } catch (Exception e) {
      LOG.error("Unable to read version from application.properties, continuing start-up.", e);
    }

    String userAgent = String.format("GBIF-IPT/%s (+https://www.gbif.org/ipt) Java/%s (%s)",
        version,
        System.getProperty("java.version", "?"),
        System.getProperty("os.name", "?")
    );

    return new IptHttp(userAgent, java.time.Duration.ofMillis(CONNECTION_TIMEOUT_MSEC));
  }

  public static JdbcSupport provideJdbcSupport() {
    JdbcSupport jdbcs = new JdbcSupport();
    InputStreamUtils streamUtils = new InputStreamUtils();
    InputStream configStream = streamUtils.classpathStream(JdbcSupport.CLASSPATH_PROPFILE);
    if (configStream == null) {
      LOG.error("Could not find supported jdbc driver information file " + JdbcSupport.CLASSPATH_PROPFILE);
    } else {
      try {
        Properties props = new Properties();
        props.load(configStream);
        jdbcs.setProperties(props);
        LOG.debug("Loaded supported jdbc driver information from " + JdbcSupport.CLASSPATH_PROPFILE);
      } catch (IOException e) {
        LOG.error("Could not load supported jdbc driver information from " + JdbcSupport.CLASSPATH_PROPFILE, e);
      }
    }
    return jdbcs;
  }

  public static SAXParserFactory provideNsAwareSaxParserFactory() {
    SAXParserFactory saxf = null;
    try {
      saxf = SAXParserFactory.newInstance();
      saxf.setValidating(false);
      saxf.setNamespaceAware(true);
    } catch (Exception e) {
      LOG.error("Cant create namespace aware SAX Parser Factory: " + e.getMessage(), e);
    }
    return saxf;
  }

  /** A data dir in the temp folder, like IPT 3's test {@code datadir.location}. */
  public static DataDir provideDataDir() {
    DataDir dd = DataDir.buildFromString(System.getProperty("java.io.tmpdir") + "/ipt4-test-datadir");
    try {
      if (dd.isConfigured()) {
        dd.clearTmp();
      }
    } catch (IOException e) {
      LOG.warn("Couldn't clear temporary data dir folder", e);
    }
    return dd;
  }

  public static AppConfig provideAppConfig(DataDir dataDir) {
    return new AppConfig(dataDir);
  }
}
