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
package org.gbif.ipt.i18n;

import org.gbif.ipt.feedback.Feedback;

/** {@link I18n} over a {@link Feedback}; replaces IPT 3's {@code StrutsI18n}. */
public class FeedbackI18n implements I18n {

  private final Feedback feedback;

  public FeedbackI18n(Feedback feedback) {
    this.feedback = feedback;
  }

  @Override
  public String getText(String key) {
    return feedback.getText(key);
  }

  @Override
  public String getText(String key, String... args) {
    return feedback.getText(key, args);
  }
}
