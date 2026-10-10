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

import org.gbif.ipt.model.User;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

/**
 * Where core code reports messages, warnings and validation errors, and looks up localised texts.
 * <p>
 * Replaces the Struts {@code Feedback} that IPT 3 passed into services and validators. The method names are those
 * of {@code Feedback}, so ported code only changes its parameter type.
 */
public interface Feedback {

  /** The locale texts are resolved in; null means English. */
  @Nullable
  Locale getLocale();

  String getText(String key);

  String getText(String key, String[] args);

  String getText(String key, List<?> args);

  String getText(String key, String defaultValue);

  String getText(String key, String defaultValue, String[] args);

  String getTextWithDynamicArgs(String key, String... args);

  void addActionMessage(String message);

  void addActionWarning(String warning);

  /** Adds the warning and, if it has one, the exception's message. */
  void addActionWarning(String warning, Exception e);

  void addActionError(String error);

  void addFieldError(String field, String error);

  boolean hasActionErrors();

  boolean hasFieldErrors();

  /** The user on whose behalf the work runs, if any. */
  @Nullable
  User getCurrentUser();
}
