<#ftl output_format="HTML">

<nav id="sidebar-content">
    <ul>
        <#list ["basic", "geographic", "taxonomic", "temporal", "keywords", "project", "citation", "other"] as it>
            <li>
                <a class="sidebar-navigation-link <#if it == currentMetadataPage>active</#if>" href="camtrap-metadata-${it}.do?r=${resource.shortname!r!}">
                    ${t("submenu.datapackagemetadata.camtrap.${it}")}<#t>
                </a>
            </li>
        </#list>
    </ul>

    <div class="d-flex align-content-between" style="margin-left: -10px;">
        <input type="submit" value="${t("button.save")?markup_string}" id="save" name="save" class="button btn btn-sm btn-outline-gbif-primary me-1"/><#t>
        <input type="submit" value="${t("button.back")?markup_string}" id="cancel" name="cancel" class="button btn btn-sm btn-outline-secondary"/><#t>
    </div>
</nav>
