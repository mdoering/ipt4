<#ftl output_format="HTML">
A:${t("menu.about")}
B:${t("basic.createdByOn", "Ann", "2020")}
X:${t("basic.createdByOn", "<i>Ann</i>", "2020")}
<#assign who><a href="x?a=1&amp;b=2">${"<Ann>"}</a></#assign>
M:${t("basic.createdByOn", who, t("menu.home"))}
N:${t("basic.createdByOn", 1234, 0.5)}|${1234}
C:${t("no.such.key")}
F:${t("basic.createdByOn", "Ann", "2020")?markup_string}
K:${t("manage.overview.title." + "label")}
G:${t("about.notRegistered")}
