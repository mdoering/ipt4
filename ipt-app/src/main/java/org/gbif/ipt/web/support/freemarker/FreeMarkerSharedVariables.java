package org.gbif.ipt.web.support.freemarker;

import java.util.Map;

/** Contributes variables visible in every template, such as the {@code s} tag library. */
public interface FreeMarkerSharedVariables {

  Map<String, Object> variables();
}
