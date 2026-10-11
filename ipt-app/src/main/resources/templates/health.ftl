<#ftl output_format="HTML">
<#include "/inc/header.ftl">
<title>${t("portal.health.title")}</title>
<#assign currentMenu = "health"/>
<#include "/inc/menu.ftl">

<div class="container-fluid border-bottom">
    <div class="container bg-body border rounded-2 mb-4">
        <div class="container my-3 p-3">
            <div class="text-center">
                <div class="text-center fs-smaller">
                    <span>IPT</span>
                </div>

                <h1 class="pb-2 mb-0 pt-2 text-gbif-header fs-2 fw-normal">
                    ${t('portal.health.title')}<#t>
                </h1>

                <#assign aDateTime = .now>
                <div class="text-smaller text-gbif-primary mb-2">
                    ${aDateTime?date?string.long}
                </div>
            </div>
        </div>
    </div>
</div>

<main class="container main-content-container">
    <div class="row g-3 mt-1">
        <div class="col-lg-6">
            <div class="card health-status-item">
                <div class="card-header">
                    ${t("portal.health.network")}<#t>
                </div>
                <div class="card-body">
                    <div class="table-responsive">
                        <table class="table table-sm">
                            <tbody>
                            <tr>
                                <td>${t("portal.health.network.registry.url")}</td>
                                <td class="text-end"><a href="${networkRegistryURL!}">${networkRegistryURL!}</a></td>
                            </tr>
                            <tr>
                                <td>${t("portal.health.network.registry.access")}</td>
                                <td class="text-end ${networkRegistry?string("text-gbif-primary", "text-gbif-danger")}">
                                    <#if networkRegistry>
                                        ${t("portal.health.operational")}<#t>
                                    <#else>
                                        ${t("portal.health.failed")}<#t>
                                    </#if>
                                </td>
                            </tr>
                            <tr>
                                <td>${t("portal.health.network.repository.url")}</td>
                                <td class="text-end"><a href="${networkRepositoryURL!}">${networkRepositoryURL!}</a>
                                </td>
                            </tr>
                            <tr>
                                <td>${t("portal.health.network.repository.access")}</td>
                                <td class="text-end ${networkRepository?string("text-gbif-primary", "text-gbif-danger")}">
                                    <#if networkRepository>
                                        ${t("portal.health.operational")}<#t>
                                    <#else>
                                        ${t("portal.health.failed")}<#t>
                                    </#if>
                                </td>
                            </tr>
                            <tr>
                                <td>${t("portal.health.network.public.access.url")}</td>
                                <td class="text-end"><a href="${networkPublicAccessURL!}">${networkPublicAccessURL!}</a>
                                </td>
                            </tr>
                            <tr>
                                <td>${t("portal.health.network.public.access.access")}</td>
                                <td class="text-end ${networkPublicAccess?string("text-gbif-primary", "text-gbif-danger")}">
                                    <#if networkPublicAccess>
                                        ${t("portal.health.operational")}<#t>
                                    <#else>
                                        ${t("portal.health.failed")}<#t>
                                    </#if>
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-lg-6">
            <div class="card h-100 health-status-item">
                <div class="card-header">
                    ${t("portal.health.disk.usage")}<#t>
                </div>
                <div class="card-body">
                    <div class="table-responsive">
                        <table class="table table-sm">
                            <tbody>
                            <tr>
                                <td>${t("portal.health.disk.usage.used.ratio")}</td>
                                <td>
                                    <div class="progress">
                                        <div class="progress-bar <#if diskUsedRatio gt 80>bg-gbif-danger<#else>bg-gbif-primary</#if>"
                                             role="progressbar" style="width: ${diskUsedRatio!}%;"
                                             aria-valuenow="${diskUsedRatio!}" aria-valuemin="0"
                                             aria-valuemax="100">${diskUsedRatio!}%
                                        </div>
                                    </div>
                                </td>
                            </tr>
                            <tr>
                                <td>${t("portal.health.disk.usage.total.size")}</td>
                                <td class="text-end">${hrDiskTotal!}</td>
                            </tr>
                            <tr>
                                <td>${t("portal.health.disk.usage.used.size")}</td>
                                <td class="text-end">${hrDiskUsed!}</td>
                            </tr>
                            <tr>
                                <td>${t("portal.health.disk.usage.free.size")}</td>
                                <td class="text-end">${hrDiskFree!}</td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="row g-3 mt-1 mb-3">
        <div class="col-lg-6">
            <div class="card health-status-item">
                <div class="card-header">
                    ${t("portal.health.file.permissions")}<#t>
                </div>
                <div class="card-body">
                    <div class="table-responsive">
                        <table class="table table-sm">
                            <tbody>
                            <tr>
                                <td>${t("portal.health.file.permissions.config.directory")}</td>
                                <td class="text-end">
                                    <#if readConfigDir>
                                        <span class="text-gbif-primary">${t("portal.health.file.permissions.operational")}</span>
                                    <#else>
                                        <span class="text-gbif-danger">${t("portal.health.file.permissions.no.access")}</span>
                                    </#if>
                                </td>
                            </tr>
                            <tr>
                                <td>${t("portal.health.file.permissions.logs.directory")}</td>
                                <td class="text-end">
                                    <#if readLogDir && writeLogDir>
                                        <span class="text-gbif-primary">${t("portal.health.file.permissions.operational")}</span>
                                    <#elseif readLogDir && !writeLogDir>
                                        <span class="text-gbif-danger">${t("portal.health.file.permissions.read.only")}</span>
                                    <#else>
                                        <span class="text-gbif-danger">${t("portal.health.file.permissions.no.access")}</span>
                                    </#if>
                            </tr>
                            <tr>
                                <td>${t("portal.health.file.permissions.tmp.directory")}</td>
                                <td class="text-end">
                                    <#if readTmpDir && writeTmpDir>
                                        <span class="text-gbif-primary">${t("portal.health.file.permissions.operational")}</span>
                                    <#elseif readTmpDir && !writeTmpDir>
                                        <span class="text-gbif-danger">${t("portal.health.file.permissions.read.only")}</span>
                                    <#else>
                                        <span class="text-gbif-danger">${t("portal.health.file.permissions.no.access")}</span>
                                    </#if>
                            </tr>
                            <tr>
                                <td>${t("portal.health.file.permissions.resources.directory")}</td>
                                <td class="text-end">
                                    <#if readResourcesDir && writeResourcesDir && readSubResourcesDir && writeSubResourcesDir>
                                        <span class="text-gbif-primary">${t("portal.health.file.permissions.operational")}</span>
                                    <#elseif readResourcesDir && writeResourcesDir && readSubResourcesDir && !writeSubResourcesDir>
                                        <span class="text-gbif-danger">${t("portal.health.file.permissions.at.least.one.read.only")}</span>
                                    <#elseif readResourcesDir && writeResourcesDir && !readSubResourcesDir>
                                        <span class="text-gbif-danger">${t("portal.health.file.permissions.at.least.one.no.access")}</span>
                                    <#elseif readResourcesDir && !writeResourcesDir>
                                        <span class="text-gbif-danger">${t("portal.health.file.permissions.read.only")}</span>
                                    <#else>
                                        <span class="text-gbif-danger">${t("portal.health.file.permissions.no.access")}</span>
                                    </#if>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-lg-6">
            <div class="card health-status-item">
                <div class="card-header">
                    ${t("portal.health.system")}<#t>
                </div>
                <div class="card-body">
                    <div class="table-responsive">
                        <table class="table table-sm">
                            <tbody>
                            <tr>
                                <td>${t("portal.health.system.os.version")}</td>
                                <td class="text-end">
                                    <#if !loggedIn>
                                        ${t("portal.health.please.log.in")}<#t>
                                    <#else>
                                        ${osName!} ${osVersion!}
                                    </#if>
                                </td>
                            </tr>
                            <tr>
                                <td>${t("portal.health.system.java.version")}</td>
                                <td class="text-end">
                                    <#if !loggedIn>
                                        ${t("portal.health.please.log.in")}<#t>
                                    <#else>
                                        ${javaVersion!}
                                    </#if>
                                </td>
                            </tr>
                            <tr>
                                <td>${t("portal.health.system.app.server.version")}</td>
                                <td class="text-end">
                                    <#if !loggedIn>
                                        ${t("portal.health.please.log.in")}<#t>
                                    <#else>
                                        ${appServerVersion!}
                                    </#if>
                                </td>
                            </tr>
                            <tr>
                                <td>${t("portal.health.system.ipt.mode")}</td>
                                <td class="text-end">
                                    <#if !loggedIn>
                                        ${t("portal.health.please.log.in")}<#t>
                                    <#else>
                                        ${iptMode!}
                                    </#if>
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>

<#include "/inc/footer.ftl">
