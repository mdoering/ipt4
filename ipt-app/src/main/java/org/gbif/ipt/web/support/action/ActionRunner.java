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

import org.gbif.ipt.model.DataPackageField;
import org.gbif.ipt.model.DataPackageSchema;
import org.gbif.ipt.model.DataPackageTableSchema;
import org.gbif.ipt.model.Extension;
import org.gbif.ipt.model.ExtensionProperty;
import org.gbif.ipt.model.Ipt;
import org.gbif.ipt.model.Network;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.Source;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.Vocabulary;
import org.gbif.ipt.model.VocabularyConcept;
import org.gbif.ipt.web.support.SanitizeHtmlFilter;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Runs a page the way IPT 3's Struts interceptor stack ran an action: {@code prepare()}, a {@code cancel} parameter
 * marks the page as cancelled, bind the request parameters that {@link StrutsParameter} allows, {@code validate()}
 * (except for input/back/cancel/list), and when there are no errors the requested method; with errors the result is
 * {@link ActionPage#INPUT}.
 */
@Component
public class ActionRunner {

  /** Methods Struts' validation interceptor skipped. */
  private static final Set<String> NOT_VALIDATED = Set.of("input", "back", "cancel", "list");

  @FunctionalInterface
  public interface ActionMethod {
    String call() throws Exception;
  }

  /**
   * Objects the IPT shares between pages and users. Requests never bind into them through another object (IPT 3 let
   * a manager post {@code resource.creator.role}, {@code mapping.extension.title} or {@code mapping.source.name});
   * a page's own object at the top level (the account form's {@code user}, the source form's {@code source}) stays
   * bindable.
   */
  private static final Set<Class<?>> SHARED = Set.of(User.class, Organisation.class, Extension.class,
      ExtensionProperty.class, Vocabulary.class, VocabularyConcept.class, DataPackageSchema.class,
      DataPackageTableSchema.class, DataPackageField.class, Resource.class, Ipt.class, Network.class, Source.class);
  private static final Pattern INDEX = Pattern.compile("\\[[^]]*]");
  private static final Pattern LIST_INDEX = Pattern.compile("\\[(\\d+)]");

  private final Map<Class<?>, Rules> rules = new ConcurrentHashMap<>();
  private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
  private final UploadStore uploads;

  public ActionRunner(UploadStore uploads) {
    this.uploads = uploads;
  }

  public String run(ActionPage page, HttpServletRequest request, String methodName, ActionMethod method)
      throws Exception {
    page.setInvokedMethod(methodName);
    page.prepare();
    boolean changing = !SAFE_METHODS.contains(request.getMethod());
    // IPT 3's SetupAndCancelInterceptor: any cancel parameter cancels (here only a POST's: cancelling changes state)
    if (changing && request.getParameterMap().containsKey(ActionPage.CANCEL)) {
      page.setCancel("true");
    }
    List<UploadedFile> files = upload(page, request);
    try {
      // a GET binds the page's own top-level parameters (mid, v, ...) only: nested paths reach live objects, and a
      // GET carries no CSRF token
      bind(page, request, !changing);
      // IPT 3's XssFieldErrorInterceptor: what SanitizeHtmlFilter had to clean is a field error, the form returns
      if (request.getAttribute(SanitizeHtmlFilter.XSS_FLAGGED_FIELDS_ATTR) instanceof Map<?, ?> flagged
          && !flagged.isEmpty()) {
        flagged.forEach((field, message) -> page.addFieldError(String.valueOf(field), String.valueOf(message)));
        return page instanceof ValidationErrorAware aware
            ? aware.actionErrorOccurred(ActionPage.INPUT) : ActionPage.INPUT;
      }
      if (!NOT_VALIDATED.contains(methodName)) {
        page.validate();
      }
      if (page.hasErrors()) {
        return page instanceof ValidationErrorAware aware
            ? aware.actionErrorOccurred(ActionPage.INPUT) : ActionPage.INPUT;
      }
      return method.call();
    } finally {
      page.afterAction();
      // Struts removed uploads after the action; pages copy what they keep
      files.forEach(UploadedFile::delete);
    }
  }

  /** Struts' action file upload interceptor: the request's files go to the page before parameters are bound. */
  private List<UploadedFile> upload(ActionPage page, HttpServletRequest request) throws IOException {
    // through request wrappers (e.g. DoSuffixFilter's)
    MultipartHttpServletRequest multipart = WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
    if (!(page instanceof UploadedFilesAware aware) || multipart == null) {
      return List.of();
    }
    List<UploadedFile> files = new ArrayList<>();
    for (MultipartFile file : multipart.getMultiFileMap().values().stream().flatMap(List::stream).toList()) {
      if (!file.isEmpty()) {
        files.add(uploads.store(file));
      }
    }
    if (!files.isEmpty()) {
      aware.withUploadedFiles(files);
    }
    return files;
  }

  /**
   * A request names list indexes, and the binder creates every element up to each (auto-grow): the elements a request
   * can create are limited to 10 per parameter, at least 10,000 (a 600-contact form needs a few thousand).
   */
  static void checkListGrowth(Map<String, String[]> parameters) {
    Map<String, Long> largest = new HashMap<>();
    for (String name : parameters.keySet()) {
      Matcher index = LIST_INDEX.matcher(name);
      while (index.find()) {
        long size = index.group(1).length() > 9 ? Long.MAX_VALUE / 2 : Long.parseLong(index.group(1)) + 1;
        largest.merge(name.substring(0, index.start()), size, Math::max);
      }
    }
    long budget = Math.max(10_000L, 10L * parameters.size());
    long total = 0;
    for (long size : largest.values()) {
      total += size;
      if (total > budget) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many list elements in the request");
      }
    }
  }

  void bind(ActionPage page, HttpServletRequest request, boolean topLevelOnly) {
    checkListGrowth(request.getParameterMap());
    Rules rules = this.rules.computeIfAbsent(page.getClass(), type -> rules(type, page.extraAllowedFields()));
    ServletRequestDataBinder binder = new ServletRequestDataBinder(page) {
      @Override
      protected boolean isAllowed(String field) {
        return super.isAllowed(field) && !(topLevelOnly && (field.contains(".") || field.contains("[")))
            && rules.withinDepth(field) && !forbidden(page, field);
      }
    };
    binder.setAllowedFields(rules.patterns());
    binder.setFieldMarkerPrefix("__checkbox_");
    // IPT 3's *-conversion.properties
    binder.setConversionService(StrutsConversions.SERVICE);
    binder.setAutoGrowCollectionLimit(100_000);
    binder.bind(request);
    BindingResult result = binder.getBindingResult();
    for (FieldError error : result.getFieldErrors()) {
      // Struts' xwork.default.invalid.fieldvalue
      page.addFieldError(error.getField(), "Invalid field value for field \"" + error.getField() + "\".");
    }
  }


  /**
   * Local files are never set from requests (Spring, unlike Struts, converts strings to files, so a source could be
   * pointed at any file on the server).
   */
  private static final Set<Class<?>> NEVER_BOUND = Set.of(File.class, Path.class);

  /**
   * Whether the path is refused: it cannot be followed on the page's objects, or it reaches a file or path, or below
   * the page's own properties a shared object, judged by the declared types (getter and setter) and by the objects
   * actually there (a property declared as {@code Source} or {@code Term} holds a {@code TextFileSource} or a shared
   * {@code ExtensionProperty}).
   */
  static boolean forbidden(Object page, String field) {
    String[] segments = field.split("\\.");
    Object current = page;
    ResolvableType declared = ResolvableType.forClass(page.getClass());
    for (int i = 0; i < segments.length; i++) {
      String name = INDEX.matcher(segments[i]).replaceAll("");
      Class<?> owner = current != null ? current.getClass() : declared.resolve();
      PropertyDescriptor pd = owner == null ? null : BeanUtils.getPropertyDescriptor(owner, name);
      if (pd == null || (pd.getReadMethod() == null && pd.getWriteMethod() == null)) {
        return true;
      }
      List<Class<?>> types = new ArrayList<>();
      types.add(pd.getPropertyType());
      if (pd.getWriteMethod() != null) {
        types.add(pd.getWriteMethod().getParameterTypes()[0]);
      }
      ResolvableType type = pd.getReadMethod() != null
          ? ResolvableType.forMethodReturnType(pd.getReadMethod(), owner)
          : ResolvableType.forMethodParameter(pd.getWriteMethod(), 0, owner);
      Object value = current != null && pd.getReadMethod() != null ? read(pd.getReadMethod(), current) : null;
      Matcher index = INDEX.matcher(segments[i]);
      while (index.find()) {
        type = elementType(type);
        value = element(value, index.group());
        types.add(type.resolve());
      }
      types.add(type.resolve());
      if (value != null) {
        types.add(value.getClass());
      }
      for (Class<?> t : types) {
        if (t == null) {
          continue;
        }
        if (NEVER_BOUND.stream().anyMatch(never -> never.isAssignableFrom(t))) {
          return true;
        }
        if (i > 0 && SHARED.stream().anyMatch(shared -> shared.isAssignableFrom(t))) {
          return true;
        }
      }
      current = value;
      declared = type;
    }
    return false;
  }

  @Nullable
  private static Object read(Method getter, Object owner) {
    try {
      return getter.invoke(owner);
    } catch (ReflectiveOperationException | RuntimeException e) {
      return null;
    }
  }

  /** The element an index ([3], ['key']) selects, or null where there is none (yet). */
  @Nullable
  private static Object element(@Nullable Object container, String index) {
    String key = index.substring(1, index.length() - 1).replaceAll("^['\"]|['\"]$", "");
    try {
      if (container instanceof Map<?, ?> map) {
        return map.get(key);
      }
      int position = Integer.parseInt(key);
      if (container instanceof List<?> list) {
        return position < list.size() ? list.get(position) : null;
      }
      if (container != null && container.getClass().isArray()) {
        return position < java.lang.reflect.Array.getLength(container)
            ? java.lang.reflect.Array.get(container, position) : null;
      }
    } catch (NumberFormatException e) {
      return null;
    }
    return null;
  }

  private static ResolvableType elementType(ResolvableType type) {
    if (type.isArray()) {
      return type.getComponentType();
    }
    return type.asMap() != ResolvableType.NONE ? type.asMap().getGeneric(1) : type.asCollection().getGeneric();
  }

  /** The binding rules of a page class: allowed patterns, and the depth of each annotated getter (Struts). */
  record Rules(String[] patterns, Map<String, Integer> depths, Set<String> exact) {

    /** Struts counts each "." and "[" below the annotated property as one level. */
    boolean withinDepth(String field) {
      if (exact.contains(field)) {
        return true;
      }
      int end = 0;
      while (end < field.length() && field.charAt(end) != '.' && field.charAt(end) != '[') {
        end++;
      }
      long levels = field.chars().filter(c -> c == '.' || c == '[').count();
      return levels <= depths.getOrDefault(field.substring(0, end), 0);
    }
  }

  /** Property patterns for the setters, getters and fields annotated with {@link StrutsParameter}. */
  static Rules rules(Class<?> type, List<String> extra) {
    Set<String> patterns = new LinkedHashSet<>(extra);
    Map<String, Integer> depths = new HashMap<>();
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
        depths.merge(property, p.depth(), Math::max);
      }
    }
    for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field f : c.getDeclaredFields()) {
        StrutsParameter p = f.getAnnotation(StrutsParameter.class);
        if (p != null && !Modifier.isStatic(f.getModifiers())) {
          patterns.add(f.getName());
          addNested(patterns, f.getName(), p.depth());
          depths.merge(f.getName(), p.depth(), Math::max);
        }
      }
    }
    return new Rules(patterns.toArray(String[]::new), Map.copyOf(depths), Set.copyOf(extra));
  }

  private static void addNested(Set<String> patterns, String property, int depth) {
    if (depth > 0) {
      patterns.add(property + ".*");
      patterns.add(property + "[*");
    }
  }
}
