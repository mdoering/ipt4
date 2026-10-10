package org.gbif.ipt.web.support.freemarker;

import freemarker.core.Environment;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Collects the {@code s.param} values of the innermost enclosing {@code s.text} within one template rendering. */
final class ParamStack {

  private static final Object KEY = new Object();

  private ParamStack() {}

  @SuppressWarnings("unchecked")
  static Deque<List<String>> of(Environment env) {
    Deque<List<String>> stack = (Deque<List<String>>) env.getCustomState(KEY);
    if (stack == null) {
      stack = new ArrayDeque<>();
      env.setCustomState(KEY, stack);
    }
    return stack;
  }
}
