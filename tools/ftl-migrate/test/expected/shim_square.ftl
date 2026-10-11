[#ftl output_format="HTML"]
<p>[#assign textArg1]<i>x</i>[/#assign]${t("basic.createdByOn", (resource.creator.name)!, textArg1)}</p>
[#assign title = t("menu.about")?markup_string]
[@fieldError cssClass="invalid-feedback" fieldName="modeSelected"/]
${(exceptionStack)!}[#t]
