package org.gbif.ipt.web.support.freemarker;

import java.util.Map;
import org.gbif.ipt.i18n.Messages;
import org.springframework.stereotype.Component;

/** Template-wide variables: the Struts tag shim {@code s} and the {@code csrfField} directive. */
@Component
public class IptTemplateVariables implements FreeMarkerSharedVariables {

  private final Messages messages;

  public IptTemplateVariables(Messages messages) {
    this.messages = messages;
  }

  @Override
  public Map<String, Object> variables() {
    return Map.of("s", new IptTagLibrary(messages), "csrfField", new CsrfFieldDirective());
  }
}
