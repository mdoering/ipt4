<#-- @ftlvariable name="" type="org.gbif.ipt.action.manage.DataPackageMappingAction" -->
    <#include "/inc/header.ftl"/>
    <title>${t("manage.mapping.title")}</title>
    <script src="${baseURL}/js/jconfirmation.jquery.js"></script>
    <link rel="stylesheet" href="${baseURL}/styles/select2/select2-4.0.13.min.css">
    <link rel="stylesheet" href="${baseURL}/styles/select2/select2-bootstrap4.min.css">
    <style>
        .select2-container--bootstrap4 .select2-selection--single {
            height: calc(1.5em + 0.5rem + 2px) !important;
        }

        .select2-container--bootstrap4 .select2-results__option {
            padding: 0.25rem 0.75rem;
        }

        .select2-container--bootstrap4 .select2-selection--single .select2-selection__rendered {
            line-height: 1.5;
            padding: 0.25rem 0.5rem !important;
        }

        .select2-container--bootstrap4 .select2-selection__clear {
            margin-right: 0.875em;
            margin-top: 0.275em;
        }

        .select2-container--bootstrap4 .select2-selection--single .select2-selection__placeholder {
            line-height: 1.5;
        }
    </style>
    <script src="${baseURL}/js/select2/select2-4.0.13.full.min.js"></script>
    <script>
        $(document).ready(function(){
            function showHideFilter(){
                var filterSelectedOption = $('#filterComp option:selected');
                var filterParam = $('#filterParam');

                if (filterSelectedOption.val() === "Equals" || filterSelectedOption.val() === "NotEquals") {
                    filterParam.show();
                } else {
                    filterParam.hide();
                    filterParam.val("");
                }
            }

            function showHideFilterName() {
                var filterSelectedName = $("#filterName option:selected");
                var filterComp = $("#filterComp");

                if (filterSelectedName.val() === "") {
                    filterComp.hide();
                    filterComp.val("");
                    showHideFilter();
                } else {
                    filterComp.show();
                }
            }

            showHideFilter();
            showHideFilterName();

            $("#filterComp").change(function() {
                showHideFilter();
            });

            $("#filterName").change(function() {
                showHideFilterName();
            });

            function activateDeactivateAllStaticInputs() {
                $('.fidx').each(function() {
                    activateDeactivateStaticInput($(this));
                });
            }

            function activateDeactivateStaticInput(target) {
                var suffix = target.attr('id').substring(4);
                var input = $("#fVal" + suffix);
                if (!target.val().trim()) {
                    input.prop('disabled', false);
                } else {
                    // deactivate input
                    input.val('');
                    input.prop('disabled', true);
                }
            }

            activateDeactivateAllStaticInputs();

            function hideFields() {
                showAll = false;
                $("#showAllValue").val("false");
                $("#toggleFields").text("${t("manage.mapping.showAll")}");
                $('div.mappingRow').not('.required').each(function(index) {
                    // always show all mapped and required fields
                    if ($(".fidx", this).val() === "" && $(".fval", this).val() === ""){
                        $(this).hide();
                    }
                });

                if($('#filterComp option:selected').val() === "") {
                    $('#filterSection').hide();
                }
            }

            // show only required and mapped fields
            $("#toggleFields").click(function() {
                if(showAll) {
                    hideFields();
                } else {
                    showAll = true;
                    $("#showAllValue").val("true");
                    $("#toggleFields").text("${t("manage.mapping.hideEmpty")}");
                    $('div.mappingRow').show();
                    $(".groupmenu").show();
                }
            });

            // Collapse/uncollapse source examples
            $(".sample").click(function() {
                if ($(this).hasClass("text-uncollapse")) {
                    $(this).removeClass("text-uncollapse")
                } else {
                    $(this).addClass("text-uncollapse")
                }
            });

            $(".fidx").change(function() {
                activateDeactivateStaticInput($(this));
            });

            var showAll=${RequestParameters.showAll!"true"};
            if (!showAll){
                hideFields();
            }

            $('.confirm').jConfirmAction({
                titleQuestion: "${t("basic.confirm")}",
                yesAnswer: "${t("basic.yes")}",
                cancelAnswer: "${t("basic.no")}",
                buttonType: "danger",
                baseUrl: "${baseURL}"
            });

            $("#idColumn").select2({
                placeholder: '${t("manage.mapping.noid")}',
                language: {
                    noResults: function () {
                        return '${selectNoResultsFound}';
                    }
                },
                width: "100%",
                allowClear: true,
                minimumResultsForSearch: 15,
                dropdownCssClass: 'text-smaller',
                theme: 'bootstrap4'
            });
            $("#mapping\\.filter\\.filterTime").select2({
                placeholder: '',
                language: {
                    noResults: function () {
                        return '${selectNoResultsFound}';
                    }
                },
                width: "100%",
                minimumResultsForSearch: 15,
                dropdownCssClass: 'text-smaller',
                theme: 'bootstrap4'
            });
            $("#filterName").select2({
                placeholder: '',
                language: {
                    noResults: function () {
                        return '${selectNoResultsFound}';
                    }
                },
                width: "100%",
                allowClear: true,
                minimumResultsForSearch: 15,
                dropdownCssClass: 'text-smaller',
                theme: 'bootstrap4'
            });
            $("#filterComp").select2({
                placeholder: '',
                language: {
                    noResults: function () {
                        return '${selectNoResultsFound}';
                    }
                },
                width: "100%",
                allowClear: true,
                minimumResultsForSearch: 15,
                dropdownCssClass: 'text-smaller',
                theme: 'bootstrap4'
            });
            $("[id^=fIdx]").select2({
                placeholder: '',
                language: {
                    noResults: function () {
                        return '${selectNoResultsFound}';
                    }
                },
                width: "100%",
                allowClear: true,
                minimumResultsForSearch: 15,
                dropdownCssClass: 'text-smaller',
                theme: 'bootstrap4'
            });
            $(".fval-select").select2({
                placeholder: '',
                language: {
                    noResults: function () {
                        return '${selectNoResultsFound}';
                    }
                },
                width: "100%",
                allowClear: true,
                minimumResultsForSearch: 15,
                dropdownCssClass: 'text-smaller',
                theme: 'bootstrap4'
            });
        });
    </script>

    <#assign currentMenu = "manage"/>
    <#assign nonMapped = action.getNonMappedColumns()/>
    <#include "/inc/menu.ftl"/>
    <#include "/macros/forms.ftl"/>
    <#include "/macros/popover.ftl"/>
<#--    1. Interpret backticked text as code-->
<#--    2. Interpret text like []() as a link-->
    <#macro processDescription description>
        ${description
        ?replace("`(.*?)`", "<code>$1</code>", "r")
        ?replace("'", "&#39;")
        ?replace("\\[([^\\]]+)\\]\\(([^\\)]+)\\)", "<a href=\"$2\">$1</a>", "r")
        ?no_esc}
    </#macro>

    <#macro sourceSample index tableSchemaName fieldsIndex>
        <div id="fSIdx_${tableSchemaName}_${fieldsIndex}" class="text-collapse sample mappingText mx-3">
            ${t('manage.mapping.sourceSample')}:
            <em>
                <#list peek as row>
                    <#if row??>
                        <#if row[index]?has_content && row[index]!=" ">
                            <code>${row[index]}</code>
                        <#else>
                            &nbsp;
                        </#if>
                        <#if row_has_next> | </#if>
                    </#if>
                </#list>
            </em>
        </div>
    </#macro>

    <#macro showField tableSchema field index>
        <#assign fieldsIndex = action.getFieldsIndices().get(field.field.name)/>

        <div class="row py-1 g-2 mappingRow border-bottom text-smaller">
            <div class="col-lg-4 pt-1 fs-smaller">
                <#assign fieldPopoverInfo>
                    <#if field.field.description?has_content>
                        <@processDescription field.field.description />
                    <#else>
                        ${t("basic.no.description")}<#t>
                    </#if>
                    <#if field.field.type??>
                        <br/><br/>
                        <em>${t("schema.field.type")}:</em>
                        <span>${field.field.type!}</span>
                    </#if>
                    <#if field.field.constraints?? && (field.field.constraints.unique?? || field.field.constraints.maximum?? || field.field.constraints.minimum?? || field.field.constraints.pattern??)>
                        <br/><br/>
                        <em>${t("schema.field.constraints")}:</em>
                        <ul>
                            <#if field.field.constraints.unique??>
                                <li>unique <code>${field.field.constraints.unique?string}</code></li>
                            </#if>
                            <#if field.field.constraints.maximum??>
                                <li>maximum <code>${field.field.constraints.maximum}</code></li>
                            </#if>
                            <#if field.field.constraints.minimum??>
                                <li>minimum <code>${field.field.constraints.minimum}</code></li>
                            </#if>
                            <#if field.field.constraints.pattern??>
                                <li>pattern <code>${field.field.constraints.pattern}</code></li>
                            </#if>
                        </ul>
                        <#assign noBrakeForExamples = true />
                    <#else>
                        <#assign noBrakeForExamples = false />
                    </#if>
                    <#if (field.field.example)?has_content>
                        <#if !noBrakeForExamples>
                            <br/><br/>
                        </#if>
                        <em>${t("basic.examples")}</em>:
                        <#if field.field.example?is_collection>
                            <#list field.field.example as ex>
                                <code>${ex}</code><#sep>, </#sep>
                            </#list>
                        <#else>
                            <#if field.field.example?is_boolean>
                                <code>${field.field.example?string("true", "false")}</code>
                            <#else>
                                <code>${field.field.example}</code>
                            </#if>
                        </#if>
                    </#if>
                </#assign>
                <@popoverTextInfo fieldPopoverInfo />

                <strong>
                    ${field.field.name}
                    <#if (field.field.constraints.required)?? && (field.field.constraints.required?string) == "true"><span class="text-gbif-danger">&#42;</span></#if>
                </strong>
            </div>

            <div class="col-lg-4">
                <select id="fIdx_${tableSchema.name}_${index}" class="fidx form-select form-select-sm" name="fields[${index}].index">
                    <option value="" <#if !field.index??> selected="selected"</#if>></option>
                    <#list columns as col>
                        <option value="${col_index}" <#if (field.index!-1)==col_index> selected="selected"</#if>>${col}</option>
                    </#list>
                </select>
            </div>

            <div class="col-lg-4">
                <#if field.field.constraints?? && field.field.constraints.vocabulary??>
                    <div class="input-group input-group-sm">
                        <select id="fVal_${tableSchema.name}_${index}" class="fval fval-select form-select form-select-sm" name="fields[${index}].defaultValue">
                            <option value="" <#if !field.defaultValue??> selected="selected"</#if>></option>
                            <#list field.field.constraints.vocabulary as code>
                                <option value="${code}" <#if (field.defaultValue!"")==code> selected="selected"</#if>>${code}</option>
                            </#list>
                        </select>
                    </div>
                <#elseif field.field.type?? && field.field.type == "boolean" >
                      <div class="input-group input-group-sm">
                            <select id="fVal_${tableSchema.name}_${index}" class="fval fval-select form-select form-select-sm" name="fields[${index}].defaultValue">
                              <option value="" <#if !field.defaultValue??> selected="selected"</#if>></option>
                              <option value="true" <#if (field.defaultValue!"")=='true'> selected="selected"</#if>>true</option>
                              <option value="false" <#if (field.defaultValue!"")=='false'> selected="selected"</#if>>false</option>
                            </select>
                      </div>
                <#else>
                    <input id="fVal_${tableSchema.name}_${index}" class="fval form-control form-control-sm" name="fields[${index}].defaultValue" value="${field.defaultValue!}"/>
                </#if>
            </div>

            <#if field.index??>
                <small><@sourceSample field.index tableSchema.name fieldsIndex/></small>

                <div id="fTIdx${fieldsIndex}" class="sample mappingText">
                    <small class="mx-lg-3">${t('manage.mapping.translation')}:</small>
                    <small>
                        <a href="dataPackageFieldTranslation.do?r=${resource.shortname}&mid=${mid}&field=${field.field.name}">
                            <#if (((field.translation?size)!0)>0)>
                                ${t("manage.overview.mappings.fields", (field.translation?size)!0)}
                            <#else>
                                ${t("button.add")}<#t>
                            </#if>
                        </a>
                    </small>
                </div>
            </#if>
        </div>
    </#macro>

    <div class="container px-0">
        <#include "/inc/action_alerts.ftl">
    </div>

    <form id="mappingForm" class="needs-validation" action="dataPackageMapping.do" method="post">
<@csrfField/>
        <div class="container-fluid border-bottom">
            <div class="container bg-body border rounded-2 mb-4">
                <div class="container p-3 my-3">
                    <div class="text-center fs-smaller">
                        <nav style="--bs-breadcrumb-divider: url(&#34;data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='8' height='8'%3E%3Cpath d='M2.5 0L1 1.5 3.5 4 1 6.5 2.5 8l4-4-4-4z' fill='currentColor'/%3E%3C/svg%3E&#34;);" aria-label="breadcrumb">
                            <ol class="breadcrumb justify-content-center mb-0">
                                <li class="breadcrumb-item"><a href="/manage/">${t("breadcrumb.manage")}</a></li>
                                <li class="breadcrumb-item"><a href="resource?r=${resource.shortname}">${t("breadcrumb.manage.overview")}</a></li>
                                <li class="breadcrumb-item active" aria-current="page">${t("breadcrumb.manage.overview.mapping")}</li>
                            </ol>
                        </nav>
                    </div>

                    <div class="text-center">
                        <h1 class="pt-2 text-gbif-header fs-2 fw-400 text-center">
                            ${t('manage.mapping.title')}<#t>
                        </h1>

                        <div class="text-center fs-smaller">
                            <a href="resource.do?r=${resource.shortname}" title="${resource.title!resource.shortname}">${resource.title!resource.shortname}</a>
                        </div>

                        <div class="my-2">
                            <input type="submit" value="${t("button.save")?markup_string}" id="save" name="save" class="button btn btn-sm btn-outline-gbif-primary top-button"/><#t>

                            <div class="btn-group btn-group-sm" role="group">
                                <button id="btnGroup" type="button" class="btn btn-sm btn-outline-gbif-primary dropdown-toggle align-self-start top-button" data-bs-toggle="dropdown" aria-expanded="false">
                                    ${t("button.options")}<#t>
                                </button>
                                <ul class="dropdown-menu" aria-labelledby="btnGroup" style="">
                                    <li>
                                        <a id="toggleFields" class="button btn btn-sm btn-outline-secondary w-100 dropdown-button">${t('manage.mapping.hideEmpty')}</a>
                                    </li>
                                    <li>
                                        <input type="submit" value="${t("button.delete")?markup_string}" id="delete" name="delete" class="confirm btn btn-sm btn-outline-gbif-danger w-100 dropdown-button"/><#t>
                                    </li>
                                </ul>
                            </div>

                            <input type="submit" value="${t("button.back")?markup_string}" id="cancel" name="cancel" class="button btn btn-sm btn-outline-secondary top-button"/><#t>
                        </div>

                        <p class="mt-3 mb-0 text-smaller fst-italic">
                            <#assign adminSchemaTitle = t("admin.dataPackageSchema.title")?markup_string>
                            <#assign textArg1><a href="source.do?r=${resource.shortname}&id=${mapping.source.name}" title="${t('manage.overview.source.data')}">
                                        ${mapping.source.name}
                                    </a></#assign><#assign textArg3><a href="${baseURL}/admin/dataPackage.do?id=${mapping.dataPackageSchema.identifier!}#anchor-${(mapping.dataPackageTableSchemaName.name)!}" target="_blank">${(mapping.dataPackageSchema.name)!}/${(mapping.dataPackageTableSchemaName.name)!}</a></#assign>${t('manage.mapping.intro1', textArg1, adminSchemaTitle?lower_case, textArg3)}<#t>
                        </p>

                        <#if dataPackageSchema.version != resource.dataPackageVersion>
                        <p class="text-smaller text-gbif-danger">
                            <#assign textArg4><b>${dataPackageSchema.name}</b></#assign><#assign textArg5><b>v${resource.dataPackageVersion!"unknown"}</b></#assign><#assign textArg6><b>v${dataPackageSchema.version}</b></#assign>${t('manage.overview.mappings.schemaChanged', textArg4, textArg5, textArg6)}<#t>
                        </p>
                        </#if>
                    </div>
                </div>
            </div>
        </div>

        <main class="container main-content-container">
            <div class="mt-0 mb-5 px-1">
                <div class="row g-3">
                    <div>
                        <input type="hidden" name="r" value="${resource.shortname}" />
                        <input type="hidden" name="id" value="${mapping.dataPackageSchema.identifier}" />
                        <input type="hidden" name="mid" value="${mid!}" />
                    </div>

                    <#-- Filter and required mapping -->
                    <div class="border-bottom mb-2 text-smaller">
                        <div id="filterSection" class="mappingRow">

                            <div class="row py-3 g-2 mappingFiler">
                                <div class="col-lg-1 pt-1" id="filter">
                                    <@popoverPropertyInfo "manage.mapping.info" />
                                    <strong>${t('manage.mapping.filter')}</strong>
                                </div>

                                <div class="col-lg-3">
                                    <select name="mapping.filter.filterTime" id="mapping.filter.filterTime" class="form-select form-select-sm">
                                        <#list mapping.filter.filterTimes?keys as filterTime>
                                            <option value="${filterTime}" <#if (mapping.filter.filterTime!"")==filterTime> selected="selected"</#if>>${filterTime}</option>
                                        </#list>
                                    </select>
                                </div>

                                <div class="col-lg-4">
                                    <select id="filterName" name="mapping.filter.column" class="form-select form-select-sm">
                                        <option value="" <#if !mapping.filter.column??> selected="selected"</#if>></option>
                                        <#list columns as c>
                                            <option value="${c_index}" <#if c_index==mapping.filter.column!-999> selected="selected"</#if>>${c}</option>
                                        </#list>
                                    </select>
                                </div>

                                <div class="col-lg-2">
                                    <select id="filterComp" name="mapping.filter.comparator" class="form-select form-select-sm">
                                        <option value="" <#if !mapping.filter.comparator??> selected="selected"</#if>></option>
                                        <#list comparators as c>
                                            <option value="${c}" <#if c==mapping.filter.comparator!""> selected="selected"</#if>>${c}</option>
                                        </#list>
                                    </select>
                                </div>

                                <div class="col-lg-2">
                                    <input id="filterParam" name="mapping.filter.param" class="form-control form-control-sm" value="${mapping.filter.param!}" />
                                </div>
                            </div>

                        </div>
                    </div>

                    <div id="sections">
                        <#list dataPackageSchema.tableSchemas as tableSchema>
                            <#if (mapping.dataPackageTableSchemaName.name)?? && mapping.dataPackageTableSchemaName.name == tableSchema.name>
                                <div id="${tableSchema.name}" class="mt-lg-3">
                                    <h4 class="pb-2 mb-2 pt-2 text-gbif-header-2 fs-5 fw-400">
                                        ${tableSchema.title}
                                    </h4>
                                    <#list fields as field>
                                        <@showField tableSchema field field_index/>
                                    </#list>
                                </div>
                            </#if>
                        </#list>

                        <#if (nonMapped?size>0)>
                            <span class="anchor anchor-base" id="anchor-nonmapped"></span>
                            <div class="mt-5" style="height: 100vh; min-height: 200px;">
                                <h4 id="nonmapped" class="pb-2 mb-2 pt-2 text-gbif-header-2 fs-5 fw-400">
                                    ${t("manage.mapping.no.mapped.title")}<#t>
                                </h4>
                                <p>${t("manage.mapping.schema.no.mapped.columns")}:</p>

                                <div class="text-smaller">
                                    <#list nonMapped as col>
                                        <#if col?has_content>
                                            <span class="unmapped-field"><strong>${col}</strong></span><#sep> </#sep>
                                        </#if>
                                    </#list>
                                </div>
                            </div>
                        </#if>
                    </div>
                </div>
            </div>
        </main>

    </form>

    <#include "/inc/footer.ftl"/>
