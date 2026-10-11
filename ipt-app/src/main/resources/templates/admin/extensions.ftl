    <#include "/inc/header.ftl">
    <title>${t("admin.home.manageExtensions")}</title>
    <script src="${baseURL}/js/jconfirmation.jquery.js"></script>

    <script>
        $(document).ready(function () {
            $('.confirm').jConfirmAction({
                titleQuestion: "${t("basic.confirm")}",
                question: "${t('admin.extension.update.confirm')}",
                yesAnswer: "${t('basic.yes')}",
                cancelAnswer: "${t('basic.no')}",
                buttonType: "primary",
                baseUrl: "${baseURL}",
                logo: "success"
            });
        });

        $("#synchronise").on("click", displayProcessing);
    </script>

    <#macro installedExtensionItem ext >
        <div class="d-flex flex-column col-lg-4 col-md-6 col-sm-6 col-12 px-2">
            <div class="extension-item border rounded-2 d-flex flex-column overflow-hidden w-100 flex-auto mb-3">
                <div class="d-flex flex-justify-between px-4 pt-4 pb-0">
                    <div>
                        <h4 class="d-flex fs-regular mt-1 mb-0">
                            ${ext.title}
                        </h4>
                        <p class="color-fg-muted mb-0 fs-smaller-2">
                            ${ext.rowType!}
                        </p>
                        <#if ext.isLatest()>
                            <p class="text-gbif-primary mb-0 fs-smaller-2">
                                <#if ext.issued??>${ext.issued?date?string["d MMMM yyyy"]}<#else>-</#if>
                            </p>
                        <#else>
                            <p class="text-gbif-danger mb-0 fs-smaller-2">
                                <#if ext.issued??>${ext.issued?date?string["d MMMM yyyy"]}<#else>-</#if>
                            </p>
                        </#if>
                    </div>
                </div>
                <div class="d-flex flex-column flex-auto flex-justify-between">
                    <div class="d-flex flex-justify-between flex-items-center text-break pt-2 pb-0 px-4 fs-smaller">
                        <div>
                            <#if !ext.isLatest()>
                                <span class="text-gbif-danger">${t("admin.extension.version.warning.short")}</span><br>
                            </#if>
                            ${ext.description!?truncate(300)}
                        </div>
                    </div>
                    <div class="d-flex pt-2 pb-4 px-4">
                        <a href="extension.do?id=${ext.rowType?url}" title="" class="action-link-button action-link-button-primary">
                            ${t("button.view")}<#t>
                        </a>

                        <#if !ext.isLatest()>
                            <form action='updateExtension.do' method='post'>
<@csrfField/>
                                <input type='hidden' name='id' value='${ext.rowType}' />

                                <button type="submit" value="Update" id="update" name="update" class="confirm action-link-button action-link-button-primary">
                                    ${t("button.update")}<#t>
                                </button>
                            </form>
                        </#if>

                        <form action='extension.do' method='post'>
<@csrfField/>
                            <input type='hidden' name='id' value='${ext.rowType}' />

                            <button type="submit" value="Delete" id="delete" name="deleteFlag" class="action-link-button action-link-button-danger">
                                ${t("button.remove")}<#t>
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </#macro>

    <#macro installedVocabularyItem vocab>
        <div class="d-flex flex-column col-lg-4 col-md-6 col-sm-6 col-12 px-2">
            <div class="extension-item border rounded-2 d-flex flex-column overflow-hidden w-100 flex-auto mb-3">
                <div class="d-flex flex-justify-between px-4 pt-4 pb-0">
                    <div>
                        <h4 class="d-flex fs-regular mt-1 mb-0">
                            ${vocab.title}
                        </h4>
                        <p class="color-fg-muted mb-0 fs-smaller-2">
                            ${vocab.uriString}
                        </p>
                        <#if vocab.isLatest()>
                            <p class="text-gbif-primary mb-0 fs-smaller-2">
                                <#if vocab.issued??>${vocab.issued?date?string["d MMMM yyyy"]}<#else>-</#if>
                            </p>
                        <#else>
                            <p class="text-gbif-danger mb-0 fs-smaller-2">
                                <#if vocab.issued??>${vocab.issued?date?string["d MMMM yyyy"]}<#else>-</#if>
                            </p>
                        </#if>
                    </div>
                </div>
                <div class="d-flex flex-column flex-auto flex-justify-between">
                    <div class="d-flex flex-justify-between flex-items-center text-break pt-2 pb-0 px-4 fs-smaller">
                        <div>
                            <#if !vocab.isLatest()>
                                <span class="text-gbif-danger">${t("admin.extension.version.warning.short")}</span><br>
                            </#if>
                            ${vocab.description!?truncate(300)}
                        </div>
                    </div>
                    <div class="d-flex pt-2 pb-4 px-4">
                        <a href="vocabulary.do?id=${vocab.uriString?url}" title="" class="action-link-button action-link-button-primary">
                            ${t("button.view")}<#t>
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </#macro>

    <#macro newExtensionItem ext>
        <div class="d-flex flex-column col-lg-4 col-md-6 col-sm-6 col-12 px-2">
            <div class="extension-item border rounded-2 d-flex flex-column overflow-hidden w-100 flex-auto mb-3">
                <div class="d-flex flex-justify-between px-4 pt-4 pb-0">
                    <div>
                        <h4 class="d-flex fs-regular mt-1 mb-0">
                            ${ext.title}
                        </h4>
                        <p class="color-fg-muted mb-0 fs-smaller-2">
                            ${ext.rowType!}
                        </p>
                        <p class="text-gbif-primary mb-0 fs-smaller-2">
                            <#if ext.issued??>${ext.issued?date?string["d MMMM yyyy"]}<#else>-</#if>
                        </p>
                    </div>
                </div>
                <div class="d-flex flex-column flex-auto flex-justify-between">
                    <div class="d-flex flex-justify-between flex-items-center text-break pt-2 pb-0 px-4 fs-smaller">
                        ${ext.description!?truncate(300)}
                    </div>
                    <div class="d-flex pt-2 pb-4 px-4">
                        <form action='extension.do' method='post'>
<@csrfField/>
                            <input type='hidden' name='url' value='${ext.url}' />

                            <button type="submit" value="Install" id="install" name="install" class="action-link-button action-link-button-primary">
                                ${t("button.install")}<#t>
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </#macro>

    <#assign numberOfCoreExtensions=0>
    <#assign numberOfNonCoreExtensions=0>

    <#list extensions! as ext>
        <#if ext.core>
            <#assign numberOfCoreExtensions=numberOfCoreExtensions+1>
        <#else>
            <#assign numberOfNonCoreExtensions=numberOfNonCoreExtensions+1>
        </#if>
    </#list>

    <#assign currentMenu = "admin"/>
    <#include "/inc/menu.ftl">

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
                                <li class="breadcrumb-item">${t("breadcrumb.admin.extensions")}</li>
                            </ol>
                        </nav>
                    </div>

                    <h1 class="pb-2 mb-0 pt-2 text-gbif-header fs-2 fw-normal">
                        ${t("admin.home.manageExtensions")}<#t>
                    </h1>

                    <div class="text-smaller">
                        <#if numberOfCoreExtensions == 0 && numberOfNonCoreExtensions == 0>
                            <span class="text-gbif-danger">${t("admin.extensions.noneInstalled")}</span>
                        <#elseif upToDate>
                            <span class="text-gbif-primary">${t("admin.extensions.upToDate")}</span>
                        <#else>
                            <span class="text-gbif-danger">${t("admin.extensions.not.upToDate")}</span>
                        </#if>
                    </div>

                    <div class="mt-2">
                        <a href="${baseURL}/admin/" class="btn btn-sm btn-outline-secondary top-button">
                            ${t("button.cancel")}<#t>
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <main class="container main-content-container">
        <div class="my-3 p-3">
            <h5 class="pb-2 mb-2 pt-2 text-gbif-header-2 fw-400">
                ${t("admin.extension.coreTypes")}<#t>
            </h5>

            <p class="mb-0">
                ${t("admin.extension.no.coreTypes.installed.help", cfg.registryUrl)}
            </p>

            <#if numberOfCoreExtensions=0>
                <div class="callout callout-warning mb-0">
                    ${t("admin.extension.no.coreTypes.installed")}<#t>
                    ${t("admin.extension.no.coreTypes.installed.debug", cfg.registryUrl)}
                </div>
            </#if>
        </div>

        <div class="">
            <div class="flex-auto">
                <div class="d-flex flex-items-stretch flex-wrap">
                    <#list extensions as ext>
                        <#if ext.core>
                            <@installedExtensionItem ext/>
                        </#if>
                    </#list>
                </div>
            </div>
        </div>

        <div class="my-3 p-3">
            <h5 class="pb-2 mb-2 pt-2 text-gbif-header-2 fw-400">
                ${t("extension.synchronise.title")}<#t>
            </h5>

            <p>
                ${t("admin.extensions.synchronise.help")}<#t>
            </p>

            <form action='extensions.do' method='post'>
<@csrfField/>
                <input type="hidden" name="synchronise" value="true"/>
                <div class="col-12 mt-2">
                    <button id="synchronise" type="submit" class="action-link-button action-link-button-primary">
                        <svg class="overview-action-button-icon" viewBox="0 0 24 24">
                            <path d="m19 8-4 4h3c0 3.31-2.69 6-6 6-1.01 0-1.97-.25-2.8-.7l-1.46 1.46C8.97 19.54 10.43 20 12 20c4.42 0 8-3.58 8-8h3l-4-4zM6 12c0-3.31 2.69-6 6-6 1.01 0 1.97.25 2.8.7l1.46-1.46C15.03 4.46 13.57 4 12 4c-4.42 0-8 3.58-8 8H1l4 4 4-4H6z"></path>
                        </svg>
                        ${t("button.synchronise")}<#t>
                    </button>
                    <#if lastSynchronised??>
                    <span class="fs-smaller">
                        ${t("extension.last.synchronised", lastSynchronised?datetime?string["d MMMM yyyy HH:mm"])}
                    </span>
                    </#if>
                </div>
            </form>
        </div>

        <div class="my-3 p-3">
            <h5 class="pb-2 mb-2 pt-2 text-gbif-header-2 fw-400">
                ${t("admin.extension.extensions")}<#t>
            </h5>

            <p class="mb-0">
                ${t("admin.extension.no.extensions.installed.help", cfg.registryUrl)}
            </p>

            <#if numberOfNonCoreExtensions=0>
                <div class="callout callout-warning mb-0">
                    ${t("admin.extension.no.extensions.installed")}<#t>
                </div>
            </#if>
        </div>
        <div>
            <div class="flex-auto">
                <div class="d-flex flex-items-stretch flex-wrap">
                    <#list extensions as ext>
                        <#if !ext.core>
                            <@installedExtensionItem ext/>
                        </#if>
                    </#list>
                </div>
            </div>
        </div>

        <div class="my-3 p-3">
            <h5 class="pb-2 mb-2 pt-2 text-gbif-header-2 fw-400">
                ${t("admin.extension.vocabularies")}<#t>
            </h5>
        </div>
        <div>
            <div class="flex-auto">
                <div class="d-flex flex-items-stretch flex-wrap">
                    <#list vocabularies as vocab>
                        <@installedVocabularyItem vocab/>
                    </#list>
                </div>
            </div>
        </div>

        <div class="my-3 p-3">
            <h5 class="pb-2 mb-2 pt-2 text-gbif-header-2 fw-400">
                ${t("extension.further.title")}<#t>
            </h5>

            <#if !newExtensions?has_content>
                <div class="callout callout-info">
                    ${t("extension.already.installed")}<#t>
                </div>
            </#if>

        </div>

        <div class="">
            <div class="flex-auto">
                <div class="d-flex flex-items-stretch flex-wrap">
                    <#list newExtensions as ext>
                        <@newExtensionItem ext/>
                    </#list>
                </div>
            </div>
        </div>
    </main>

    <#include "/inc/footer.ftl">
