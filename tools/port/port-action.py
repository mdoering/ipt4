#!/usr/bin/env python3
"""Ports an IPT 3 Struts action to an IPT4 page bean with the smallest diff.

Usage: tools/port/port-action.py <legacy-repo> <baseline> <legacy path> <target package> <target dir>

- BaseAction/POSTAction become ActionPage/PostActionPage, the action gets @PageBean,
- the (SimpleTextProvider, AppConfig, RegistrationManager) constructor head becomes (PageContext ctx),
- Struts' @StrutsParameter keeps its name (org.gbif.ipt.web.support.action.StrutsParameter), @JSON goes,
- RequireManagerInterceptor.isAuthorized becomes ResourceAccessPolicy.canManage.
The result still needs a look: anything Struts-specific left over fails to compile.
"""
import re
import subprocess
import sys
from pathlib import Path

legacy, baseline, path, package, target_dir = sys.argv[1:6]
src = subprocess.run(["git", "-C", legacy, "show", f"{baseline}:{path}"], check=True, capture_output=True,
                     text=True).stdout

s = re.sub(r"^package [\w.]+;", f"package {package};", src, count=1, flags=re.M)
s = s.replace("import org.gbif.ipt.action.BaseAction;", "import org.gbif.ipt.web.support.action.ActionPage;")
s = s.replace("import org.gbif.ipt.action.POSTAction;", "import org.gbif.ipt.web.support.action.PostActionPage;")
s = re.sub(r"\bextends BaseAction\b", "extends ActionPage", s)
s = re.sub(r"\bextends POSTAction\b", "extends PostActionPage", s)
s = re.sub(r"\bBaseAction\.", "ActionPage.", s)
s = s.replace("import org.apache.struts2.interceptor.parameter.StrutsParameter;",
              "import org.gbif.ipt.web.support.action.StrutsParameter;")
s = re.sub(r"^import org\.apache\.struts2\.json\.annotations\.JSON;\n", "", s, flags=re.M)
s = re.sub(r"^\s*@JSON(\([^)]*\))?\s*\n", "", s, flags=re.M)
s = s.replace("import org.gbif.ipt.struts2.SimpleTextProvider;", "import org.gbif.ipt.i18n.SimpleTextProvider;")
s = s.replace("import org.gbif.ipt.struts2.RequireManagerInterceptor;",
              "import org.gbif.ipt.service.manage.ResourceAccessPolicy;")
s = s.replace("RequireManagerInterceptor.isAuthorized(", "ResourceAccessPolicy.canManage(")
# constructor head
s = re.sub(r"(public \w+\()\s*SimpleTextProvider \w+,\s*AppConfig \w+,\s*RegistrationManager \w+(,?)",
           lambda m: m.group(1) + "\n      PageContext ctx" + m.group(2), s)
s = re.sub(r"super\(\s*\w+,\s*\w+,\s*\w+(,?)", lambda m: "super(ctx" + m.group(1), s)
# imports needed by the new head
s = re.sub(r"(^package [\w.]+;\n)",
           r"\1\nimport org.gbif.ipt.web.support.action.PageBean;\nimport org.gbif.ipt.web.support.page.PageContext;",
           s, count=1, flags=re.M)
# Spring creates page beans; @Inject on the constructor stays valid (jakarta.inject)
s = re.sub(r"^(public (abstract )?class \w+)", r"@PageBean\n\1", s, count=1, flags=re.M)
s = re.sub(r"^@PageBean\n(public abstract class)", r"\1", s, flags=re.M)

# Struts' ActionSupport without IPT 3's BaseAction (JSON and stream actions): a plain page bean
if re.search(r"\bextends ActionSupport\b", s):
    s = s.replace("import org.apache.struts2.ActionSupport;\n", "")
    s = re.sub(r" extends ActionSupport", "", s)
    s = re.sub(r"\n  @Override\n  public String execute\(\)", "\n  public String execute()", s)
    s = re.sub(r"\breturn (SUCCESS|ERROR|INPUT|NONE);", r"return ActionPage.\1;", s)
    s = re.sub(r"(^package [\w.]+;\n)", r"\1\nimport org.gbif.ipt.web.support.action.ActionPage;", s, count=1,
               flags=re.M)
s = s.replace("import org.apache.struts2.ServletActionContext;\n", "")
s = s.replace("ServletActionContext.getRequest()", "req")
# ActionPage provides cfg; ported constructors no longer receive one
s = s.replace("    this.cfg = cfg;\n", "")
s = re.sub(r"\n\s*private (final )?AppConfig cfg;", "", s)
# actions in BaseAction's own package had no import for it
for base in ("ActionPage", "PostActionPage"):
    if re.search(rf"\bextends {base}\b|\b{base}\.", s) and f"web.support.action.{base};" not in s:
        s = re.sub(r"(^package [\w.]+;\n)", rf"\1\nimport org.gbif.ipt.web.support.action.{base};", s, count=1,
                   flags=re.M)
# every ported action is renamed XxxAction -> XxxPage, also where other ported actions refer to it
s = re.sub(r"\b(\w+)Action\b(?!Support|Context|Invocation|Logger|ErrorCollector|ValidationResult)",
           lambda m: m.group(1) + "Page" if m.group(1)[:1].isupper() and m.group(1) not in ("POST", "Base") else m.group(0),
           s)
s = s.replace("import org.gbif.ipt.action.portal.", "import org.gbif.ipt.web.portal.")
s = s.replace("import org.gbif.ipt.action.manage.", "import org.gbif.ipt.web.manage.")
s = s.replace("import org.gbif.ipt.action.admin.", "import org.gbif.ipt.web.admin.")
s = s.replace("org.gbif.ipt.web.portal.OrganizedTaxonomic", "org.gbif.ipt.model.metadata.OrganizedTaxonomic")
if "OrganizedTaxonomicCoverage" in s and "import org.gbif.ipt.model.metadata.OrganizedTaxonomicCoverage;" not in s:
    s = re.sub(r"(^package [\w.]+;\n)", r"\1\nimport org.gbif.ipt.model.metadata.OrganizedTaxonomicCoverage;\n"
               r"import org.gbif.ipt.model.metadata.OrganizedTaxonomicKeywords;", s, count=1, flags=re.M)
out = Path(target_dir) / Path(path).name.replace("Action.java", "Page.java")
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(s)
print(out)
