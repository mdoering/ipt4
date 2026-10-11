<#ftl output_format="HTML">
S1:<@s.submit cssClass="btn" name="save" key="button.save"/>
S2:<@s.submit form="profileData" name="change-password" key="button.cancel" cssStyle="display: none;"/>
P1:<@s.property value="html"/>
P2:<@s.property value="%{page.title}"/>
P3:<@s.property value="no.such.thing"/>
F1:<@s.fielderror id="field-error-email" cssClass="invalid-feedback" fieldName="email"/>
F2:<@s.fielderror fieldName="password"/>
C1:<@s.checkbox name="user.admin" cssClass="form-check-input" fieldValue="true" value="true" disabled=false/>
C2:<@s.checkbox name="flag" value="false"/>
L1:<@s.select id="org" class="form-select" name="org" list=options value="b" emptyOption=true/>
L2:<@s.select name="lang" list=beans listKey="code" listValue="label" value="fr"/>
U1:<@s.file name="file" cssClass="form-control" key="manage.resource.create.file" />
U2:<@s.file id="custom-data-input" cssClass="d-none" />
P4:<@s.property value="beans[1].label"/>
P5:<@s.property value="stringKeyed['b']"/>|<@s.property value="beans[9].label"/>|
