    <#include "/inc/header.ftl">
    <title>${t("admin.home.manageUsers")}</title>
    <script src="${baseURL}/js/jconfirmation.jquery.js"></script>
    <#assign currentMenu = "admin"/>
    <#include "/inc/menu.ftl">
    <#include "/macros/usersTable.ftl"/>
    <script src="${baseURL}/js/jquery/jquery.dataTables-1.13.6.min.js"></script>
    <script>
        $(document).ready(function(){
            var initConfirmationModal = function () {
                if ($('.userConfirmDeletion').length > 0 && $('.confirmPasswordReset').length > 0) {
                    $('.userConfirmDeletion').jConfirmAction({
                        titleQuestion: "${t("basic.confirm")}",
                        question: "${t("admin.user.delete.confirmation.message")}",
                        yesAnswer: "${t("basic.yes")}",
                        cancelAnswer: "${t("basic.no")}",
                        buttonType: "danger",
                        baseUrl: "${baseURL}"
                    });

                    $('.confirmPasswordReset').jConfirmAction({
                        titleQuestion: "${t("basic.confirm")}",
                        question: "${t("admin.user.resetPassword.confirmation.message")}",
                        yesAnswer: "${t("basic.yes")}",
                        cancelAnswer: "${t("basic.no")}",
                        buttonType: "danger",
                        baseUrl: "${baseURL}"
                    });
                } else {
                    setTimeout(initConfirmationModal, 100); // check again in a moment
                }
            }

            setTimeout(initializeConfirmationWindows, 1000);

            function initializeConfirmationWindows() {
                $(".paginate_button").click(function () {
                    $('.userConfirmDeletion').jConfirmAction({
                        titleQuestion: "${t("basic.confirm")}",
                        question: "${t("admin.user.delete.confirmation.message")}",
                        yesAnswer: "${t("basic.yes")}",
                        cancelAnswer: "${t("basic.no")}",
                        buttonType: "danger",
                        baseUrl: "${baseURL}"
                    });
                    $('.confirmPasswordReset').jConfirmAction({
                        titleQuestion: "${t("basic.confirm")}",
                        question: "${t("admin.user.resetPassword.confirmation.message")}",
                        yesAnswer: "${t("basic.yes")}",
                        cancelAnswer: "${t("basic.no")}",
                        buttonType: "danger",
                        baseUrl: "${baseURL}"
                    });
                });
            }

            initConfirmationModal();

            // Hack needed for Internet Explorer
            $('#create').click(function() {
                window.location='user.do';
            });
        });
    </script>

    <div class="container px-0">
        <#include "/inc/action_alerts.ftl">
    </div>

    <div class="container-fluid bg-body border-bottom">
        <div class="container border rounded-2 mb-4">
            <div class="container my-3 p-3">
                <div class="text-center fs-smaller">
                    <nav style="--bs-breadcrumb-divider: url(&#34;data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='8' height='8'%3E%3Cpath d='M2.5 0L1 1.5 3.5 4 1 6.5 2.5 8l4-4-4-4z' fill='currentColor'/%3E%3C/svg%3E&#34;);" aria-label="breadcrumb">
                        <ol class="breadcrumb justify-content-center mb-0">
                            <li class="breadcrumb-item"><a href="${baseURL}/admin/">${t("breadcrumb.admin")}</a></li>
                            <li class="breadcrumb-item">${t("breadcrumb.admin.users")}</li>
                        </ol>
                    </nav>
                </div>

                <div class="text-center">
                    <h1 class="pb-2 mb-0 pt-2 text-gbif-header fs-2 fw-normal">
                        ${t("admin.home.manageUsers")}<#t>
                    </h1>

                    <div class="mt-2">
                        <button id="create" class="btn btn-sm btn-outline-gbif-primary top-button">${t("button.create")}</button>
                        <a href="${baseURL}/admin/" class="btn btn-sm btn-outline-secondary top-button">
                            ${t("button.cancel")}<#t>
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <main class="container main-content-container border rounded-2 mt-4 mb-5">
        <div class="mb-3 py-4 px-5">
            <@usersTable numUsersShown=20 sEmptyTable="dataTables.sEmptyTable.users" columnToSortOn=0 sortOrder="asc" />
            <div id="tableContainer" class="table-responsive text-smaller pt-2"></div>
        </div>
    </main>

    <#include "/inc/footer.ftl">
