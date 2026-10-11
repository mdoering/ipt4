    <#include "/inc/header.ftl">
    <link rel="stylesheet" href="${baseURL}/styles/select2/select2-4.0.13.min.css">
    <link rel="stylesheet" href="${baseURL}/styles/select2/select2-bootstrap4.min.css">
    <link rel="stylesheet" href="${baseURL}/styles/smaller-inputs.css">
    <script src="${baseURL}/js/select2/select2-4.0.13.min.js"></script>
    <script src="${baseURL}/js/jconfirmation.jquery.js"></script>
    <script>
        $(document).ready(function(){
            $('.confirm').jConfirmAction({
                titleQuestion: "${t("basic.confirm")}",
                question: "${t("admin.organisation.delete.confirmation.message")}",
                yesAnswer: "${t("basic.yes")}",
                cancelAnswer: "${t("basic.no")}",
                buttonType: "danger",
                baseUrl: "${baseURL}"
            });

            $('select#organisation\\.key').select2({
                placeholder: '${action.getText("admin.organisation.name.select")}',
                language: {
                    noResults: function () {
                        return '${selectNoResultsFound}';
                    }
                },
                width: "100%",
                allowClear: true,
                theme: 'bootstrap4'
            });

            $('#organisation\\.key').change(function() {

                var organisationSelected = $('#organisation\\.key :selected');
                var orgName = organisationSelected.text();
                var organisationKey = organisationSelected.val();
                $('#organisation\\.name').val(orgName);
                $('#organisation\\.alias').val(orgName);

                var emailContent = 'Dear sir/madam,%0d%0d';
                emailContent += 'I am installing an Integrated Publishing Toolkit (IPT).%0d';
                emailContent += 'This tool allows me to create resources and assign these resources to organizations. %0d';
                emailContent += 'I would like to link your organization with my IPT, but for this I will need you to provide me with your organization\'s shared token%0d';
                emailContent += 'In case you don\'t know this information, you can open the following link in your browser to receive this information%0d%0d';
                emailContent += 'https://gbrds.gbif.org/registry/organisation/' + organisationKey + '?op=password%0d%0d';
                emailContent += 'Thank you for your attention.';

                if (organisationKey) {
                    var url = '${registryURL}organisation/' + $('#organisation\\.key :selected').val() + ".json";
                    $.getJSON(url, function (data) {

                        $('#organisation\\.primaryContactType').val(data.primaryContactType);
                        $('#organisation\\.primaryContactName').val(data.primaryContactName);
                        $('#organisation\\.primaryContactEmail').val(data.primaryContactEmail);
                        $('#organisation\\.nodeKey').val(data.nodeKey);
                        $('#organisation\\.nodeName').val(data.nodeName);

                        //Create a contact link to prefill an email to request a password from an Organisation
                        var contactLink = '<div class="mt-2">';
                        contactLink += '<a href=\"mailto:';
                        contactLink += data.primaryContactEmail;
                        contactLink += '?subject=';
                        contactLink += 'Shared token request for ';
                        contactLink += orgName;
                        contactLink += '&body=';
                        contactLink += emailContent;
                        contactLink += '\">';
                        contactLink += '<svg class="link-icon" focusable="false" aria-hidden="true" viewBox="0 0 24 24" data-testid="ContactSupportOutlinedIcon" tabindex="-1" title="ContactSupportOutlined"><path d="M15 4v7H5.17l-.59.59-.58.58V4h11m1-2H3c-.55 0-1 .45-1 1v14l4-4h10c.55 0 1-.45 1-1V3c0-.55-.45-1-1-1zm5 4h-2v9H6v2c0 .55.45 1 1 1h11l4 4V7c0-.55-.45-1-1-1z"></path></svg>';
                        contactLink += ' Click here to contact';
                        contactLink += '</a> ';
                        contactLink += orgName;
                        contactLink += "</div>";
                        $('#requestDetails').html(contactLink);
                    });
                } else {
                    // remove link
                    $("#requestDetails").empty();
                }
            });

            var doiRegistrationAgency = $('organisation\\.doiRegistrationAgency :selected').val();
            if (doiRegistrationAgency) {
                $(".doiAgencyField").css("display", "");
            }

            function hideDoiRegistrationFields() {
                $("#organisation\\.doiRegistrationAgency").val('');
                $("#organisation\\.agencyAccountUsername").val('');
                $("#organisation\\.agencyAccountPassword").val('');
                $("#organisation\\.doiPrefix").val('');
                $('#organisation\\.agencyAccountPrimary').attr('checked', false);
                $(".doiAgencyField").hide();
            }

            function showDoiRegistrationFields() {
                $(".doiAgencyField").show();
            }

            $('#organisation\\.doiRegistrationAgency').change(function() {
                var doiRegistrationAgency = $('#organisation\\.doiRegistrationAgency :selected').val();

                if (doiRegistrationAgency) {
                    showDoiRegistrationFields();
                } else {
                    hideDoiRegistrationFields();
                }
            });

            <#if !organisationWithDoiRegistrationAgencyPresent>
            $("#organisation\\.associatedWithDoiRegistrationAgency").change(function () {
                if (!$('#organisation\\.associatedWithDoiRegistrationAgency').is(':checked')) {
                    hideDoiRegistrationFields();
                } else {
                    showDoiRegistrationFields();
                }
            });
            </#if>

            $("#save").on("click", displayProcessing);
        });
    </script>
    <title>${t("title")}</title>

    <#assign currentMenu = "admin"/>
    <#include "/inc/menu.ftl">
    <#include "/macros/forms.ftl">
    <#include "/macros/popover.ftl">

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
                            <li class="breadcrumb-item"><a href="${baseURL}/admin/organisations.do">${t("breadcrumb.admin.organisations")}</a></li>
                            <li class="breadcrumb-item active" aria-current="page">${t("breadcrumb.admin.organisations.organisation")}</li>
                        </ol>
                    </nav>
                </div>

                <div class="text-center">
                    <h1 class="pb-2 mb-0 pt-2 text-gbif-header fs-2 fw-normal">
                        <#if id?has_content>
                            ${organisation.name!}
                        <#else>
                            ${t("admin.organisation.add.title")}<#t>
                        </#if>
                    </h1>

                    <#if organisation.key?has_content>
                        <div class="text-smaller">
                            <a href="${portalUrl}/publisher/${organisation.key}" target="_blank">${t("about.link")}</a>
                        </div>
                    </#if>

                    <div class="mt-2">
                        <input type="submit" value="${t("button.save")?markup_string}" id="save" name="save" class="button btn btn-sm btn-outline-gbif-primary top-button" form="organisationsForm"/><#t>
                        <#if id?has_content>
                            <input type="submit" value="${t("button.delete")?markup_string}" id="deleteFlag" name="deleteFlag" class="button confirm btn btn-sm btn-outline-gbif-danger top-button" form="organisationsForm"/><#t>
                        </#if>
                        <input type="submit" value="${t("button.cancel")?markup_string}" id="cancel" name="cancel" class="button btn btn-sm btn-outline-secondary top-button" form="organisationsForm"/><#t>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <main class="container main-content-container">
        <div class="my-3 p-3">
            <form id="organisationsForm" class="needs-validation" action="organisation.do" method="post" novalidate>
<@csrfField/>
                <div class="row g-3 mt-1">
                    <#if id?has_content>
                        <p>${organisation.description!}</p>

                        <input type="hidden" name="organisation.name" value="${organisation.name!}" id="organisation.name"/><#t>
                        <input type="hidden" name="organisation.key" value="${organisation.key!}" id="organisation.key"/><#t>
                        <input type="hidden" name="id" value="${(id)!}" id="id"/><#t>
                        <!-- preserve other fields -->
                        <input type="hidden" name="organisation.nodeName" value="${organisation.nodeName!}" id="organisation.nodeName"/><#t>
                        <input type="hidden" name="organisation.nodeKey" value="${organisation.nodeKey!}" id="organisation.nodeKey"/><#t>
                        <input type="hidden" name="organisation.primaryContactPhone" value="${organisation.primaryContactPhone!}" id="organisation.primaryContactPhone"/><#t>
                        <input type="hidden" name="organisation.primaryContactEmail" value="${organisation.primaryContactEmail!}" id="organisation.primaryContactEmail"/><#t>
                        <input type="hidden" name="organisation.primaryContactAddress" value="${organisation.primaryContactAddress!}" id="organisation.primaryContactAddress"/><#t>
                        <input type="hidden" name="organisation.primaryContactDescription" value="${organisation.primaryContactDescription!}" id="organisation.primaryContactDescription"/><#t>
                        <input type="hidden" name="organisation.primaryContactName" value="${organisation.primaryContactName!}" id="organisation.primaryContactName"/><#t>
                        <input type="hidden" name="organisation.primaryContactType" value="${organisation.primaryContactType!}" id="organisation.primaryContactType"/><#t>
                        <input type="hidden" name="organisation.homepageURL" value="${organisation.homepageURL!}" id="organisation.homepageURL"/><#t>
                        <input type="hidden" name="organisation.description" value="${organisation.description!}" id="organisation.description"/><#t>
                    <#else>
                        <input type="hidden" name="organisation.name" value="${(organisation.name)!}" id="organisation.name"/><#t>
                        <!-- preserve other fields -->
                        <input type="hidden" name="organisation.nodeName" value="${organisation.nodeName!}" id="organisation.nodeName"/><#t>
                        <input type="hidden" name="organisation.nodeKey" value="${organisation.nodeKey!}" id="organisation.nodeKey"/><#t>
                        <input type="hidden" name="organisation.primaryContactPhone" value="${organisation.primaryContactPhone!}" id="organisation.primaryContactPhone"/><#t>
                        <input type="hidden" name="organisation.primaryContactEmail" value="${organisation.primaryContactEmail!}" id="organisation.primaryContactEmail"/><#t>
                        <input type="hidden" name="organisation.primaryContactAddress" value="${organisation.primaryContactAddress!}" id="organisation.primaryContactAddress"/><#t>
                        <input type="hidden" name="organisation.primaryContactDescription" value="${organisation.primaryContactDescription!}" id="organisation.primaryContactDescription"/><#t>
                        <input type="hidden" name="organisation.primaryContactName" value="${organisation.primaryContactName!}" id="organisation.primaryContactName"/><#t>
                        <input type="hidden" name="organisation.primaryContactType" value="${organisation.primaryContactType!}" id="organisation.primaryContactType"/><#t>
                        <input type="hidden" name="organisation.homepageURL" value="${organisation.homepageURL!}" id="organisation.homepageURL"/><#t>
                        <input type="hidden" name="organisation.description" value="${organisation.description!}" id="organisation.description"/><#t>

                        <div class="col-lg-6">
                            <div class="form-group">
                                <#assign selectOrganisationInfo>
                                    ${t("admin.registration.intro")}&nbsp;${t("admin.organisation.add.intro2")}
                                </#assign>
                                <#assign selectOrganisationInfo = selectOrganisationInfo?markup_string>
                                <div class="d-flex text-smaller">
                                    <a tabindex="0" role="button"
                                       class="popover-link"
                                       data-bs-toggle="popover"
                                       data-bs-trigger="focus"
                                       data-bs-html="true"
                                       data-bs-content="${selectOrganisationInfo}">
                                        <i class="bi bi-info-circle text-gbif-primary"></i>
                                    </a>&nbsp;
                                    <label for="organisation.key" class="form-label">
                                        ${t("admin.organisation.key")} <span class="text-gbif-danger">&#42;</span>
                                    </label>
                                </div>
                                <select name="organisation.key" id="organisation.key" class="form-select">
                                    <#assign selectedKey = ((organisation.key)!"")?string>
                                    <#list (organisations)![] as o>
                                        <#assign optionKey = ((o.key)!"")?string>
                                        <option value="${optionKey}"<#if optionKey == selectedKey> selected="selected"</#if>>${(o.name)!}</option>
                                    </#list>
                                </select>
                                <@fieldError id="field-error-organisation.key" cssClass="invalid-feedback list-unstyled field-error my-1" fieldName="organisation.key"/>
                            </div>
                            <div id="requestDetails" class="mt-0 text-smaller"></div>
                        </div>
                    </#if>

                    <div class="col-lg-6">
                        <@input name="organisation.alias" value=organisation.alias! i18nkey="admin.organisation.alias" type="text"/>
                    </div>
                </div>

                <div class="row g-3 mt-1">
                    <div class="col-lg-6">
                        <@input name="organisation.password" value="" i18nkey="admin.organisation.password" type="password" requiredField=true />
                    </div>
                </div>

                <div class="row g-3 mt-1">
                    <div class="col-12">
                        <@checkbox name="organisation.canHost" value=organisation.canHost i18nkey="admin.organisation.canPublish" help="i18n"/>
                    </div>

                    <#assign doiRegistrationAgencyAssociation = organisation.doiRegistrationAgency?has_content || organisation.associatedWithDoiRegistrationAgency />

                    <div class="col-12">
                        <@checkbox name="organisation.associatedWithDoiRegistrationAgency" value=organisation.associatedWithDoiRegistrationAgency i18nkey="admin.organisation.doiRegistrationAgencyAssociation" help="i18n"/>
                    </div>

                    <div class="col-lg-6 doiAgencyField" <#if !organisation.doiRegistrationAgency??>style="display: none;"</#if>>
                        <@select name="organisation.doiRegistrationAgency" value="${organisation.doiRegistrationAgency!''}" options=doiRegistrationAgencies help="i18n" i18nkey="admin.organisation.doiRegistrationAgency" includeEmpty=true />
                    </div>

                    <div class="col-lg-6 doiAgencyField" <#if !organisation.doiRegistrationAgency??>style="display: none;"</#if> >
                        <@input name="organisation.agencyAccountUsername" value=organisation.agencyAccountUsername! i18nkey="admin.organisation.doiRegistrationAgency.username" help="i18n" type="text"/>
                    </div>

                    <div class="col-lg-6 doiAgencyField" <#if !organisation.doiRegistrationAgency??>style="display: none;"</#if> >
                        <@input name="organisation.agencyAccountPassword" value="" i18nkey="admin.organisation.doiRegistrationAgency.password" help="i18n" type="password"/>
                    </div>

                    <div class="col-lg-6 doiAgencyField" <#if !organisation.doiRegistrationAgency??>style="display: none;"</#if> >
                        <@input name="organisation.doiPrefix" value=organisation.doiPrefix! i18nkey="admin.organisation.doiRegistrationAgency.prefix" help="i18n" type="text"/>
                    </div>

                    <div class="col-12 doiAgencyField" <#if !organisation.doiRegistrationAgency??>style="display: none;"</#if> >
                        <@checkbox name="organisation.agencyAccountPrimary" value=organisation.agencyAccountPrimary i18nkey="admin.organisation.doiAccount.activated" help="i18n"/>
                    </div>
                </div>
            </form>
        </div>
    </main>

    <#include "/inc/footer.ftl">
