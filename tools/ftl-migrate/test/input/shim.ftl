<#ftl output_format="HTML">
<p><@s.text name="menu.about"/></p>
<p title="<@s.text name='basic.yes'/>"><@s.text name="user.roles.${role?lower_case}"/> <@s.text name="${step}"/></p>
<span><@s.text name="basic.createdByOn"><@s.param>${(resource.creator.name)!}</@s.param><@s.param>${resource.created?date?string("MMM d, yyyy")}</@s.param></@s.text></span>
<@s.text name="portal.rss"><@s.param>${baseURL}/rss.do</@s.param></@s.text>
<@s.text name="portal.node"><@s.param>${cfg.portalUrl}/node/${node!"#"}</@s.param></@s.text>
<@s.text name='manage.add'><@s.param><@s.text name='button.add'/></@s.param></@s.text>
<@s.text name="admin.link">
    <@s.param><a href="${cfg.portalUrl}/installation/${key}" target="_blank">IPT</a></@s.param>
    <@s.param><b>${name}</b></@s.param>
</@s.text>
<@s.text name="help" escapeHtml=true/>
var role = '<@s.text name="user.roles.${r}" escapeJavaScript="true"/>';
<@s.text var="schemaTitle" name="admin.dataPackageSchema.title"/>
<@s.text name="with.default">Default text</@s.text>
<@s.submit cssClass="btn" name="save" key="button.save"/>
<@s.submit cssClass="btn" form="f" name="change-password" id="cp" key="button.cancel" cssStyle="display: none;"/>
<@s.submit cssClass="btn" name="reserveDoi" key="button.reserve" disabled="${missing?string}"/>
<@s.hidden name="r" value="${resource.shortname}"/>
<@s.hidden name="organisation.key" id="organisation.key" value=organisation.key! required="true" />
<@s.hidden id="organisation.name" name="organisation.name" />
<@s.hidden name="eml.parties[0].role"/>
<@s.file cssClass="form-control" name="file"/>
<@s.file name="file" cssClass="form-control" key="manage.resource.create.file" />
<@s.file id="custom-data-input" cssClass="d-none" />
<@s.fielderror id="field-error-${name}" cssClass="invalid-feedback" fieldName="${name}"/>
<@s.property value="%{exception.message}"/>
<@s.property value="${name}"/>
<@s.select name="x" list="organisations"/>
