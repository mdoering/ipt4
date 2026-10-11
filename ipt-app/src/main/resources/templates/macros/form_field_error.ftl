<#ftl output_format="HTML">
<#if errorfield=="">
    <@fieldError id="field-error-${name}" cssClass="invalid-feedback list-unstyled field-error my-1" fieldName="${name}"/>
<#else>
    <@fieldError id="field-error-${name}" cssClass="invalid-feedback list-unstyled field-error my-1" fieldName="${errorfield}"/>
</#if>
