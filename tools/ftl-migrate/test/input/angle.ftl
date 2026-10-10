<#include "/WEB-INF/pages/inc/header.ftl">
<#if (Session.curr_user)??>${Session.curr_user.email}</#if>
<#if Parameters.showAll??>all</#if>
<form action="${baseURL}/manage/x.do" method="post" id="f">
  <input name="a"/>
</form>
<form action="search" method="get"></form>
<a href="%{foo}">x</a>
