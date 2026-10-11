[#ftl output_format="HTML"]
[#include "/inc/header_setup.ftl"]
[#include "/macros/forms.ftl"]
[#assign setupStepIndex = 4]

<div class="container px-0">
    [#if warnings?size == 0 && actionErrors?size == 0]
        [#include "/inc/action_alerts.ftl"]
    [#else]
        [#include "/inc/action_alerts_warnings.ftl"]
        [#include "/inc/action_alerts_errors.ftl"]
    [/#if]
</div>

<form action="setupComplete.do" method="post" class="needs-validation" novalidate>
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
                        <input type="submit" value="${t("button.continue")?markup_string}" id="continue" name="continue" class="btn btn-sm btn-outline-gbif-primary top-button"/>[#t]
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
                    [#if warnings?size == 0 && actionErrors?size == 0]
                        <p>${t("admin.config.setup3.welcome")}</p>
                    [#else]
                        <p>${t("admin.config.setup3.welcomeWithIssues")}</p>
                    [/#if]
                </div>
            </div>
        </main>
    </div>

</form>
</div>


[#include "/inc/footer.ftl"]
