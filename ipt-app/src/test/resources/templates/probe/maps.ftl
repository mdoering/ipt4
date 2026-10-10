${stringKeyed.get("a")}|${stringKeyed.b}|<#list stringKeyed?keys as k>${k}=${stringKeyed[k]};</#list>|<#if emptyMap?has_content>full<#else>empty</#if>
