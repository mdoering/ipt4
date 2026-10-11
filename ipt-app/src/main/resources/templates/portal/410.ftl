[#ftl output_format="HTML"]
[#include "/inc/header.ftl"/]
<title>${t("410.title")}</title>
[#include "/inc/menu.ftl"/]

<div class="container-fluid bg-body border-bottom">
    <div class="container my-3">
        [#include "/inc/action_alerts_warnings.ftl"]
        [#include "/inc/action_alerts_errors.ftl"]
    </div>

    <div class="container my-3 p-3">
        <div class="text-center fs-smaller">
            ${t("basic.error")}[#t]
        </div>

        <div class="text-center">
            <h1 class="pb-2 mb-0 pt-2 text-gbif-header fs-2 fw-normal">
                ${t("410.title")}[#t]
            </h1>
        </div>
    </div>
</div>

<main class="container">
    <div class="my-3 p-3">
        <p class="text-center">${t("410.body")}</p>
    </div>
</main>

[#include "/inc/footer.ftl"/]
