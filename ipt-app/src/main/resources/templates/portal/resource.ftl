<#if resource.isDataPackage()>
    <#if resource.coreType?has_content && resource.coreType == "coldp">
        <#include "/portal/resource_col_dp.ftl">
    <#elseif resource.coreType?has_content && resource.isDwcDp()>
        <#include "/portal/resource_dwc_dp.ftl">
    <#else>
        <#include "/portal/resource_dp.ftl">
    </#if>
<#else>
    <#include "/portal/resource_new.ftl">
</#if>
