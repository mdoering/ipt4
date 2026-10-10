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
package org.gbif.ipt.web.support.page;

import org.gbif.ipt.app.IptProperties;

import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;

import static org.assertj.core.api.Assertions.assertThat;

class SiteConfigTest {

  private static IptProperties props(String baseUrl) {
    return new IptProperties(null, baseUrl, "en", null, false);
  }

  @Test
  void versionCarriesTheGitRevisionLikeIpt3() {
    Properties build = new Properties();
    build.setProperty("version", "4.0.0-SNAPSHOT");
    Properties git = new Properties();
    git.setProperty("commit.id.abbrev", "abc1234");

    SiteConfig cfg =
        new SiteConfig(props(null), Optional.of(new BuildProperties(build)), Optional.of(new GitProperties(git)));

    assertThat(cfg.getVersion()).isEqualTo("4.0.0-SNAPSHOT-rabc1234");
    assertThat(cfg.getShortVersion()).isEqualTo("4.0.0-SNAPSHOT");
  }

  @Test
  void versionWithoutBuildInfo() {
    SiteConfig cfg = new SiteConfig(props(null), Optional.empty(), Optional.empty());
    assertThat(cfg.getVersion()).isEqualTo("dev");
  }

  @Test
  void baseUrlLosesTrailingSlashesAndBlankMeansUnset() {
    assertThat(new SiteConfig(props("https://ipt.example.org//"), Optional.empty(), Optional.empty()).getBaseUrl())
        .isEqualTo("https://ipt.example.org");
    assertThat(new SiteConfig(props(" "), Optional.empty(), Optional.empty()).getBaseUrl()).isNull();
  }
}
