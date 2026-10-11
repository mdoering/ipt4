<#ftl output_format="HTML">
<p>${t("menu.about")}</p>
<p title="${t('basic.yes')}">${t("user.roles.${role?lower_case}")} ${t(step)}</p>
<span>${t("basic.createdByOn", (resource.creator.name)!, resource.created?date?string("MMM d, yyyy"))}</span>
${t("portal.rss", "${baseURL}/rss.do")}
${t("portal.node", '${cfg.portalUrl}/node/${node!"#"}')}
${t('manage.add', t('button.add'))}
<#assign textArg1><a href="${cfg.portalUrl}/installation/${key}" target="_blank">IPT</a></#assign><#assign textArg2><b>${name}</b></#assign>${t("admin.link", textArg1, textArg2)}
${t("help")?markup_string}
var role = '${t("user.roles.${r}")?markup_string?js_string?no_esc}';
<#assign schemaTitle = t("admin.dataPackageSchema.title")?markup_string>
<@s.text name="with.default">Default text</@s.text>
<input type="submit" value="${t("button.save")?markup_string}" id="save" name="save" class="btn"/>
<input type="submit" value="${t("button.cancel")?markup_string}" id="cp" name="change-password" class="btn" style="display: none;" form="f"/>
<input type="submit" value="${t("button.reserve")?markup_string}" id="reserveDoi" name="reserveDoi"<#if missing> disabled="disabled"</#if> class="btn"/>
<input type="hidden" name="r" value="${resource.shortname}" id="r"/>
<input type="hidden" name="organisation.key" value="${organisation.key!}" id="organisation.key"/>
<input type="hidden" name="organisation.name" value="${(organisation.name)!}" id="organisation.name"/>
<input type="hidden" name="eml.parties[0].role" value="${(eml.parties[0].role)!}" id="eml_parties_0__role"/>
<input type="file" name="file" id="file" class="form-control"/>
<input type="file" name="file" id="file" class="form-control"/>
<input type="file" name="" id="custom-data-input" class="d-none"/>
<@fieldError id="field-error-${name}" cssClass="invalid-feedback" fieldName="${name}"/>
${(exception.message)!}
<@s.property value="${name}"/>
<@s.select name="x" list="organisations"/>
