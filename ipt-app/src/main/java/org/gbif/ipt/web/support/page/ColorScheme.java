package org.gbif.ipt.web.support.page;

/**
 * UI theme colours (hex). M0 always uses the IPT 3 defaults; M1 reads the installation's scheme from the data
 * directory ({@code config/.uiSettings/ipt-color-scheme.properties}).
 */
public record ColorScheme(
    String primaryColor,
    String navbarColor,
    String navbarLinkColor,
    String navbarGbifLogoColor,
    String navbarActiveTabColor,
    String linkColor) {

  public static final ColorScheme DEFAULT =
      new ColorScheme("#61a861", "#78b578", "#ffffff", "#ffffff", "#ffffff", "#4ba2ce");

  /** {@code "#61a861"} to {@code "97,168,97"}, the form the CSS variables in inc/header.ftl expect. */
  public static String toRgb(String hex) {
    return Integer.valueOf(hex.substring(1, 3), 16)
        + ","
        + Integer.valueOf(hex.substring(3, 5), 16)
        + ","
        + Integer.valueOf(hex.substring(5, 7), 16);
  }
}
