<#-- @ftlvariable name="" type="org.gbif.ipt.action.manage.OverviewAction" -->
<#assign lastModifiedNotSet>${t('basic.lastModified.not.set')}<#t></#assign>

<!-- Represents source data section on resource overview page -->
<span class="anchor anchor-overview-page" id="anchor-sources"></span>
<div class="py-5 border-bottom section" id="sources">
    <div class="titleOverview">
        <div class="d-flex justify-content-between">
            <div class="d-flex">
                <h5 class="my-auto text-gbif-header-2 fw-400">
                    <#assign sourcesInfo>
                        ${t('manage.overview.source.description1')}&nbsp;${t('manage.overview.source.description2')}&nbsp;${t('manage.overview.source.description3', t('button.add'))}<br><br>${t('manage.overview.source.description4', t("button.connectDB"))}<#if !resource.dataPackage??><br><br>${t('manage.overview.source.description5')}</#if>
                    </#assign>
                    <@popoverTextInfo sourcesInfo/>

                    ${t('manage.overview.source.data')}<#t>
                </h5>
            </div>

            <div class="d-flex justify-content-end">
                <a id="add-source-button" class="text-gbif-header-2 icon-button icon-material-actions overview-action-button source-action" type="button" href="#">
                    <svg class="overview-action-button-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                        <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"></path>
                    </svg>
                    ${t("button.add")}<#t>
                </a>
            </div>
        </div>

        <#if sourcesModifiedSinceLastPublication || resource.lastPublished??>
        <div class="text-smaller">
            <small>
                <span>
                    <#if sourcesModifiedSinceLastPublication>
                        ${t('manage.home.last.modified')} ${resource.getSourcesModified()?datetime?string.medium!}
                    <#elseif resource.lastPublished??>
                        ${t("manage.overview.notModified")}<#t>
                    </#if>
                </span>
            </small>
        </div>
        </#if>

        <div class="mt-4">
            <p class="mb-0">
                <#if isDataPackage>
                    ${t('manage.overview.source.dataPackage.intro')}<#t>
                <#else>
                    ${t('manage.overview.source.intro')}<#t>
                </#if>
            </p>

            <#if (resource.sources?size>0)>
                <div class="details mt-3">
                    <div class="row g-2">
                        <#list resource.sources as src>
                            <div class="col-xl-6">
                                <div class="d-flex border rounded-2 mx-1 p-1 py-2 source-item">
                                    <#if src.isFileSource()>
                                        <div class="d-flex source-item-icon ps-2 my-auto" data-ipt-resource="${resource.shortname}" data-ipt-source="${src.name}">
                                            <#if src.readable>
                                                <i class="bi bi-file-text me-1 text-gbif-primary"  data-bs-toggle="tooltip" data-bs-placement="top" data-bs-html="true" title="${t('manage.overview.source.file')}<br><span class='text-gbif-primary'>${t('manage.source.readable')}<span><br>"></i>
                                            <#else>
                                                <i class="bi bi-file-text me-1 text-gbif-danger"  data-bs-toggle="tooltip" data-bs-placement="top" data-bs-html="true" title="${t('manage.overview.source.file')}<br><span class='text-gbif-danger'>${t('manage.source.notReadable')}<span><br>"></i>
                                            </#if>
                                        </div>

                                    <#elseif src.isExcelSource()>
                                        <div class="d-flex source-item-icon ps-2 my-auto" data-ipt-resource="${resource.shortname}" data-ipt-source="${src.name}">
                                            <#if src.readable>
                                                <i class="bi bi-file-excel me-1 text-gbif-primary"  data-bs-toggle="tooltip" data-bs-placement="top" data-bs-html="true" title="${t('manage.overview.source.excel')}<br><span class='text-gbif-primary'>${t('manage.source.readable')}<span><br>"></i>
                                            <#else>
                                                <i class="bi bi-file-excel me-1 text-gbif-danger"  data-bs-toggle="tooltip" data-bs-placement="top" data-bs-html="true" title="${t('manage.overview.source.excel')}<br><span class='text-gbif-danger'>${t('manage.source.notReadable')}<span><br>"></i>
                                            </#if>
                                        </div>

                                    <#elseif src.isUrlSource()>
                                        <div class="d-flex source-item-icon ps-2 my-auto" data-ipt-resource="${resource.shortname}" data-ipt-source="${src.name}">
                                            <#if src.readable>
                                                <i class="bi bi-link me-1 text-gbif-primary"  data-bs-toggle="tooltip" data-bs-placement="top" data-bs-html="true" title="${t('manage.overview.source.url')}<br><span class='text-gbif-primary'>${t('manage.source.readable')}<span><br>"></i>
                                            <#else>
                                                <i class="bi bi-link me-1 text-gbif-danger"  data-bs-toggle="tooltip" data-bs-placement="top" data-bs-html="true" title="${t('manage.overview.source.url')}<br><span class='text-gbif-danger'>${t('manage.source.notReadable')}<span><br>"></i>
                                            </#if>
                                        </div>

                                    <#else>
                                        <div class="d-flex source-item-icon ps-2 my-auto" data-ipt-resource="${resource.shortname}" data-ipt-source="${src.name}">
                                            <#if src.readable>
                                                <i class="bi bi-database me-1 text-gbif-primary"  data-bs-toggle="tooltip" data-bs-placement="top" data-bs-html="true" title="${t('manage.overview.source.sql')}<br><span class='text-gbif-primary'>${t('manage.source.readable')}<span><br>"></i>
                                            <#else>
                                                <i class="bi bi-database me-1 text-gbif-danger"  data-bs-toggle="tooltip" data-bs-placement="top" data-bs-html="true" title="${t('manage.overview.source.sql')}<br><span class='text-gbif-danger'>${t('manage.source.notReadable')}<span><br>"></i>
                                            </#if>
                                        </div>

                                    </#if>

                                    <div class="fs-smaller-2 source-item-link text-truncate ps-2 me-auto" data-ipt-resource="${resource.shortname}" data-ipt-source="${src.name}">
                                        <span class="fs-smaller fw-bold">${src.name!}</span><br>
                                        <#if src.isSqlSource()>
                                            <small>
                                                ${src.columns}&nbsp;${t('manage.overview.source.columns')} <span class="fw-bold">|</span>
                                            </small>
                                        <#else>
                                            <small>
                                                <#attempt>${src.formattedFileSize(currentLocale)}<#recover>-</#attempt> <span class="fw-bold">|</span>
                                                ${src.rows}&nbsp;${t('manage.overview.source.rows')}/${src.getColumns()}&nbsp;${t('manage.overview.source.columns')} <span class="fw-bold">|</span>
                                            </small>
                                        </#if>

                                        <small>
                                            ${(src.lastModified?datetime?string.medium)!lastModifiedNotSet}
                                        </small>
                                    </div>
                                    <div class="d-flex justify-content-end my-auto source-item-actions">
                                        <a title="${t("button.edit")}" class="icon-button icon-material-actions source-item-action fs-smaller-2 d-sm-max-none" type="button" href="source.do?r=${resource.shortname}&id=${src.name}">
                                            <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a.9959.9959 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"></path>
                                            </svg>
                                        </a>
                                        <#if src.isFileSource() || src.isExcelSource()>
                                            <a title="${t("button.download")}" class="icon-button icon-material-actions source-item-action fs-smaller-2 d-sm-max-none" type="button" href="raw-source.do?r=${resource.shortname}&id=${src.name}">
                                                <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                    <path d="M5 20h14v-2H5v2zM19 9h-4V3H9v6H5l7 7 7-7z"></path>
                                                </svg>
                                            </a>
                                        </#if>
                                        <a data-method="post" title="${t("button.delete")}" class="delete-source icon-button icon-material-actions source-item-action fs-smaller-2 d-sm-max-none" type="button" href="delete-source.do?r=${resource.shortname}&id=${src.name}">
                                            <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                            </svg>
                                        </a>
                                        <div class="dropdown d-sm-none">
                                            <a class="icon-button icon-material-actions source-item-action" type="button" href="#" id="dropdown-source-item-actions-${src_index}" data-bs-toggle="dropdown" aria-expanded="false">
                                                <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                    <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path>
                                                </svg>
                                            </a>

                                            <ul class="dropdown-menu" aria-labelledby="dropdown-source-item-actions-${src_index}">
                                                <#if src.isFileSource() || src.isExcelSource()>
                                                    <li>
                                                        <a class="dropdown-item action-link" type="button" href="raw-source.do?r=${resource.shortname}&id=${src.name}" target="_blank">
                                                            <svg class="overview-item-dropdown-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                                <path d="M5 20h14v-2H5v2zM19 9h-4V3H9v6H5l7 7 7-7z"></path>
                                                            </svg>
                                                            ${t("button.download")}<#t>
                                                        </a>
                                                    </li>
                                                </#if>
                                                <li>
                                                    <a data-method="post" class="delete-source source-item-action dropdown-item action-link" type="button" href="delete-source.do?r=${resource.shortname}&id=${src.name}">
                                                        <svg class="overview-item-dropdown-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                            <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                        </svg>
                                                        ${t("button.delete")}<#t>
                                                    </a>
                                                </li>
                                            </ul>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </#list>
                    </div>
                </div>
            </#if>
        </div>
    </div>

</div>

<!-- Represents mapping data section on resource overview page -->
<span class="anchor anchor-overview-page" id="anchor-mappings"></span>
<div class="py-5 border-bottom section" id="mappings">
    <#if isDataPackage>
        <div class="d-flex justify-content-between">
            <div class="d-flex">
                <h5 class="my-auto text-gbif-header-2 fw-400">
                    <#assign mappingsInfo>
                        ${t('manage.overview.mappings.info')}<#t>
                    </#assign>
                    <@popoverTextInfo mappingsInfo/>

                    ${t('manage.overview.mappings')}<#t>
                </h5>
            </div>

            <div class="d-flex justify-content-end">
                <#if (resource.sources?size>0)>
                    <form action='dataPackageMappingSourceNew.do' method='post'>
<@csrfField/>
                        <input name="r" type="hidden" value="${resource.shortname}"/>
                        <input name="id" type="hidden" value="${resource.dataPackageIdentifier}">
                        <button name="add" class="icon-button icon-material-actions overview-action-button" type="submit">
                            <svg class="overview-action-button-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"></path>
                            </svg>
                            ${t("button.add")}<#t>
                        </button>
                    </form>
                <#else>
                    <a id="add-mapping-button" class="text-gbif-header-2 icon-button icon-material-actions overview-action-button mapping-action" type="button" href="#">
                        <svg class="overview-action-button-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                            <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"></path>
                        </svg>
                        ${t("button.add")}<#t>
                    </a>
                </#if>
            </div>
        </div>

        <#if mappingsModifiedSinceLastPublication || resource.lastPublished??>
            <div class="text-smaller">
                <small>
                    <span>
                        <#if mappingsModifiedSinceLastPublication>
                            ${t('manage.home.last.modified')} ${resource.getMappingsModified()?datetime?string.medium!}
                        <#elseif resource.lastPublished??>
                            ${t("manage.overview.notModified")}<#t>
                        </#if>
                    </span>
                </small>
            </div>
        </#if>

        <div class="row mt-4">
            <p>
                ${t('manage.overview.mappings.description')}<#t>
            </p>

            <#if !dataPackageMappingsMissing && dataPackageSchemaChanged>
                <p class="text-gbif-danger">
                    <#assign textArg1><b>${installedSchemaName!"?"}</b></#assign><#assign textArg2><b>v${resource.dataPackageVersion!"?"}</b></#assign><#assign textArg3><b>v${installedSchemaVersion!"?"}</b></#assign>${t('manage.overview.mappings.schemaChanged', textArg1, textArg2, textArg3)}<#t>
                </p>
            </#if>

            <#if resource.dataPackageIdentifier?has_content>
                <#if resource.dataPackageIdentifier?contains("dwc-dp")>
                    <#assign eventMappings =
                    resource.dataPackageMappings!?filter(m -> dwcDpEvents?seq_contains(m.dataPackageTableSchemaName.name))>
                    <#assign entityMappings =
                    resource.dataPackageMappings!?filter(m -> dwcDpEntities?seq_contains(m.dataPackageTableSchemaName.name))>
                    <#assign agentMappings =
                    resource.dataPackageMappings!?filter(m -> dwcDpAgents?seq_contains(m.dataPackageTableSchemaName.name))>
                    <#assign joinMappings =
                    resource.dataPackageMappings!?filter(m -> dwcDpJoins?seq_contains(m.dataPackageTableSchemaName.name))>
                    <#assign otherMappings =
                    resource.dataPackageMappings!?filter(m -> !dwcDpEvents?seq_contains(m.dataPackageTableSchemaName.name)
                    && !dwcDpEntities?seq_contains(m.dataPackageTableSchemaName.name)
                    && !dwcDpAgents?seq_contains(m.dataPackageTableSchemaName.name)
                    && !dwcDpJoins?seq_contains(m.dataPackageTableSchemaName.name))>

                    <div class="details">
                        <#-- Events -->
                        <#if eventMappings?size &gt; 0>
                            <div class="mapping_head">Events</div>
                            <div class="row g-2">
                                <#list resource.dataPackageMappings as m>
                                    <#if dwcDpEvents?seq_contains(m.dataPackageTableSchemaName.name)>
                                    <div class="col-xl-6">
                                        <div class="d-flex border rounded-2 mx-1 p-1 py-2 mapping-item mapping-item-dwc-dp-event">
                                            <div class="d-flex my-auto mapping-item-icon ps-2">
                                                <i class="bi bi-arrow-down-up me-1 text-gbif-primary"></i>
                                            </div>
                                            <div class="fs-smaller-2 text-truncate schema-mapping-item-link ps-2 me-auto" data-ipt-resource="${resource.shortname}" data-ipt-extension="${m.dataPackageSchema.identifier?url}" data-ipt-mapping="${m_index}">
                                                <strong class="fs-smaller">${(m.source.name)!"?"}</strong>
                                                <i class="bi bi-arrow-right"></i>
                                                <strong class="fs-smaller">${(m.dataPackageTableSchemaName.name)!"?"}</strong>
                                                <br>
                                                <small>${t('manage.overview.mappings.fields', (m.fields)!?size)} | ${(m.lastModified?datetime?string.medium)!lastModifiedNotSet}</small>
                                            </div>
                                            <div class="my-auto d-flex justify-content-end pt-0 mapping-item-actions">
                                                <a title="${t("button.edit")}" class="icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="dataPackageMapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                    <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                        <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a.9959.9959 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"></path>
                                                    </svg>
                                                </a>
                                                <a data-method="post" title="${t("button.delete")}" class="delete-mapping icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="deleteDataPackageMapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                    <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                        <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                    </svg>
                                                </a>

                                                <div class="dropdown d-sm-none">
                                                    <a class="icon-button icon-material-actions mapping-item-action" type="button" href="#" id="dropdown-mapping-item-actions-${m_index}" data-bs-toggle="dropdown" aria-expanded="false">
                                                        <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                            <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path>
                                                        </svg>
                                                    </a>

                                                    <ul class="dropdown-menu" aria-labelledby="dropdown-mapping-item-actions-${m_index}">
                                                        <li>
                                                            <a data-method="post" class="dropdown-item action-link delete-mapping" type="button" href="delete-mapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                                <svg class="overview-item-action-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                                    <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                                </svg>
                                                                ${t("button.delete")}<#t>
                                                            </a>
                                                        </li>
                                                    </ul>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    </#if>
                                </#list>
                            </div>
                        </#if>
                        <#-- Entities -->
                        <#if entityMappings?size &gt; 0>
                        <div class="mapping_head mt-2">Entities</div>
                        <div class="row g-2">
                            <#list resource.dataPackageMappings as m>
                                <#if dwcDpEntities?seq_contains(m.dataPackageTableSchemaName.name)>
                                <div class="col-xl-6">
                                    <div class="d-flex border rounded-2 mx-1 p-1 py-2 mapping-item mapping-item-dwc-dp-entity">
                                        <div class="d-flex my-auto mapping-item-icon ps-2">
                                            <i class="bi bi-arrow-down-up me-1 text-gbif-primary"></i>
                                        </div>
                                        <div class="fs-smaller-2 text-truncate schema-mapping-item-link ps-2 me-auto" data-ipt-resource="${resource.shortname}" data-ipt-extension="${m.dataPackageSchema.identifier?url}" data-ipt-mapping="${m_index}">
                                            <strong class="fs-smaller">${(m.source.name)!"?"}</strong>
                                            <i class="bi bi-arrow-right"></i>
                                            <strong class="fs-smaller">${(m.dataPackageTableSchemaName.name)!"?"}</strong>
                                            <br>
                                            <small>${t('manage.overview.mappings.fields', (m.fields)!?size)} | ${(m.lastModified?datetime?string.medium)!lastModifiedNotSet}</small>
                                        </div>
                                        <div class="my-auto d-flex justify-content-end pt-0 mapping-item-actions">
                                            <a title="${t("button.edit")}" class="icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="dataPackageMapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                    <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a.9959.9959 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"></path>
                                                </svg>
                                            </a>
                                            <a data-method="post" title="${t("button.delete")}" class="delete-mapping icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="deleteDataPackageMapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                    <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                </svg>
                                            </a>

                                            <div class="dropdown d-sm-none">
                                                <a class="icon-button icon-material-actions mapping-item-action" type="button" href="#" id="dropdown-mapping-item-actions-${m_index}" data-bs-toggle="dropdown" aria-expanded="false">
                                                    <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                        <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path>
                                                    </svg>
                                                </a>

                                                <ul class="dropdown-menu" aria-labelledby="dropdown-mapping-item-actions-${m_index}">
                                                    <li>
                                                        <a data-method="post" class="dropdown-item action-link delete-mapping" type="button" href="delete-mapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                            <svg class="overview-item-action-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                            </svg>
                                                            ${t("button.delete")}<#t>
                                                        </a>
                                                    </li>
                                                </ul>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                </#if>
                            </#list>
                        </div>
                        </#if>
                        <#-- Agents -->
                        <#if agentMappings?size &gt; 0>
                            <div class="mapping_head mt-2">Agents</div>
                            <div class="row g-2">
                                <#list resource.dataPackageMappings as m>
                                    <#if dwcDpAgents?seq_contains(m.dataPackageTableSchemaName.name)>
                                    <div class="col-xl-6">
                                        <div class="d-flex border rounded-2 mx-1 p-1 py-2 mapping-item mapping-item-dwc-dp-agent">
                                            <div class="d-flex my-auto mapping-item-icon ps-2">
                                                <i class="bi bi-arrow-down-up me-1 text-gbif-primary"></i>
                                            </div>
                                            <div class="fs-smaller-2 text-truncate schema-mapping-item-link ps-2 me-auto" data-ipt-resource="${resource.shortname}" data-ipt-extension="${m.dataPackageSchema.identifier?url}" data-ipt-mapping="${m_index}">
                                                <strong class="fs-smaller">${(m.source.name)!"?"}</strong>
                                                <i class="bi bi-arrow-right"></i>
                                                <strong class="fs-smaller">${(m.dataPackageTableSchemaName.name)!"?"}</strong>
                                                <br>
                                                <small>${t('manage.overview.mappings.fields', (m.fields)!?size)} | ${(m.lastModified?datetime?string.medium)!lastModifiedNotSet}</small>
                                            </div>
                                            <div class="my-auto d-flex justify-content-end pt-0 mapping-item-actions">
                                                <a title="${t("button.edit")}" class="icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="dataPackageMapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                    <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                        <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a.9959.9959 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"></path>
                                                    </svg>
                                                </a>
                                                <a data-method="post" title="${t("button.delete")}" class="delete-mapping icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="deleteDataPackageMapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                    <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                        <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                    </svg>
                                                </a>

                                                <div class="dropdown d-sm-none">
                                                    <a class="icon-button icon-material-actions mapping-item-action" type="button" href="#" id="dropdown-mapping-item-actions-${m_index}" data-bs-toggle="dropdown" aria-expanded="false">
                                                        <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                            <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path>
                                                        </svg>
                                                    </a>

                                                    <ul class="dropdown-menu" aria-labelledby="dropdown-mapping-item-actions-${m_index}">
                                                        <li>
                                                            <a data-method="post" class="dropdown-item action-link delete-mapping" type="button" href="delete-mapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                                <svg class="overview-item-action-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                                    <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                                </svg>
                                                                ${t("button.delete")}<#t>
                                                            </a>
                                                        </li>
                                                    </ul>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    </#if>
                                </#list>
                            </div>
                        </#if>
                        <#-- Joins -->
                        <#if joinMappings?size &gt; 0>
                            <div class="mapping_head mt-2">Joins</div>
                            <div class="row g-2">
                                <#list resource.dataPackageMappings as m>
                                    <#if dwcDpJoins?seq_contains(m.dataPackageTableSchemaName.name)>
                                    <div class="col-xl-6">
                                        <div class="d-flex border rounded-2 mx-1 p-1 py-2 mapping-item mapping-item-dwc-dp-join">
                                            <div class="d-flex my-auto mapping-item-icon ps-2">
                                                <i class="bi bi-arrow-down-up me-1 text-gbif-primary"></i>
                                            </div>
                                            <div class="fs-smaller-2 text-truncate schema-mapping-item-link ps-2 me-auto" data-ipt-resource="${resource.shortname}" data-ipt-extension="${m.dataPackageSchema.identifier?url}" data-ipt-mapping="${m_index}">
                                                <strong class="fs-smaller">${(m.source.name)!"?"}</strong>
                                                <i class="bi bi-arrow-right"></i>
                                                <strong class="fs-smaller">${(m.dataPackageTableSchemaName.name)!"?"}</strong>
                                                <br>
                                                <small>${t('manage.overview.mappings.fields', (m.fields)!?size)} | ${(m.lastModified?datetime?string.medium)!lastModifiedNotSet}</small>
                                            </div>
                                            <div class="my-auto d-flex justify-content-end pt-0 mapping-item-actions">
                                                <a title="${t("button.edit")}" class="icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="dataPackageMapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                    <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                        <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a.9959.9959 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"></path>
                                                    </svg>
                                                </a>
                                                <a data-method="post" title="${t("button.delete")}" class="delete-mapping icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="deleteDataPackageMapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                    <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                        <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                    </svg>
                                                </a>

                                                <div class="dropdown d-sm-none">
                                                    <a class="icon-button icon-material-actions mapping-item-action" type="button" href="#" id="dropdown-mapping-item-actions-${m_index}" data-bs-toggle="dropdown" aria-expanded="false">
                                                        <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                            <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path>
                                                        </svg>
                                                    </a>

                                                    <ul class="dropdown-menu" aria-labelledby="dropdown-mapping-item-actions-${m_index}">
                                                        <li>
                                                            <a data-method="post" class="dropdown-item action-link delete-mapping" type="button" href="delete-mapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                                <svg class="overview-item-action-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                                    <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                                </svg>
                                                                ${t("button.delete")}<#t>
                                                            </a>
                                                        </li>
                                                    </ul>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    </#if>
                                </#list>
                            </div>
                        </#if>
                        <#-- Others -->
                        <#if otherMappings?size &gt; 0>
                            <div class="mapping_head mt-2">Others</div>
                            <div class="row g-2">
                                <#list resource.dataPackageMappings as m>
                                    <#if !dwcDpTables?seq_contains(m.dataPackageTableSchemaName.name)>
                                        <div class="col-xl-6">
                                            <div class="d-flex border rounded-2 mx-1 p-1 py-2 mapping-item mapping-item-dwc-dp-other">
                                                <div class="d-flex my-auto mapping-item-icon ps-2">
                                                    <i class="bi bi-arrow-down-up me-1 text-gbif-primary"></i>
                                                </div>
                                                <div class="fs-smaller-2 text-truncate schema-mapping-item-link ps-2 me-auto" data-ipt-resource="${resource.shortname}" data-ipt-extension="${m.dataPackageSchema.identifier?url}" data-ipt-mapping="${m_index}">
                                                    <strong class="fs-smaller">${(m.source.name)!"?"}</strong>
                                                    <i class="bi bi-arrow-right"></i>
                                                    <strong class="fs-smaller">${(m.dataPackageTableSchemaName.name)!"?"}</strong>
                                                    <br>
                                                    <small>${t('manage.overview.mappings.fields', (m.fields)!?size)} | ${(m.lastModified?datetime?string.medium)!lastModifiedNotSet}</small>
                                                </div>
                                                <div class="my-auto d-flex justify-content-end pt-0 mapping-item-actions">
                                                    <a title="${t("button.edit")}" class="icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="dataPackageMapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                        <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                            <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a.9959.9959 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"></path>
                                                        </svg>
                                                    </a>
                                                    <a data-method="post" title="${t("button.delete")}" class="delete-mapping icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="deleteDataPackageMapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                        <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                            <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                        </svg>
                                                    </a>

                                                    <div class="dropdown d-sm-none">
                                                        <a class="icon-button icon-material-actions mapping-item-action" type="button" href="#" id="dropdown-mapping-item-actions-${m_index}" data-bs-toggle="dropdown" aria-expanded="false">
                                                            <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                                <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path>
                                                            </svg>
                                                        </a>

                                                        <ul class="dropdown-menu" aria-labelledby="dropdown-mapping-item-actions-${m_index}">
                                                            <li>
                                                                <a data-method="post" class="dropdown-item action-link delete-mapping" type="button" href="delete-mapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                                    <svg class="overview-item-action-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                                        <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                                    </svg>
                                                                    ${t("button.delete")}<#t>
                                                                </a>
                                                            </li>
                                                        </ul>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </#if>
                                </#list>
                            </div>
                        </#if>
                    </div>
                <#else>
                    <div class="details">
                        <div class="row g-2">
                            <#list resource.dataPackageMappings! as m>
                                <div class="col-xl-6">
                                    <div class="d-flex border rounded-2 mx-1 p-1 py-2 mapping-item">
                                        <div class="d-flex my-auto mapping-item-icon ps-2">
                                            <i class="bi bi-arrow-down-up me-1 text-gbif-primary"></i>
                                        </div>
                                        <div class="fs-smaller-2 text-truncate schema-mapping-item-link ps-2 me-auto" data-ipt-resource="${resource.shortname}" data-ipt-extension="${m.dataPackageSchema.identifier?url}" data-ipt-mapping="${m_index}">
                                            <strong class="fs-smaller">${(m.source.name)!"?"}</strong>
                                            <i class="bi bi-arrow-right"></i>
                                            <strong class="fs-smaller">${(m.dataPackageTableSchemaName.name)!"?"}</strong>
                                            <br>
                                            <small>${t('manage.overview.mappings.fields', (m.fields)!?size)} | ${(m.lastModified?datetime?string.medium)!lastModifiedNotSet}</small>
                                        </div>
                                        <div class="my-auto d-flex justify-content-end pt-0 mapping-item-actions">
                                            <a title="${t("button.edit")}" class="icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="dataPackageMapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                    <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a.9959.9959 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"></path>
                                                </svg>
                                            </a>
                                            <a data-method="post" title="${t("button.delete")}" class="delete-mapping icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="deleteDataPackageMapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                    <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                </svg>
                                            </a>

                                            <div class="dropdown d-sm-none">
                                                <a class="icon-button icon-material-actions mapping-item-action" type="button" href="#" id="dropdown-mapping-item-actions-${m_index}" data-bs-toggle="dropdown" aria-expanded="false">
                                                    <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                        <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path>
                                                    </svg>
                                                </a>

                                                <ul class="dropdown-menu" aria-labelledby="dropdown-mapping-item-actions-${m_index}">
                                                    <li>
                                                        <a data-method="post" class="dropdown-item action-link delete-mapping" type="button" href="delete-mapping.do?r=${resource.shortname}&id=${m.dataPackageSchema.identifier?url}&mid=${m_index}">
                                                            <svg class="overview-item-action-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                            </svg>
                                                            ${t("button.delete")}<#t>
                                                        </a>
                                                    </li>
                                                </ul>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </#list>
                        </div>
                    </div>
                </#if>
            </#if>
        </div>
    <#else>
        <div class="d-flex justify-content-between">
            <div class="d-flex">
                <h5 class="my-auto text-gbif-header-2 fw-400">
                    <#assign mappingsInfo>
                        ${t('manage.overview.DwC.Mappings.coretype.description1')}<br><br>${t('manage.overview.DwC.Mappings.coretype.description2')}<br><br>${t('manage.overview.DwC.Mappings.coretype.description3')}<br><br>${t('manage.overview.DwC.Mappings.coretype.description4')}
                    </#assign>
                    <@popoverTextInfo mappingsInfo/>

                    ${t('manage.overview.DwC.Mappings')}<#t>
                </h5>
            </div>

            <div class="d-flex justify-content-end">
                <a id="add-mapping-button" class="text-gbif-header-2 icon-button icon-material-actions overview-action-button mapping-action" type="button" href="#">
                    <svg class="overview-action-button-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                        <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"></path>
                    </svg>
                    ${t("button.add")}<#t>
                </a>
            </div>
        </div>

        <#if mappingsModifiedSinceLastPublication || resource.lastPublished??>
            <div class="text-smaller">
                <small>
                    <#if mappingsModifiedSinceLastPublication>
                        ${t('manage.home.last.modified')} ${resource.mappingsModified?datetime?string.medium!}
                    <#elseif resource.lastPublished??>
                        ${t("manage.overview.notModified")}<#t>
                    </#if>
                </small>
            </div>
        </#if>

        <div class="mt-4">
            <p class="mb-0">
                ${t('manage.overview.DwC.Mappings.description')}<#t>
            </p>

            <#if resource.coreRowType?has_content>
                <#assign extensionMappingsSize=resource.getMappedExtensions()?size/>
                <#if (extensionMappingsSize > 1)>
                    <#assign deleteCoreMappingClass="delete-core-mapping"/>
                <#else>
                    <#assign deleteCoreMappingClass="delete-mapping"/>
                </#if>
                <div class="details mt-3">
                    <div class="mapping_head">${t('manage.overview.DwC.Mappings.cores.select')}</div>
                    <div class="row g-2">
                        <#list resource.getMappings(resource.coreRowType) as m>
                            <div class="col-xl-6">
                                <div class="d-flex border rounded-2 mx-1 p-1 py-2 mapping-item">
                                    <div class="d-flex my-auto mapping-item-icon ps-2">
                                        <i class="bi bi-arrow-down-up me-1 text-gbif-primary"></i>
                                    </div>
                                    <div class="fs-smaller-2 text-truncate mapping-item-link ps-2 me-auto" data-ipt-resource="${resource.shortname}" data-ipt-extension="${m.extension.rowType?url}" data-ipt-mapping="${m_index}">
                                        <strong class="fs-smaller">${(m.source.name)!}</strong>
                                        <i class="bi bi-arrow-right"></i>
                                        <strong class="fs-smaller">${m.extension.title}</strong>
                                        <br>
                                        <small>${t('manage.overview.mappings.terms', m.fields?size)} | ${(m.lastModified?datetime?string.medium)!lastModifiedNotSet}</small>
                                    </div>
                                    <div class="my-auto d-flex justify-content-end pt-0 mapping-item-actions">
                                        <a title="${t("button.edit")}" class="icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="mapping.do?r=${resource.shortname}&id=${m.extension.rowType?url}&mid=${m_index}">
                                            <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a.9959.9959 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"></path>
                                            </svg>
                                        </a>
                                        <a title="${t("button.preview")}" class="peekBtn icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="mappingPeek.do?r=${resource.shortname}&id=${m.extension.rowType?url}&mid=${m_index}">
                                            <svg class="overview-item-action-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"></path>
                                            </svg>
                                        </a>
                                        <a data-method="post" title="${t("button.delete")}" class="${deleteCoreMappingClass} icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="delete-mapping.do?r=${resource.shortname}&id=${m.extension.rowType?url}&mid=${m_index}">
                                            <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                            </svg>
                                        </a>

                                        <div class="dropdown d-sm-none">
                                            <a class="icon-button icon-material-actions mapping-item-action" type="button" href="#" id="dropdown-mapping-item-actions-${m_index}" data-bs-toggle="dropdown" aria-expanded="false">
                                                <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                    <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path>
                                                </svg>
                                            </a>

                                            <ul class="dropdown-menu" aria-labelledby="dropdown-mapping-item-actions-${m_index}">
                                                <li>
                                                    <a class="dropdown-item action-link peekBtn" type="button" href="mappingPeek.do?r=${resource.shortname}&id=${m.extension.rowType?url}&mid=${m_index}">
                                                        <svg class="overview-item-action-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                            <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"></path>
                                                        </svg>
                                                        ${t("button.preview")}<#t>
                                                    </a>
                                                </li>
                                                <li>
                                                    <a data-method="post" class="dropdown-item action-link ${deleteCoreMappingClass}" type="button" href="delete-mapping.do?r=${resource.shortname}&id=${m.extension.rowType?url}&mid=${m_index}">
                                                        <svg class="overview-item-action-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                            <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                        </svg>
                                                        ${t("button.delete")}<#t>
                                                    </a>
                                                </li>
                                            </ul>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </#list>
                    </div>
                    <#if (extensionMappingsSize > 1)>
                        <div class="mapping_head mt-3">${t('manage.overview.DwC.Mappings.extensions.select')}</div>
                        <div class="row g-2">
                            <#list resource.getMappedExtensions() as ext>
                                <#if ext.rowType != resource.coreRowType>
                                    <#list resource.getMappings(ext.rowType) as m>
                                        <div class="col-xl-6">
                                            <div class="d-flex border rounded-2 mx-1 p-1 py-2 mapping-item">
                                                <div class="d-flex my-auto mapping-item-icon ps-2">
                                                    <i class="bi bi-arrow-down-up me-1 text-gbif-primary"></i>
                                                </div>
                                                <div class="fs-smaller-2 text-truncate mapping-item-link ps-2 me-auto" data-ipt-resource="${resource.shortname}" data-ipt-extension="${ext.rowType?url}" data-ipt-mapping="${m_index}">
                                                    <strong class="fs-smaller">${(m.source.name)!}</strong>
                                                    <i class="bi bi-arrow-right"></i>
                                                    <strong class="fs-smaller">${ext.title}</strong>
                                                    <br>
                                                    <small>${m.fields?size} terms | ${(m.lastModified?datetime?string.medium)!lastModifiedNotSet}</small>
                                                </div>
                                                <div class="my-auto d-flex justify-content-end pt-0 mapping-item-actions">
                                                    <a title="${t("button.edit")}" class="icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="mapping.do?r=${resource.shortname}&id=${ext.rowType?url}&mid=${m_index}">
                                                        <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                            <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a.9959.9959 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"></path>
                                                        </svg>
                                                    </a>
                                                    <a title="${t("button.preview")}" class="peekBtn icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="mappingPeek.do?r=${resource.shortname}&id=${ext.rowType?url}&mid=${m_index}">
                                                        <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                            <svg class="overview-item-action-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                                <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"></path>
                                                            </svg>
                                                        </svg>
                                                    </a>
                                                    <a data-method="post" title="${t("button.delete")}" class="delete-mapping icon-button icon-material-actions mapping-item-action fs-smaller-2 d-sm-max-none" type="button" href="delete-mapping.do?r=${resource.shortname}&id=${ext.rowType?url}&mid=${m_index}">
                                                        <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                            <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                        </svg>
                                                    </a>

                                                    <div class="dropdown d-sm-none">
                                                        <a class="icon-button icon-material-actions mapping-item-action" type="button" href="#" id="dropdown-mapping-item-actions-${m_index}" data-bs-toggle="dropdown" aria-expanded="false">
                                                            <svg class="icon-button-svg" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                                <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path>
                                                            </svg>
                                                        </a>

                                                        <ul class="dropdown-menu" aria-labelledby="dropdown-mapping-item-actions-${m_index}">
                                                            <li>
                                                                <a class="dropdown-item action-link peekBtn" type="button" href="mappingPeek.do?r=${resource.shortname}&id=${ext.rowType?url}&mid=${m_index}">
                                                                    <svg class="overview-item-action-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                                        <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"></path>
                                                                    </svg>
                                                                    ${t("button.preview")}<#t>
                                                                </a>
                                                            </li>
                                                            <li>
                                                                <a data-method="post" class="dropdown-item action-link delete-mapping" type="button" href="delete-mapping.do?r=${resource.shortname}&id=${ext.rowType?url}&mid=${m_index}">
                                                                    <svg class="overview-item-action-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                                                        <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                                    </svg>
                                                                    ${t("button.delete")}<#t>
                                                                </a>
                                                            </li>
                                                        </ul>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </#list>
                                </#if>
                            </#list>
                        </div>
                    </#if>
                </div>
            </#if>
        </div>
    </#if>
</div>
