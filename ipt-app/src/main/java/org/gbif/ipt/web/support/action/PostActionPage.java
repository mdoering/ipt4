/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.ipt.web.support.action;

import org.gbif.ipt.web.support.page.PageContext;

import org.apache.commons.lang3.StringUtils;

/**
 * IPT 3's {@code POSTAction}: GET shows the form ({@link #INPUT}); POST saves, or deletes when {@code delete} was sent;
 * {@code cancel} cancels. Validation only runs on POST.
 */
public class PostActionPage extends ActionPage {

  protected boolean delete = false;
  protected boolean notFound = false;
  protected boolean validate = true;
  protected String defaultResult = INPUT;

  public PostActionPage(PageContext ctx) {
    super(ctx);
  }

  public String delete() throws Exception {
    return SUCCESS;
  }

  @Override
  public String execute() throws Exception {
    // if notFound was set to true during prepare() the supplied id parameter didn't exist - return a 404!
    if (notFound) {
      return NOT_FOUND;
    }
    if (cancel) {
      return cancel();
    }
    if (isHttpPost()) {
      String result = delete ? delete() : save();
      return notFound ? NOT_FOUND : result;
    }
    return defaultResult;
  }

  public String save() throws Exception {
    return SUCCESS;
  }

  public boolean isDelete() {
    return delete;
  }

  @StrutsParameter
  public void setDeleteFlag(String delete) {
    this.delete = StringUtils.trimToNull(delete) != null;
    if (this.delete) {
      validate = false;
    }
  }

  @Override
  public void setCancel(String cancel) {
    this.cancel = StringUtils.trimToNull(cancel) != null;
    if (this.cancel) {
      validate = false;
    }
  }

  @StrutsParameter
  public void setValidate(boolean validate) {
    this.validate = validate;
  }

  @Override
  public void validate() {
    if (isHttpPost() && validate) {
      validateHttpPostOnly();
    }
  }

  public void validateHttpPostOnly() {
  }
}
