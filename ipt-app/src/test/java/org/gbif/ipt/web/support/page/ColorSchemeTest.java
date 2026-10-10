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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ColorSchemeTest {

  @Test
  void convertsHexToCssRgbTriplet() {
    assertThat(ColorScheme.toRgb("#61a861")).isEqualTo("97,168,97");
    assertThat(ColorScheme.toRgb("#ffffff")).isEqualTo("255,255,255");
  }

  @Test
  void defaultsMatchIpt3() {
    assertThat(ColorScheme.DEFAULT.primaryColor()).isEqualTo("#61a861");
    assertThat(ColorScheme.DEFAULT.navbarColor()).isEqualTo("#78b578");
    assertThat(ColorScheme.DEFAULT.linkColor()).isEqualTo("#4ba2ce");
  }
}
