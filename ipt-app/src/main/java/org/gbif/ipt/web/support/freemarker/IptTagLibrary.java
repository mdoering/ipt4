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
package org.gbif.ipt.web.support.freemarker;

import org.gbif.ipt.i18n.Messages;

import java.util.Map;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * The subset of the Struts {@code s} tag library that IPT4 templates may still use. Tags are added as milestones port
 * the pages that need them; any other tag fails rendering, so leftovers cannot go unnoticed.
 */
public final class IptTagLibrary implements TemplateHashModel {

  private final Map<String, TemplateModel> tags;

  public IptTagLibrary(Messages messages) {
    this.tags = Map.of("text", new TextDirective(messages), "param", new ParamDirective());
  }

  @Override
  public TemplateModel get(String key) throws TemplateModelException {
    TemplateModel tag = tags.get(key);
    if (tag == null) {
      throw new TemplateModelException("Struts tag s." + key + " is not supported by IPT4 yet");
    }
    return tag;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }
}
