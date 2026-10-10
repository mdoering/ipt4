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
                        <span>[@s.text name="admin.config.setup.common.setup"/]</span>
                    </div>

                    <h1 class="pb-2 mb-0 pt-2 text-gbif-header fs-2 fw-normal">
                        [@s.text name="admin.config.setup.common.ipt"/]
                    </h1>
                    <h1 class="pb-2 mb-0 text-gbif-header-light fs-5 fw-normal">
                        [@s.text name="admin.config.setup.common.tagline"/]
                    </h1>

                    <div class="mt-2">
                        [@s.submit cssClass="btn btn-sm btn-outline-gbif-primary top-button" name="continue" key="button.continue"/]
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
                        <p>[@s.text name="admin.config.setup3.welcome"/]</p>
                    [#else]
                        <p>[@s.text name="admin.config.setup3.welcomeWithIssues"/]</p>
                    [/#if]
                </div>
            </div>
        </main>
    </div>

</form>
</div>


[#include "/inc/footer.ftl"]
