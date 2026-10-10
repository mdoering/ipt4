package org.gbif.ipt.web.support.page;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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
