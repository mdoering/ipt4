    <#setting number_format="#####.##">
    <#include "/inc/header.ftl">
    <title>${t('manage.metadata.methods.title')}</title>
    <#include "/macros/metadata.ftl"/>
    <script>
        $(document).ready(function(){
            $("#removeLink-0").hide();
            if("${eml.methodSteps?size}" == 0){
                $("#plus").click();
            }

            $('#metadata-section').change(function () {
                var metadataSection = $('#metadata-section').find(':selected').val()
                $(location).attr('href', 'metadata-' + metadataSection + '.do?r=${resource.shortname!r!}');
            });

            // scroll to the error if present
            var invalidElements = $(".is-invalid");

            if (invalidElements !== undefined && invalidElements.length > 0) {
                var invalidElement = invalidElements.first();
                var pos = invalidElement.offset().top - 100;
                // scroll to the element
                $('body, html').animate({scrollTop: pos});
            }

            // reordering
            function initAndGetSortable(selector) {
                return sortable(selector, {
                    forcePlaceholderSize: true,
                    placeholderClass: 'border',
                    handle: '.handle'
                });
            }

            const sortable_keywords = initAndGetSortable('#items');

            sortable_keywords[0].addEventListener('sortupdate', changeInputNamesAfterDragging);
            sortable_keywords[0].addEventListener('drag', dragScroll);

            function dragScroll(e) {
                var cursor = e.pageY;
                var parentWindow = parent.window;
                var pixelsToTop = $(parentWindow).scrollTop();
                var screenHeight = $(parentWindow).height();

                if ((cursor - pixelsToTop) > screenHeight * 0.9) {
                    parentWindow.scrollBy(0, (screenHeight / 30));
                } else if ((cursor - pixelsToTop) < screenHeight * 0.1) {
                    parentWindow.scrollBy(0, -(screenHeight / 30));
                }
            }

            function changeInputNamesAfterDragging(e) {
                displayProcessing();
                var contactItems = $("#items div.item");

                contactItems.each(function (index) {
                    var elementId = $(this)[0].id;

                    $("div#" + elementId + " textarea").attr("name", "eml.methodSteps[" + index + "]");
                });

                hideProcessing();
            }

            makeSureResourceParameterIsPresentInURL('${resource.shortname}');
        });
    </script>
    <#assign currentMetadataPage = "methods"/>
    <#assign currentMenu="manage"/>
    <#include "/inc/menu.ftl">
    <#include "/macros/forms.ftl"/>

    <div class="container px-0">
        <#include "/inc/action_alerts.ftl">
    </div>

    <form class="needs-validation track-unsaved" action="metadata-${section}.do" method="post" novalidate>
<@csrfField/>
        <div class="container-fluid bg-body border-bottom">
            <div class="container bg-body border rounded-2 mb-4">
                <div class="container my-3 p-3">
                    <div class="text-center fs-smaller">
                        <nav style="--bs-breadcrumb-divider: url(&#34;data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='8' height='8'%3E%3Cpath d='M2.5 0L1 1.5 3.5 4 1 6.5 2.5 8l4-4-4-4z' fill='currentColor'/%3E%3C/svg%3E&#34;);" aria-label="breadcrumb">
                            <ol class="breadcrumb justify-content-center mb-0">
                                <li class="breadcrumb-item"><a href="${baseURL}/manage/">${t("breadcrumb.manage")}</a></li>
                                <li class="breadcrumb-item"><a href="resource?r=${resource.shortname}">${t("breadcrumb.manage.overview")}</a></li>
                                <li class="breadcrumb-item active" aria-current="page">${t("breadcrumb.manage.overview.metadata")}</li>
                            </ol>
                        </nav>
                    </div>

                    <div class="text-center">
                        <h1 class="py-2 mb-0 text-gbif-header fs-2 fw-normal">
                            ${t('manage.metadata.methods.title')}<#t>
                        </h1>
                    </div>

                    <div class="text-center fs-smaller">
                        <a href="resource.do?r=${resource.shortname}" title="${resource.title!resource.shortname}">${resource.title!resource.shortname}</a>
                    </div>

                    <div class="text-center mt-2">
                        <input type="submit" value="${t("button.save")?markup_string}" id="save" name="save" class="button btn btn-sm btn-outline-gbif-primary top-button"/><#t>
                        <button type="button" class="btn btn-sm btn-outline-secondary top-button" onclick="window.history.back();">
                            ${t("button.back")}<#t>
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <#include "metadata_section_select.ftl"/>

        <div class="container-fluid bg-body">
            <div class="container bd-layout main-content-container">
                <main class="bd-main">
                    <div class="bd-toc mt-4 mb-5 ps-3 mb-lg-5 text-muted">
                        <#include "eml_sidebar.ftl"/>
                    </div>

                    <div class="bd-content">
                        <div class="my-md-3 p-3">
                            <p class="mb-0">
                                ${t('manage.metadata.methods.intro')}<#t>
                            </p>

                            <div id="sampling" class="row g-3 mt-1">
                                <div class="col-lg-12">
                                    <@text name="eml.studyExtent"  i18nkey="eml.studyExtent" help="i18n" />
                                </div>

                                <div class="col-lg-12">
                                    <@text name="eml.sampleDescription" i18nkey="eml.sampleDescription" help="i18n" />
                                </div>
                            </div>

                            <div id="qualitycontrol" class="row g-3 mt-1">
                                <div class="col-lg-12">
                                    <@text name="eml.qualityControl" i18nkey="eml.qualityControl" help="i18n"/>
                                </div>
                            </div>

                            <div id="items">
                                <#if eml.methodSteps??>
                                    <#list eml.methodSteps as item>
                                        <div id="item-${item_index}" class="item row g-3 border-bottom pb-3 mt-1">
                                            <div class="handle mt-2 d-flex justify-content-end">
                                                <a id="removeLink-${item_index}" class="removeLink metadata-action-link custom-link" href="">
                                                    <span>
                                                        <svg viewBox="0 0 24 24" class="link-icon link-icon-danger">
                                                            <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                        </svg>
                                                    </span>
                                                    <span>${t('manage.metadata.removethis')} ${t('manage.metadata.methods.item')}</span>
                                                </a>
                                            </div>
                                            <div class="col-lg-12">
                                                <@text name="eml.methodSteps[${item_index}]" i18nkey="eml.methodSteps" help="i18n" />
                                            </div>
                                        </div>
                                    </#list>
                                </#if>
                            </div>

                            <div class="addNew col-12 mt-2">
                                <a id="plus" href="" class="metadata-action-link custom-link">
                                    <span>
                                        <svg viewBox="0 0 24 24" class="link-icon link-icon-primary">
                                            <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"></path>
                                        </svg>
                                    </span>
                                    <span>${t('manage.metadata.addnew')} ${t('manage.metadata.methods.item')}</span>
                                </a>
                            </div>

                            <!-- internal parameter -->
                            <input name="r" type="hidden" value="${resource.shortname}" />

                            <div id="baseItem" class="item row g-3 border-bottom pb-3 mt-1" style="display:none">
                                <div class="handle d-flex justify-content-end mt-2">
                                    <a id="removeLink" class="removeLink metadata-action-link custom-link" href="">
                                        <span>
                                            <svg viewBox="0 0 24 24" class="link-icon link-icon-danger">
                                                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                            </svg>
                                        </span>
                                        <span>${t('manage.metadata.removethis')} ${t('manage.metadata.methods.item')}</span>
                                    </a>
                                </div>
                                <div class="col-lg-12">
                                    <@text name="" i18nkey="eml.methodSteps" help="i18n" />
                                </div>
                            </div>

                        </div>
                    </div>
                </main>
            </div>
        </div>
    </form>

    <#include "/manage/eml/unsaved_changes_modal.ftl">

    <#include "/inc/footer.ftl">
