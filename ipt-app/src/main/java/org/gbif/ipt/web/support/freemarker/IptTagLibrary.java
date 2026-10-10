package org.gbif.ipt.web.support.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Map;
import org.gbif.ipt.i18n.Messages;

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
