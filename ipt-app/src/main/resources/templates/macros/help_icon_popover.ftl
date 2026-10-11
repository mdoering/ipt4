<#ftl output_format='HTML'>
<div class='info'>
    <#if help=='i18n'>
        <#if i18nkey==''>
            ${t('${name}.help')?markup_string}<#t>
        <#else>
            ${t('${i18nkey}.help')?markup_string}<#t>
        </#if>
    <#else>
        <#if help?has_content>
            ${help}
        </#if>
    </#if>
    <#if (helpOptions?? && helpOptions?size>0)>
        <p>${t('help.options')}</p>
        <ol>
            <#list helpOptions?keys as val>
                <li>
                    <a href='#' class='helpOptionLink inputName-${name} inputValue-${val}'>${helpOptions[val]}</a>
                </li>
            </#list>
        </ol>
    </#if>
</div>
