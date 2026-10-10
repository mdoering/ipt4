<#ftl output_format="HTML">
A:<@s.text name="menu.about"/>
B:<@s.text name="basic.createdByOn"><@s.param>Ann</@s.param><@s.param>2020</@s.param></@s.text>
C:<@s.text name="no.such.key"/>
D:<@s.text name="no.such.key">Fallback</@s.text>
E:<@s.text var="home" name="menu.home"/>[${home}]
F:<@s.text name="basic.createdByOn" escapeHtml=true><@s.param>Ann</@s.param><@s.param>2020</@s.param></@s.text>
G:<@s.text name="about.notRegistered"/>
H:<@csrfField/>
