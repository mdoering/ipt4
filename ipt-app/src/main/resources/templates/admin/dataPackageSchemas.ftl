    <#include "/inc/header.ftl">
    <title>${t("admin.home.manageDataPackageSchemas")}</title>
    <script src="${baseURL}/js/jconfirmation.jquery.js"></script>

    <#macro installedSchemaItem ds>
        <div class="d-flex flex-column col-lg-6 col-12 px-2">
            <div class="data-package-schema-item border rounded-2 d-flex flex-column overflow-hidden w-100 flex-auto mb-3">
                <div class="d-flex flex-justify-between px-4 pt-4 pb-0">
                    <div class="me-2">
                        <h4 class="d-flex fs-regular mt-1 mb-0">
                            ${ds.title}
                        </h4>
                        <p class="color-fg-muted mb-0 fs-smaller-2">
                            ${ds.issued?date?string["d MMMM yyyy"]}
                        </p>
                    </div>
                    <div class="fs-smaller pt-1">
                        <span class="schema-status schema-status-installed">
                            v${ds.version!}
                        </span>

                        <#if ds.latest>
                            <span class="schema-status schema-status-installed">
                                ${t("admin.dataPackages.dataPackage.latest")}<#t>
                            </span>
                        <#else>
                            <span class="schema-status schema-status-update">
                                ${t("admin.dataPackages.dataPackage.updateAvailable")}<#t>
                            </span>
                        </#if>
                    </div>
                </div>
                <div class="d-flex flex-column flex-auto flex-justify-between">
                    <div class="d-flex flex-justify-between flex-items-center text-break pt-2 pb-0 px-4 fs-smaller">
                        <div>
                            <#if !ds.latest>
                                <span class="text-gbif-danger">${t("admin.dataPackages.version.warning")}</span><br>
                            </#if>
                            ${ds.description!?truncate(300)}
                        </div>
                    </div>
                    <div class="d-flex pt-2 pb-4 px-4">
                        <a href="dataPackage.do?id=${ds.identifier?url}" title="" class="action-link-button action-link-button-primary">
                            ${t("button.view")}<#t>
                        </a>
                        <#if !ds.latest && ds.updatable>
                            <form action='updateDataPackage.do' method='post'>
<@csrfField/>
                                <input type='hidden' name='id' value='${ds.identifier}' />

                                <button type="submit" value="Update" id="update" name="update" class="confirm action-link-button action-link-button-primary">
                                    ${t("button.update")}<#t>
                                </button>
                            </form>
                        </#if>
                    </div>
                </div>
            </div>
        </div>
    </#macro>

    <#assign currentMenu = "admin"/>
    <#include "/inc/menu.ftl">

    <#assign schemasCount=0/>
    <#assign updateRequired=false/>
    <#list schemas as ds>
        <#assign schemasCount=schemasCount+1/>
        <#if !ds.isLatest()>
            <#assign updateRequired=true/>
        </#if>
    </#list>

    <div class="container px-0">
        <#include "/inc/action_alerts.ftl">
    </div>

    <div class="container-fluid bg-body border-bottom">
        <div class="container bg-body border rounded-2 mb-4">
            <div class="container my-3 p-3">
                <div class="text-center">
                    <div class="fs-smaller">
                        <nav style="--bs-breadcrumb-divider: url(&#34;data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='8' height='8'%3E%3Cpath d='M2.5 0L1 1.5 3.5 4 1 6.5 2.5 8l4-4-4-4z' fill='currentColor'/%3E%3C/svg%3E&#34;);" aria-label="breadcrumb">
                            <ol class="breadcrumb justify-content-center mb-0">
                                <li class="breadcrumb-item"><a href="${baseURL}/admin/">${t("breadcrumb.admin")}</a></li>
                                <li class="breadcrumb-item">${t("breadcrumb.admin.dataPackageSchemas")}</li>
                            </ol>
                        </nav>
                    </div>

                    <h1 class="pb-2 mb-0 pt-2 text-gbif-header fs-2 fw-normal">
                        ${t("admin.dataPackageSchemas.title")}<#t>
                    </h1>

                    <div class="text-smaller">
                        <#if schemasCount=0>
                            <span class="text-gbif-danger mt-3">
                                ${t("admin.dataPackages.no.schemas.installed")}<#t>
                            </span>
                        <#else>
                            <#if upToDate>
                                <span class="text-gbif-primary">${t("admin.dataPackages.upToDate")}</span>
                            <#elseif iptReinstallationRequired>
                                <span class="text-gbif-danger">${t("admin.dataPackages.iptReinstallationRequired")}</span>
                            <#else>
                                <span class="text-gbif-danger">${t("admin.dataPackages.not.upToDate")}</span>
                            </#if>
                        </#if>
                    </div>

                    <div class="mt-2">
                        <a href="${baseURL}/admin/" class="btn btn-sm btn-outline-secondary mt-1 me-xl-1 top-button">
                            ${t("button.back")}<#t>
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <main class="container main-content-container">
        <div class="mt-3 mb-0 p-3">
            <h5 class="pb-2 mb-0 pt-2 text-gbif-header-2 fw-400">
                ${t("admin.dataPackages.installed")}<#t>
            </h5>
        </div>

        <div>
            <div class="flex-auto">
                <div class="d-flex flex-items-stretch flex-wrap">
                    <#list schemas as ds>
                        <@installedSchemaItem ds/>
                    </#list>
                </div>
            </div>
        </div>
    </main>

    <#include "/inc/footer.ftl">
