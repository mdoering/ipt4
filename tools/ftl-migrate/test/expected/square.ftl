[#ftl output_format="HTML"]
[#include "/inc/menu.ftl"/]
    <FORM method='POST'
          action="${baseURL}/login.do">
[@csrfField/]
${t("portal.login")}[#t]
</FORM>
