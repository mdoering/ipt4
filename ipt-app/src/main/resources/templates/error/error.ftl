[#ftl output_format="HTML"]
[#include "/inc/header.ftl"/]
<title>${t("error.header.title")}</title>
[#include "/inc/menu.ftl"/]

<div class="container px-0">
    [#include "/inc/action_alerts_warnings.ftl"]
    [#include "/inc/action_alerts_errors.ftl"]
</div>

<div class="container-fluid bg-body border-bottom">
    <div class="container bg-body border rounded-2 mb-4">
        <div class="container my-3 p-3">
            <div class="text-center fs-smaller">
                ${t("basic.error")}[#t]
            </div>

            <div class="text-center">
                <h1 class="pb-2 mb-0 pt-2 text-gbif-header fs-2 fw-normal">
                    ${t("error.title")}[#t]
                </h1>
            </div>
        </div>
    </div>
</div>

<main class="container main-content-container">
    <div class="my-3 p-3">
        <p>
            ${t("error.body")}[#t]
        </p>

        <pre class="mb-0">
            ${(exception.message)!}[#t]
        </pre>

        [#if adminRights]
            <p>
                <a href="${baseURL}/admin/logs.do">${t("error.view.logs")}</a>
            </p>
        [/#if]

        <p>
            ${t("error.report")}[#t]
        </p>

        <p>
            ${t("error.thanks")}[#t]
        </p>
    </div>

    <div class="my-3 p-3">
        <h5 class="pb-2 mb-2 pt-2 text-gbif-header-2 fw-400">
            ${t("error.details.title")}[#t]
        </h5>

        <pre>
            ${(exceptionStack)!}[#t]
        </pre>
    </div>
</main>

[#include "/inc/footer.ftl"/]
