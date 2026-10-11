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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
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

  private final Map<Class<?>, String[]> allowedFields = new ConcurrentHashMap<>();
  private final UploadStore uploads;

  public ActionRunner(UploadStore uploads) {
    this.uploads = uploads;
  }

  public String run(ActionPage page, HttpServletRequest request, String methodName, ActionMethod method)
      throws Exception {
    page.setInvokedMethod(methodName);
    page.prepare();
    // IPT 3's SetupAndCancelInterceptor: any cancel parameter cancels
    if (request.getParameterMap().containsKey(ActionPage.CANCEL)) {
      page.setCancel("true");
    }
    List<UploadedFile> files = upload(page, request);
    try {
      bind(page, request);
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

  void bind(ActionPage page, HttpServletRequest request) {
    Class<?> pageType = page.getClass();
    ServletRequestDataBinder binder = new ServletRequestDataBinder(page) {
      @Override
      protected boolean isAllowed(String field) {
        return super.isAllowed(field) && !forbidden(pageType, field);
      }
    };
    binder.setAllowedFields(
        allowedFields.computeIfAbsent(pageType, type -> allowed(type, page.extraAllowedFields())));
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
   * Whether the path leads into (or replaces) a shared object below the page's own properties, or sets a file, judged
   * by declared types.
   */
  static boolean forbidden(Class<?> pageType, String field) {
    String[] segments = field.split("\\.");
    ResolvableType type = ResolvableType.forClass(pageType);
    for (int i = 0; i < segments.length; i++) {
      String name = INDEX.matcher(segments[i]).replaceAll("");
      Class<?> owner = type.resolve();
      PropertyDescriptor pd = owner == null ? null : BeanUtils.getPropertyDescriptor(owner, name);
      if (pd == null || pd.getReadMethod() == null) {
        return false;
      }
      type = ResolvableType.forMethodReturnType(pd.getReadMethod(), owner);
      long indices = segments[i].chars().filter(c -> c == '[').count();
      for (long n = 0; n < indices; n++) {
        type = elementType(type);
      }
      Class<?> resolved = type.resolve();
      if (resolved != null && NEVER_BOUND.stream().anyMatch(never -> never.isAssignableFrom(resolved))) {
        return true;
      }
      if (i > 0 && resolved != null && SHARED.stream().anyMatch(shared -> shared.isAssignableFrom(resolved))) {
        return true;
      }
    }
    return false;
  }

  private static ResolvableType elementType(ResolvableType type) {
    if (type.isArray()) {
      return type.getComponentType();
    }
    return type.asMap() != ResolvableType.NONE ? type.asMap().getGeneric(1) : type.asCollection().getGeneric();
  }

  /** Property patterns for the setters, getters and fields annotated with {@link StrutsParameter}. */
  static String[] allowed(Class<?> type, List<String> extra) {
    Set<String> patterns = new LinkedHashSet<>(extra);
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
