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
package org.gbif.ipt.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageSnapshotTest {

  @Test
  void attributeOrderAndEntityEncodingDoNotMatter() {
    String a = "<p><a href=\"x?a=1&b=2\" title='it&#39;s'>Caf&eacute;</a><input name=\"n\" type=\"submit\"/></p>";
    String b = "<p><a title=\"it's\" href=\"x?a=1&amp;b=2\">Café</a><input type=\"submit\" name=\"n\"></p>";
    assertThat(PageSnapshot.normalise(a)).isEqualTo(PageSnapshot.normalise(b));
  }

  @Test
  void differentTextOrAttributesMatter() {
    assertThat(PageSnapshot.normalise("<p title=\"a\">x</p>")).isNotEqualTo(PageSnapshot.normalise("<p title=\"b\">x</p>"));
    assertThat(PageSnapshot.normalise("<p>x</p>")).isNotEqualTo(PageSnapshot.normalise("<p>&lt;b&gt;x</p>"));
  }

  @Test
  void csrfTokensAndTimestampsAreMasked() {
    String a = "<meta name=\"_csrf\" content=\"abc\"><form><input type=\"hidden\" name=\"_csrf\" value=\"abc\"></form>"
        + "<p>2026-10-11 10:12:13</p><script>var t = '2026-10-11T10:12';</script>";
    String b = "<meta name=\"_csrf\" content=\"xyz\"><form><input type=\"hidden\" name=\"_csrf\" value=\"xyz\"></form>"
        + "<p>2026-10-12 11:00:00</p><script>var t = '2026-10-12T11:00';</script>";
    assertThat(PageSnapshot.normalise(a)).isEqualTo(PageSnapshot.normalise(b));
  }

  @Test
  void tokensInScriptsTimesAndTempDirsAreMasked() {
    String a = "<script>var f = '<input type=\"hidden\" name=\"_csrf\" value=\"abc-1\"/>'; d.append(\"_csrf\", \"a\");"
        + " var free = 28310953984;</script>"
        + "<p>11 Oct 2026, 14:29:31 <span>14:29:26</span> 26 GB</p><input value=\"/tmp/ipt-portal8715749116694822052/ipt4\">";
    String b = "<script>var f = '<input type=\"hidden\" name=\"_csrf\" value=\"xyz_2\"/>'; d.append(\"_csrf\", \"b\");"
        + " var free = 28276678656;</script>"
        + "<p>11 Oct 2026, 14:29:54 <span>14:30:00</span> 25 GB</p><input value=\"/tmp/ipt-portal16668370174724247599/ipt4\">";
    assertThat(PageSnapshot.normalise(a)).isEqualTo(PageSnapshot.normalise(b));
  }

  @Test
  void scriptsAreKept() {
    assertThat(PageSnapshot.normalise("<script>var a = 'x';</script>"))
        .isNotEqualTo(PageSnapshot.normalise("<script>var a = 'y';</script>"));
  }

  @Test
  void fileNameOfUrl() {
    assertThat(PageSnapshot.fileName("/manage/resource.do?r=res1&a=b")).isEqualTo("manage_resource.do_r=res1_a=b.html");
    assertThat(PageSnapshot.fileName("/")).isEqualTo("_.html");
  }
}
