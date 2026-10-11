<#ftl output_format="HTML">
<#include "/inc/header.ftl">
<title>${t("title")}</title>
<#assign currentMenu = "manage"/>
<#include "/inc/menu.ftl">
<#include "/macros/forms.ftl"/>
<#include "/macros/popover.ftl">
<#include "/macros/resourcesTable.ftl"/>
<script src="${baseURL}/js/jquery/jquery.dataTables-1.13.6.min.js"></script>
<script src="${baseURL}/js/jquery/dataTables.responsive-2.5.0.min.js"></script>

<@resourcesTable resources=resources shownPublicly=false numResourcesShown=10 sEmptyTable="manage.home.resources.none" columnToSortOn=6 sortOrder="desc"/>

<div class="container">
    <#include "/inc/action_alerts.ftl">
</div>

<div class="container-fluid border-bottom">
    <div class="container bg-body border rounded-2 mb-4">
        <div class="container my-3 p-3">
            <div class="text-center fs-smaller">
                ${t("menu.manage.short")}<#t>
            </div>

            <div class="text-center">
                <h1 class="pb-2 mb-0 pt-2 text-gbif-header fs-2 fw-normal">
                    ${t("manage.home.title")}<#t>
                </h1>

                <#if (resources?size>0)>
                    <div class="text-smaller text-gbif-primary">
                        ${t("portal.home.resources.available", resourcesSize)}
                    </div>
                <#else>
                    <div class="text-smaller text-gbif-danger">
                        ${t("manage.home.no.resources")}<#t>
                    </div>
                </#if>

                <#if (organisations?size==0)>
                    <div class="text-smaller text-gbif-danger">
                        ${t("manage.resource.create.forbidden")}<#t>
                    </div>
                </#if>

                <#if (organisations?size>0)>
                    <div class="mt-2">
                        <a href="${baseURL}/manage/create.do" class="btn btn-sm btn-outline-gbif-primary top-button">${t("button.create.new")}</a>
                    </div>
                </#if>
            </div>
        </div>
    </div>
</div>

<main class="container main-content-container border rounded my-4">
    <div class="py-md-4 py-3 px-md-5 px-3">
        <div id="tableContainer" class="resource-table text-smaller pt-2"></div>
    </div>
</main>

<#include "/inc/footer.ftl">
