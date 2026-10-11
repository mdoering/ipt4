[#ftl output_format="HTML"]
<p>[@s.text name="basic.createdByOn"][@s.param]${(resource.creator.name)!}[/@s.param][@s.param]<i>x</i>[/@s.param][/@s.text]</p>
[@s.text var="title" name="menu.about"/]
[@s.fielderror cssClass="invalid-feedback" fieldName="modeSelected"/]
[@s.property value="%{exceptionStack}"/]
