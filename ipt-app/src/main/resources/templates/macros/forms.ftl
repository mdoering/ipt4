<#ftl output_format="HTML">

    <#-- The validation errors of a field, as a list. -->
    <#macro fieldError fieldName id="" cssClass="errorMessage" cssStyle="">
        <#if (fieldErrors[fieldName])?has_content>
            <ul<#if id?has_content> id="${id}"</#if> class="${cssClass}"<#if cssStyle?has_content> style="${cssStyle}"</#if>><#list fieldErrors[fieldName] as message><li><span>${message}</span></li></#list></ul><#t>
        </#if>
    </#macro>

    <#-- The value at a property path such as eml.title, as text. -->
    <#function propertyText path>
        <#if !path?matches(r"[A-Za-z_][\w.\[\]]*")>
            <#return "">
        </#if>
        <#local value = ("(" + path + ")!''")?eval>
        <#if value?is_number || value?is_boolean>
            <#return value?c>
        </#if>
        <#return value>
    </#function>

    <#macro input name value="" i18nkey="" errorfield="" type="text" size=-1 disabled=false help="" helpOptions=[] date=false requiredField=false maxlength=-1 withLabel=true tabindex=-1 placeholder="">
        <div>
            <#if withLabel>
                <div class="d-flex text-smaller">
                    <#include "/macros/help_icon.ftl">
                    <#include "/macros/form_field_label.ftl">
                </div>
            </#if>
            <input
                    class="form-control"
                    type="${type}"
                    id="${name}"
                    name="${name}"
                    placeholder="${placeholder}"
                    <#if (tabindex>0)>tabindex="${tabindex}" </#if>
                    value="${value!}"
                    <#if (size>0)>size="${size}"</#if>
                    <#if (maxlength>0)>maxlength="${maxlength}"</#if>
                    <#if disabled>readonly="readonly"</#if>
                    <#if requiredField>required</#if>
            />
            <#include "/macros/form_field_error.ftl">
        </div>
    </#macro>

    <#macro text name value="-99999" i18nkey="" errorfield="" size=40 rows=5 disabled=false help="" requiredField=false minlength=-1 maxlength=-1>
        <div>
            <div class="d-flex text-smaller">
                <#include "/macros/help_icon.ftl">
                <#include "/macros/form_field_label.ftl">
            </div>
            <textarea id="${name}" class="form-control" name="${name}" cols="${size}" rows="${rows}"<#rt>
                      <#if (minlength>0)> minlength="${minlength}"</#if><#t>
                      <#if (maxlength>0)> maxlength="${maxlength}"</#if><#t>
                      <#if requiredField> required</#if><#t>
                      <#if disabled> disabled</#if>><#t>
                      <#if value=="-99999"><#t>
                          ${propertyText(name)}<#t>
                      <#else><#t>
                          ${value}<#t>
                      </#if><#t>
            </textarea><#lt>
            <#include "/macros/form_field_error.ftl">
        </div>
    </#macro>

    <#-- has no label or help icon, and is used exclusively on basic metadata page description textareas -->
    <#macro simpleText name value="-99999" errorfield="" size=40 rows=5 disabled=false requiredField=false minlength=-1 maxlength=-1>
        <div>
            <textarea id="${name}" class="form-control" name="${name}" cols="${size}" rows="${rows}" class="basic"<#rt>
                      <#if (minlength>0)> minlength="${minlength}"</#if><#t>
                      <#if (maxlength>0)> maxlength="${maxlength}"</#if><#t>
                      <#if requiredField> required</#if><#t>
                      <#if disabled> readonly="readonly"</#if>><#t>
                      <#if value=="-99999">${propertyText(name)}<#else>${value}</#if><#t>
            </textarea><#lt>
            <#include "/macros/form_field_error.ftl">
        </div>
    </#macro>

    <#macro textinline name value="-99999" i18nkey="" errorfield="" help="" requiredField=false>
        <div class="textinline">
            <h5 class="py-2 mb-0 text-gbif-header fw-400">
                <#include "/macros/help_icon.ftl">
                <span>${t(name)}<#if requiredField> <span class="text-gbif-danger">&#42;</span></#if></span>
            </h5>
        </div>
    </#macro>

    <#macro link name value="" href="" class="" i18nkey="" help="" errorfield="">
        <div>
            <#include "/macros/help_icon.ftl">
            <a id="${name}" name="${name}" class="${class}" href="${href}">${t(value)}</a>
        </div>
    </#macro>

    <#macro select name options value="" i18nkey="" errorfield="" size=1 disabled=false help="" includeEmpty=false javaGetter=true requiredField=false tabindex=-1 compareValues=false withLabel=true>
        <div>
            <#if withLabel>
            <div class="d-flex text-smaller">
                <#include "/macros/help_icon.ftl">
                <#include "/macros/form_field_label.ftl">
            </div>
            </#if>
            <select name="${name}" id="${name}" size="${size}" class="form-select" <#if (tabindex>0)>tabindex="${tabindex}" </#if> <#if disabled>readonly="readonly"</#if> <#if requiredField>required</#if>>
                <#if includeEmpty>
                    <option value="" <#if (value!"")==""> selected="selected"</#if>></option>
                </#if>
                <#list options?keys as val>
                    <option value="${val}" <#if !compareValues && (value!"") == "" + val> selected="selected"</#if> <#if compareValues && (value!"") == "" + options[val]>selected</#if> >
                        <#if javaGetter>${t(options.get(val))}<#t><#else>${t(options[val])}</#if>
                    </option>
                </#list>
            </select>
            <#include "/macros/form_field_error.ftl">
        </div>
    </#macro>

    <#macro selectList name options objValue objTitle value="" i18nkey="" errorfield="" size=1 disabled=false help="" includeEmpty=false requiredField=false withLabel=true>
        <div>
            <#if withLabel>
                <div class="d-flex text-smaller">
                    <#include "/macros/help_icon.ftl">
                    <#include "/macros/form_field_label.ftl">
                </div>
            </#if>
            <select name="${name}" size="${size}"<#if disabled> disabled="disabled"</#if> id="${name}" class="form-select">
                <#if includeEmpty>
                    <option value=""></option>
                </#if>
                <#if options?is_enumerable>
                    <#list options as option>
                        <#assign optionValue = (option[objValue])!"">
                        <option value="${optionValue}"<#if optionValue == value> selected="selected"</#if>>${(option[objTitle])!}</option>
                    </#list>
                </#if>
            </select>
            <#include "/macros/form_field_error.ftl">
        </div>
    </#macro>

    <#macro checkbox name i18nkey="" errorfield="" disabled=false value=false help="" requiredField=false>
        <div class="form-check">
            <input type="checkbox" name="${name}" value="true"<#if value?is_boolean && value || value?is_string && value == "true"> checked="checked"</#if><#if disabled> disabled="disabled"</#if> id="${name}" class="form-check-input"/>
            <#-- A checkbox sends only true (when checked). -->
            <#-- Send false additionaly. It is picked when the first value is absent otherwise ignored. -->
            <input type="hidden" name="${name}" value="false">
            <div class="d-flex">
                <#include "/macros/form_checkbox_label.ftl">
                <#include "/macros/help_icon.ftl">
            </div>
            <#include "/macros/form_field_error.ftl">
        </div>
    </#macro>

    <#macro readonly name i18nkey value size=-1 help="" errorfield="" requiredField=false>
        <div class="d-flex text-smaller">
            <#include "/macros/help_icon.ftl">
            <#include "/macros/form_field_label.ftl">
        </div>
        <input type="text" class="form-control" value="${value}" <#if (size>0)>size="${size}"</#if> readonly="readonly"/>
        <#include "/macros/form_field_error.ftl">
    </#macro>

    <#macro label i18nkey help="" requiredField=false>
        <div>
            <#include "/macros/help_icon.ftl">
            <label class="form-label">
                ${t(i18nkey)} <#if requiredField><span class="text-gbif-danger">&#42;</span></#if>
            </label>
            <#nested>
        </div>
    </#macro>

    <#--
      In the textWithFormattedLink macro, each word in view source will appear on a separate line. This fix this issue
      http://code.google.com/p/gbif-providertoolkit/issues/detail?id=856 the <#t> trim directive is used to ensure that
      the word list gets displayed as a complete line instead.
    -->
    <#macro textWithFormattedLink text>
    <#-- replace less than and greater than characters -->
        <#assign sanitized = text?replace("<", "&lt;")?replace(">", "&gt;")>
        <#assign words = sanitized?word_list>
        <#list words as x>
            <#assign res = x?matches("(http(s)?|ftp)://(([\\w-]+\\.)?)+[\\w-]+(:\\d+)?+(/[\\w- ./-?%&=]*)?")>
            <#assign flag=false>
            <#list res as m>
                <#if x?contains(m)><a href="${m}">${x}</a> <#t><#assign flag = true><#break></#if>
            </#list>
            <#if flag==false>${x}</#if> <#t>
        </#list>
    </#macro>

    <#macro showMore text maxLength>
        <#if (text?length>maxLength)>
            <div id="visibleContent">
                <@textWithFormattedLink (text)?substring(0,maxLength)/>... <a id="showMore" href="">${t('basic.showMore')}</a>
            </div>
            <div id="hiddenContent" style="display: none">
                <@textWithFormattedLink text/>
                <a id="showLess" href="">${t('basic.showLess')}</a>
            </div>
        <#else>
            <@textWithFormattedLink text/>
        </#if>
    </#macro>

    <#-- Adds the CC icon. Only CC-BY-NC, CC-BY, and CC0 are supported -->
    <#macro licenseLogoClass rights>
        <#if rights?contains("CC-BY-NC")>
            <a rel="license" id="cc_by_nc" class="cc_logo cc_by_nc"
               href="http://creativecommons.org/licenses/by-nc/4.0/legalcode"
               title="Creative Commons Attribution Non Commercial (CC-BY-NC) 4.0 License">
                <img src="${baseURL}/images/icons/cc-by-nc.svg" alt="Sample Image">
            </a>
        <#elseif rights?contains("CC-BY")>
            <a rel="license" id="cc_by" class="cc_logo cc_by"
               href="http://creativecommons.org/licenses/by/4.0/legalcode"
               title="Creative Commons Attribution (CC-BY) 4.0 License">
                <img src="${baseURL}/images/icons/cc-by.svg" alt="Sample Image">
            </a>
        <#elseif rights?contains("CC0")>
            <a rel="license" id="cc_zero" class="cc_logo cc_zero"
               href="http://creativecommons.org/publicdomain/zero/1.0/legalcode"
               title="Creative Commons CCZero (CC0) 1.0 License">
                <img src="${baseURL}/images/icons/cc-by-nc.svg" alt="Sample Image">
            </a>
        </#if>
    </#macro>
