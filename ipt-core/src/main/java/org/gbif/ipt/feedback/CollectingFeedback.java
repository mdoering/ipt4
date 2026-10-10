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
package org.gbif.ipt.feedback;

import org.gbif.ipt.i18n.SimpleTextProvider;
import org.gbif.ipt.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

/** A {@link Feedback} that keeps everything it is given, for callers that render or inspect it afterwards. */
public class CollectingFeedback implements Feedback {

  private static final SimpleTextProvider TEXTS = new SimpleTextProvider();

  private final Locale locale;
  private final User currentUser;
  private final List<String> messages = new ArrayList<>();
  private final List<String> warnings = new ArrayList<>();
  private final List<String> errors = new ArrayList<>();
  private final Map<String, List<String>> fieldErrors = new LinkedHashMap<>();

  public CollectingFeedback(@Nullable Locale locale) {
    this(locale, null);
  }

  public CollectingFeedback(@Nullable Locale locale, @Nullable User currentUser) {
    this.locale = locale;
    this.currentUser = currentUser;
  }

  @Override
  @Nullable
  public Locale getLocale() {
    return locale;
  }

  @Override
  public String getText(String key) {
    return TEXTS.getText(locale, key, null, new String[0]);
  }

  @Override
  public String getText(String key, String[] args) {
    return TEXTS.getText(locale, key, null, args);
  }

  @Override
  public String getText(String key, List<?> args) {
    return TEXTS.getText(locale, key, null, args);
  }

  @Override
  public String getText(String key, String defaultValue) {
    return TEXTS.getText(locale, key, defaultValue, new String[0]);
  }

  @Override
  public String getText(String key, String defaultValue, String[] args) {
    return TEXTS.getText(locale, key, defaultValue, args);
  }

  @Override
  public String getTextWithDynamicArgs(String key, String... args) {
    return TEXTS.getText(locale, key, null, args);
  }

  @Override
  public void addActionMessage(String message) {
    messages.add(message);
  }

  @Override
  public void addActionWarning(String warning) {
    if (!warnings.contains(warning)) {
      warnings.add(warning);
    }
  }

  @Override
  public void addActionWarning(String warning, Exception e) {
    warnings.add(warning);
    if (e != null && e.getMessage() != null) {
      warnings.add(e.getMessage());
    }
  }

  @Override
  public void addActionError(String error) {
    errors.add(error);
  }

  @Override
  public void addFieldError(String field, String error) {
    fieldErrors.computeIfAbsent(field, f -> new ArrayList<>()).add(error);
  }

  @Override
  public boolean hasActionErrors() {
    return !errors.isEmpty();
  }

  @Override
  public boolean hasFieldErrors() {
    return !fieldErrors.isEmpty();
  }

  @Override
  @Nullable
  public User getCurrentUser() {
    return currentUser;
  }

  /** Forgets action and field errors (e.g. before a form is shown again with fresh values). */
  public void clearErrors() {
    errors.clear();
    fieldErrors.clear();
  }

  public List<String> getActionMessages() {
    return Collections.unmodifiableList(messages);
  }

  public List<String> getActionWarnings() {
    return Collections.unmodifiableList(warnings);
  }

  public List<String> getActionErrors() {
    return Collections.unmodifiableList(errors);
  }

  public Map<String, List<String>> getFieldErrors() {
    return Collections.unmodifiableMap(fieldErrors);
  }
}
