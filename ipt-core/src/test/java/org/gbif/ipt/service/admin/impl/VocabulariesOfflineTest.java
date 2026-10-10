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
package org.gbif.ipt.service.admin.impl;

import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.config.ConfigWarnings;
import org.gbif.ipt.config.DataDir;
import org.gbif.ipt.config.TestBeanProvider;
import org.gbif.ipt.http.IptHttp;
import org.gbif.ipt.i18n.SimpleTextProvider;
import org.gbif.ipt.model.factory.VocabularyFactory;
import org.gbif.ipt.service.RegistryException;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.registry.RegistryManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** IPT 3 only loaded installed vocabularies when the registry answered; IPT4 also loads them when it is down. */
class VocabulariesOfflineTest {

  @TempDir
  Path tmp;

  @Test
  void loadsInstalledVocabulariesWhenTheRegistryIsDown() throws Exception {
    Path dir = Files.createDirectories(tmp.resolve(".vocabularies"));
    Files.writeString(dir.resolve("http_iso_org_639-2.vocab"), """
        <?xml version="1.0" encoding="UTF-8"?>
        <thesaurus dc:URI="http://iso.org/639-2" dc:title="Languages" xmlns="http://rs.gbif.org/thesaurus/" xmlns:dc="http://purl.org/dc/terms/">
          <concept dc:identifier="eng" dc:URI="http://iso.org/639-2/eng"><preferred><term dc:title="English" xml:lang="en"/></preferred></concept>
        </thesaurus>
        """);
    DataDir dataDir = mock(DataDir.class);
    when(dataDir.configFile(anyString())).thenAnswer(i -> new File(tmp.toFile(), i.getArgument(0)));
    RegistryManager registry = mock(RegistryManager.class);
    when(registry.getVocabularies()).thenThrow(new RegistryException(RegistryException.Type.IO_ERROR, "x", "down"));

    VocabulariesManagerImpl manager = new VocabulariesManagerImpl(mock(AppConfig.class), dataDir,
        new VocabularyFactory(TestBeanProvider.provideNsAwareSaxParserFactory()), mock(IptHttp.class), registry,
        new ConfigWarnings(), mock(SimpleTextProvider.class), mock(RegistrationManager.class));

    assertThat(manager.load()).isEqualTo(1);
    assertThat(manager.get("http://iso.org/639-2").findConcept("eng")).isNotNull();
    assertThat(dir.resolve("http_iso_org_639-2.vocab")).as("files are kept").exists();
  }
}
