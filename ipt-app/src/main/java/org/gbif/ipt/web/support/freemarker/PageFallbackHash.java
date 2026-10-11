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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/** Template root: the Spring model first, then the page bean, then OGNL-style {@code get<Name>()} on the page bean. */
final class PageFallbackHash extends SimpleHash {

  /** The one page method templates call by bare name; OGNL resolved other bare names as properties only. */
  private static final String GET_TEXT = "getText";

  /** The hashes Spring's FreeMarkerView adds next to the model. */
  private static final Set<String> SPRING_HASHES =
      Set.of("JspTaglibs", "Application", "Session", "Request", "RequestParameters");

  private final SimpleHash base;
  private final Set<String> modelKeys;
  private final Object page;
  private final TemplateHashModel pageModel;

  /**
   * @param modelKeys the model's names: they come before the page; request, session and application attributes,
   *                  which {@code base} also resolves, come after it (Struts searched the action first)
   */
  PageFallbackHash(SimpleHash base, Set<String> modelKeys, Object page, ObjectWrapper wrapper) {
    super(wrapper);
    this.base = base;
    this.modelKeys = modelKeys;
    this.page = page;
    try {
      this.pageModel = (TemplateHashModel) wrapper.wrap(page);
    } catch (TemplateModelException e) {
      throw new IllegalStateException("Cannot expose page bean " + page.getClass().getName(), e);
    }
  }

  @Override
  public TemplateModel get(String key) throws TemplateModelException {
    if (modelKeys.contains(key) || SPRING_HASHES.contains(key)) {
      return base.get(key);
    }
    TemplateModel value = pageValue(key);
    // request, session and application attributes
    return value != null ? value : base.get(key);
  }

  private TemplateModel pageValue(String key) throws TemplateModelException {
    TemplateModel value = pageModel.get(key);
    if (value != null && (!(value instanceof TemplateMethodModel) || GET_TEXT.equals(key))) {
      return value;
    }
    Method getter = findGetter(page.getClass(), key);
    if (getter == null) {
      return publicField(key);
    }
    try {
      return getObjectWrapper().wrap(getter.invoke(page));
    } catch (ReflectiveOperationException e) {
      throw new TemplateModelException("Cannot read " + key + " from " + page.getClass().getName(), e);
    }
  }

  /** OGNL also read public fields of the action (IPT 3's HealthAction relied on it). */
  private TemplateModel publicField(String key) throws TemplateModelException {
    try {
      java.lang.reflect.Field field = page.getClass().getField(key);
      return getObjectWrapper().wrap(field.get(page));
    } catch (NoSuchFieldException e) {
      return null;
    } catch (IllegalAccessException e) {
      throw new TemplateModelException("Cannot read " + key + " from " + page.getClass().getName(), e);
    }
  }

  /** Java beans name {@code getBaseURL()} "BaseURL"; OGNL (and so IPT 3 templates) call it {@code baseURL}. */
  private static Method findGetter(Class<?> type, String key) {
    if (key.isEmpty()) {
      return null;
    }
    String capitalized = Character.toUpperCase(key.charAt(0)) + key.substring(1);
    for (String name : List.of("get" + capitalized, "is" + capitalized)) {
      try {
        return type.getMethod(name);
      } catch (NoSuchMethodException ignored) {
        // try the next accessor name
      }
    }
    return null;
  }

  @Override
  public void put(String key, Object value) {
    base.put(key, value);
  }

  @Override
  public void put(String key, boolean value) {
    base.put(key, value);
  }

  @Override
  public void remove(String key) {
    base.remove(key);
  }

  @Override
  public boolean containsKey(String key) {
    return base.containsKey(key);
  }

  @Override
  public int size() {
    return base.size();
  }

  @Override
  public boolean isEmpty() {
    return base.isEmpty();
  }

  @Override
  public TemplateCollectionModel keys() {
    return base.keys();
  }

  @Override
  public TemplateCollectionModel values() {
    return base.values();
  }

  @Override
  public KeyValuePairIterator keyValuePairIterator() {
    return base.keyValuePairIterator();
  }

  @Override
  @SuppressWarnings({"rawtypes", "deprecation"})
  public Map toMap() throws TemplateModelException {
    return base.toMap();
  }
}
