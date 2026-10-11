<#--
versionsTable macro: Generates a data table that has pagination.
- numVersionsShown: The number of versions shown in the table
- sEmptyTable: The message shown when there are no resource records in the table
- columnToSortOn: The column to sort on by default (index starting at 0)
- sortOrder: The sort order of the columnToSortOn
- baseURL: IPT baseURL
- shortname: The shortname of the resource
-->
<#macro versionsTable numVersionsShown sEmptyTable baseURL shortname>
    <script charset="utf-8">

      <#assign isDataPackage = resource.isDataPackage() />

        /* version history list */
        var aDataSet = [
            <#list resource.getVersionHistory() as v>
            /* only show released versions. The version must be public unless user has manager rights in which case they can see all released versions */
            <#if v.released?? && ((v.publicationStatus == 'PUBLIC' || v.publicationStatus == 'REGISTERED') || managerRights) >
            [<#if (version?? && v.version == version.toPlainString()) || (!version?? && v.version == resource.emlVersion.toPlainString())>'<img class="latestVersion" src="${baseURL}/images/dataTables/forward_enabled_hover.png"/>${v.version!}'<#else>'<img class="latestVersionHidden" src="${baseURL}/images/dataTables/forward_enabled_hover.png"/><a href="${baseURL}/resource?r=${shortname}&amp;v=${v.version!}">${v.version!}</a>'</#if>,
                '${(v.released?date?string("yyyy-MM-dd HH:mm:ss"))!""}',
                '${v.recordsPublished}',
                <#if v.changeSummary?has_content>"${v.changeSummary?replace("\'", "\\'")?replace("\"", '\\"')?replace("[\r\n]+", "<br>",'r')}&nbsp;<#if managerRights && !isPreviewPage><a href='${baseURL}/manage/history.do?r=${resource.shortname}&v=${v.version}'>${t('button.edit')}</a></#if>"<#else>"${t("publishing.changeSummary.default")}&nbsp;<#if managerRights && !isPreviewPage><a href='${baseURL}/manage/history.do?r=${resource.shortname}&v=${v.version}'>${t('button.edit')}</a></#if>"</#if>,
                <#if v.doi?? && v.status=="PUBLIC">'${v.doi!}'<#else>''</#if>,
                <#if v.modifiedBy??>'${v.modifiedBy.firstname?replace("\'", "\\'")?replace("\"", '\\"')!} ${v.modifiedBy.lastname?replace("\'", "\\'")?replace("\"", '\\"')!}'<#else>""</#if>]<#if v_has_next>,</#if>
            </#if>
            </#list>
        ];

        $(document).ready(function() {
            $('#vtableContainer').html( '<table class="hover row-border dataTable compact" id="rtable"></table>' );
            $('#rtable').dataTable( {
                "bFilter": false,
                "aaData": aDataSet,
                "iDisplayLength": ${numVersionsShown},
                "bLengthChange": false,
                "bAutoWidth": false,
                "bSort": false,
                "oLanguage": {
                    "sEmptyTable": "${t(sEmptyTable)}",
                    "sZeroRecords": "${t("dataTables.sZeroRecords.versions")}",
                    "sInfo": "${t("dataTables.sInfo")}",
                    "sInfoEmpty": "${t("dataTables.sInfoEmpty")}",
                    "sInfoFiltered": "${t("dataTables.sInfoFiltered")}",
                    "oPaginate": {
                        "sNext": "${t("pager.next")}",
                        "sPrevious": "${t("pager.previous")}"

                    }
                },
                "aoColumns": [
                    { "sTitle": "${t("manage.overview.published.version")}", "bSearchable": false},
                    { "sTitle": "${t("manage.overview.published.released")}", "bSearchable": false},
                    { "sTitle": "${t("portal.home.records")}", "bSearchable": false},
                    { "sTitle": "${t("portal.home.summary")}", "bSearchable": false},
                    { "sTitle": "${t("portal.home.doi")}", "bSearchable": false},
                    { "sTitle": "${t("portal.home.modifiedBy")}", "bSearchable": false}
                ],
                "aoColumnDefs": [
                    { 'bSortable': false, 'aTargets': [ 0,1,2,3,4,5] }
                ]
            } );
        } );
    </script>
</#macro>
