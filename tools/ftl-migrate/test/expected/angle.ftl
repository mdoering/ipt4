<#include "/inc/header.ftl">
<#if (currentUser)??>${currentUser.email}</#if>
<#if RequestParameters.showAll??>all</#if>
<form action="${baseURL}/manage/x.do" method="post" id="f">
<@csrfField/>
  <input name="a"/>
</form>
<form action="search" method="get"></form>
<a href="%{foo}">x</a>
