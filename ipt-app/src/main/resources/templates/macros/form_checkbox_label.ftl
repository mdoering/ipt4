<#ftl output_format="HTML">
<label for="${name}" class="form-check-label">
    <#if i18nkey=="">
        ${t(name)}<#t>
    <#else>
        ${t(i18nkey)}<#t>
    </#if>
    <#if requiredField><span class="text-gbif-danger">&#42;</span></#if>
</label>
