<#ftl output_format="HTML">

<nav id="sidebar-content">
    <ul>
        <#list ["basic", "contacts", "acknowledgements", "geocoverage", "taxcoverage", "tempcoverage", "additionalDescription", "keywords", "project", "methods", "citations", "collections", "physical", "additional"] as it>
            <li>
                <a class="sidebar-navigation-link <#if it == currentMetadataPage>active</#if>" href="metadata-${it}.do?r=${resource.shortname!r!}">
                    ${t("submenu.${it}")}<#t>
                </a>
            </li>
        </#list>
    </ul>

    <div class="d-flex align-content-between" style="margin-left: -10px;">
        <input type="submit" value="${t("button.save")?markup_string}" id="save" name="save" class="button btn btn-sm btn-outline-gbif-primary me-1"/><#t>
        <button type="button" class="btn btn-sm btn-outline-secondary" onclick="window.history.back();">
            ${t("button.back")}<#t>
        </button>
    </div>
</nav>
