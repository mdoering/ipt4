    <#if "${newUser!}"=="no">
        <#assign newUserPage=false/>
        <#assign editUserPage=true/>
    <#else>
        <#assign newUserPage=true/>
        <#assign editUserPage=false/>
    </#if>

    <#include "/inc/header.ftl">
    <title><#if editUserPage>${t("admin.user.title.edit")}<#else>${t("admin.user.title.new")}</#if></title>
    <script src="${baseURL}/js/jconfirmation.jquery.js"></script>
    <link rel="stylesheet" href="${baseURL}/styles/select2/select2-4.0.13.min.css">
    <link rel="stylesheet" href="${baseURL}/styles/select2/select2-bootstrap4.min.css">
    <link rel="stylesheet" href="${baseURL}/styles/smaller-inputs.css">
    <script src="${baseURL}/js/select2/select2-4.0.13.full.min.js"></script>

    <script>
        $(document).ready(function(){
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

            $("#user\\.role").select2({
                placeholder: '',
                language: {
                    noResults: function () {
                        return '${selectNoResultsFound}';
                    }
                },
                width:"100%",
                minimumResultsForSearch: 15,
                theme: 'bootstrap4'});

            $("#save").on("click", displayProcessing);
        });
    </script>
    <#assign currentMenu = "admin"/>
    <#include "/inc/menu.ftl">
    <#include "/macros/forms.ftl">

    <div class="container px-0">
        <#include "/inc/action_alerts.ftl">
    </div>

    <div class="container-fluid bg-body border-bottom">
        <div class="container bg-body border rounded-2 mb-4">
            <div class="container my-3 p-3">
                <div class="text-center fs-smaller">
                    <nav style="--bs-breadcrumb-divider: url(&#34;data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='8' height='8'%3E%3Cpath d='M2.5 0L1 1.5 3.5 4 1 6.5 2.5 8l4-4-4-4z' fill='currentColor'/%3E%3C/svg%3E&#34;);" aria-label="breadcrumb">
                        <ol class="breadcrumb justify-content-center mb-0">
                            <li class="breadcrumb-item"><a href="${baseURL}/admin/">${t("breadcrumb.admin")}</a></li>
                            <li class="breadcrumb-item"><a href="${baseURL}/admin/users.do">${t("breadcrumb.admin.users")}</a></li>
                            <li class="breadcrumb-item active" aria-current="page">${t("breadcrumb.admin.users.user")}</li>
                        </ol>
                    </nav>
                </div>

                <div class="text-center">
                    <h1 class="pb-2 mb-0 pt-2 text-gbif-header fs-2 fw-normal">
                        <#if editUserPage>
                            ${user.firstname!} ${user.lastname!}
                        <#else>
                            ${t("admin.user.title.new")}<#t>
                        </#if>
                    </h1>

                    <#if editUserPage>
                        <div class="text-smaller">
                            <a href="mailto:${user.email!}">${user.email!}</a>
                        </div>
                    </#if>

                    <div class="mt-2">
                        <input type="submit" value="${t("button.save")?markup_string}" id="save" name="save" class="button btn btn-sm btn-outline-gbif-primary top-button" tabindex="${7}" form="newuser"/><#t>
                        <#if editUserPage>
                            <div class="btn-group btn-group-sm" role="group">
                                <button id="btnGroup" tabindex=8 type="button" class="btn btn-sm btn-outline-gbif-primary dropdown-toggle align-self-start top-button" data-bs-toggle="dropdown" aria-expanded="false">
                                    ${t("button.options")}<#t>
                                </button>
                                <ul class="dropdown-menu" aria-labelledby="btnGroup" style="">
                                    <li>
                                        <input type="submit" value="${t("button.resetPassword")?markup_string}" id="resetPassword" name="resetPassword" class="confirmPasswordReset button btn btn-sm btn-outline-gbif-danger w-100 dropdown-button" form="newuser"/><#t>
                                    </li>
                                    <li>
                                        <input type="submit" value="${t("button.delete")?markup_string}" id="deleteFlag" name="deleteFlag" class="userConfirmDeletion btn btn-sm btn-outline-gbif-danger w-100 dropdown-button" form="newuser"/><#t>
                                    </li>
                                </ul>
                            </div>
                        </#if>
                        <input type="submit" value="${t("button.cancel")?markup_string}" id="cancel" name="cancel" class="button btn btn-sm btn-outline-secondary top-button" tabindex="${9}" form="newuser"/><#t>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <main class="container main-content-container">
        <div class="my-3 p-3">
            <p>
                ${t("admin.user.intro")}<#t>
            </p>
            <p>
                ${t("admin.user.intro2")}<#t>
            </p>

            <form id="newuser" class="needs-validation" action="user.do" method="post">
<@csrfField/>
                <div class="row g-3 mt-2">
                    <#if editUserPage>
                        <input type="hidden" name="id" value="${(user.email)!}" id="id"/><#t>
                    </#if>

                    <div class="col-md-6">
                        <@input tabindex=1 name="user.firstname" value=user.firstname! />
                    </div>

                    <div class="col-md-6">
                        <@input tabindex=2 name="user.lastname" value=user.lastname! />
                    </div>

                    <#if !id?has_content>
                        <div class="col-md-6">
                            <@input tabindex=3 name="user.email" value=user.email! disabled=id?has_content/>
                        </div>
                    </#if>

                    <div class="col-md-6">
                        <@select name="user.role" tabindex=4 value=(user.role)! javaGetter=false options={"User":"user.roles.user", "Manager":"user.roles.manager", "Publisher":"user.roles.publisher", "Admin":"user.roles.admin"}/>
                    </div>

                    <#if newUserPage>
                        <div class="col-md-6">
                            <@input name="user.password" type="password" tabindex=5 />
                        </div>
                        <div class="col-md-6">
                            <@input name="password2" i18nkey="user.password2" type="password" tabindex=6/>
                        </div>
                    </#if>
                </div>
            </form>
        </div>
    </main>

    <#include "/inc/footer.ftl">
