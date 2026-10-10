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

import java.util.Locale;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A {@link CollectingFeedback} that also logs, for background jobs and the command line. */
public class LoggingFeedback extends CollectingFeedback {

  private static final Logger LOG = LoggerFactory.getLogger(LoggingFeedback.class);

  private final String context;

  public LoggingFeedback(@Nullable Locale locale, String context) {
    this(locale, null, context);
  }

  public LoggingFeedback(@Nullable Locale locale, @Nullable User currentUser, String context) {
    super(locale, currentUser);
    this.context = context;
  }

  @Override
  public void addActionMessage(String message) {
    LOG.info("[{}] {}", context, message);
    super.addActionMessage(message);
  }

  @Override
  public void addActionWarning(String warning) {
    LOG.warn("[{}] {}", context, warning);
    super.addActionWarning(warning);
  }

  @Override
  public void addActionWarning(String warning, Exception e) {
    LOG.warn("[{}] {}", context, warning, e);
    super.addActionWarning(warning, e);
  }

  @Override
  public void addActionError(String error) {
    LOG.error("[{}] {}", context, error);
    super.addActionError(error);
  }

  @Override
  public void addFieldError(String field, String error) {
    LOG.warn("[{}] {}: {}", context, field, error);
    super.addFieldError(field, error);
  }
}
