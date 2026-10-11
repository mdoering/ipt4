[#ftl output_format="HTML"]
[#include "/inc/header_setup.ftl"]
[#include "/macros/forms.ftl"]
[#assign setupStepIndex = 2]

<div class="container px-0">
    [#include "/inc/action_alerts.ftl"]
</div>

<form action="setupMode.do" method="post" class="needs-validation" novalidate>
[@csrfField/]
    <div class="container-fluid bg-body border-bottom">
        <div class="container bg-body border rounded-2 mb-4">
            <div class="container my-3 p-3">
                <div class="text-center">
                    <div class="text-center fs-smaller">
                        <span>${t("admin.config.setup.common.setup")}</span>
                    </div>

                    <h1 class="pb-2 mb-0 pt-2 text-gbif-header fs-2 fw-normal">
                        ${t("admin.config.setup.common.ipt")}[#t]
                    </h1>
                    <h1 class="pb-2 mb-0 text-gbif-header-light fs-5 fw-normal">
                        ${t("admin.config.setup.common.tagline")}[#t]
                    </h1>

                    <div class="mt-2">
                        <input type="submit" value="${t("button.save")?markup_string}" id="save" name="save" class="btn btn-sm btn-outline-gbif-primary top-button"/>[#t]
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="container bd-layout main-content-container">
        <main class="bd-main">
            <div class="bd-toc py-3">
                [#include "/inc/setup_sidebar.ftl"]
            </div>

            <div class="bd-content">
                <div class="my-3 p-3">
                    <h5 class="text-gbif-header-2 pb-2 mt-2 fw-400">
                        ${t("admin.config.setup2.mode.title")}[#t]
                    </h5>

                    <p>${t("admin.config.setup2.mode.help")}</p>
                    <p>${t("admin.config.setup2.mode.test")}</p>
                    <p>${t("admin.config.setup2.mode.production")}</p>
                    <p>${t("admin.config.setup2.mode")} <span class="text-gbif-danger">&#42;</span></p>

                    [#list modes as mode]
                        <div class="form-check form-check-inline pb-2">
                            <input class="form-check-input" type="radio" name="modeSelected" id="mode${mode}" aria-describedby="field-error-mode" [#if mode??]value="${mode}"[/#if] [#if modeSelected?? && mode == modeSelected] checked [/#if]>
                            <label class="form-check-label" for="mode${mode}">
                                ${t("admin.config.setup2.mode.${mode?lower_case}.label")}[#t]
                            </label>
                        </div>
                    [/#list]
                    [@fieldError cssClass="invalid-feedback list-unstyled radio-error radio-name-modeSelected my-0" fieldName="modeSelected"/]

                </div>
            </div>
        </main>
    </div>

</form>
</div>


[#include "/inc/footer.ftl"]
