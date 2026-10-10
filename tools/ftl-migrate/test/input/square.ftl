[#ftl output_format="HTML"]
[#include "/WEB-INF/pages/inc/menu.ftl"/]
    <FORM method='POST'
          action="${baseURL}/login.do">
[@s.text name="portal.login"/]
</FORM>
