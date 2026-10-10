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

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.ServletRequestDataBinder;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Runs a page the way IPT 3's Struts interceptor stack ran an action: {@code prepare()}, bind the request parameters
 * that {@link StrutsParameter} allows, {@code validate()} (except for input/back/cancel/list), and when there are no
 * errors the requested method; with errors the result is {@link ActionPage#INPUT}.
 */
@Component
public class ActionRunner {

  /** Methods Struts' validation interceptor skipped. */
  private static final Set<String> NOT_VALIDATED = Set.of("input", "back", "cancel", "list");

  @FunctionalInterface
  public interface ActionMethod {
    String call() throws Exception;
  }

  private final Map<Class<?>, String[]> allowedFields = new ConcurrentHashMap<>();

  public String run(ActionPage page, HttpServletRequest request, String methodName, ActionMethod method)
      throws Exception {
    page.prepare();
    bind(page, request);
    if (!NOT_VALIDATED.contains(methodName)) {
      page.validate();
    }
    if (page.hasErrors()) {
      return ActionPage.INPUT;
    }
    return method.call();
  }

  void bind(ActionPage page, HttpServletRequest request) {
    ServletRequestDataBinder binder = new ServletRequestDataBinder(page);
    binder.setAllowedFields(allowedFields.computeIfAbsent(page.getClass(), ActionRunner::allowed));
    binder.setFieldMarkerPrefix("__checkbox_");
    binder.setAutoGrowCollectionLimit(100_000);
    binder.bind(request);
    BindingResult result = binder.getBindingResult();
    for (FieldError error : result.getFieldErrors()) {
      // Struts' xwork.default.invalid.fieldvalue
      page.addFieldError(error.getField(), "Invalid field value for field \"" + error.getField() + "\".");
    }
  }

  /** Property patterns for the setters, getters and fields annotated with {@link StrutsParameter}. */
  static String[] allowed(Class<?> type) {
    Set<String> patterns = new LinkedHashSet<>();
    for (Method m : type.getMethods()) {
      StrutsParameter p = m.getAnnotation(StrutsParameter.class);
      if (p == null) {
        continue;
      }
      String name = m.getName();
      if (name.startsWith("set") && m.getParameterCount() == 1) {
        patterns.add(Introspector.decapitalize(name.substring(3)));
      } else if ((name.startsWith("get") || name.startsWith("is")) && m.getParameterCount() == 0) {
        String property = Introspector.decapitalize(name.substring(name.startsWith("get") ? 3 : 2));
        addNested(patterns, property, p.depth());
      }
    }
    for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field f : c.getDeclaredFields()) {
        StrutsParameter p = f.getAnnotation(StrutsParameter.class);
        if (p != null && !Modifier.isStatic(f.getModifiers())) {
          patterns.add(f.getName());
          addNested(patterns, f.getName(), p.depth());
        }
      }
    }
    return patterns.toArray(String[]::new);
  }

  private static void addNested(Set<String> patterns, String property, int depth) {
    if (depth > 0) {
      patterns.add(property + ".*");
      patterns.add(property + "[*");
    }
  }
}
