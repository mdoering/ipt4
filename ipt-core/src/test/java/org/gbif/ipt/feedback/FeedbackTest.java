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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackTest {

  @Test
  void collectsMessagesWarningsErrorsAndFieldErrors() {
    CollectingFeedback feedback = new CollectingFeedback(Locale.ENGLISH);
    feedback.addActionMessage("saved");
    feedback.addActionWarning("careful");
    feedback.addActionWarning("careful");
    feedback.addActionWarning("broken", new IllegalStateException("cause"));
    feedback.addActionError("failed");
    feedback.addFieldError("eml.title", "required");
    feedback.addFieldError("eml.title", "too short");

    assertThat(feedback.getActionMessages()).containsExactly("saved");
    assertThat(feedback.getActionWarnings()).containsExactly("careful", "broken", "cause");
    assertThat(feedback.getActionErrors()).containsExactly("failed");
    assertThat(feedback.getFieldErrors()).containsEntry("eml.title", List.of("required", "too short"));
    assertThat(feedback.hasActionErrors()).isTrue();
    assertThat(feedback.hasFieldErrors()).isTrue();
  }

  @Test
  void resolvesTextsInItsLocale() {
    assertThat(new CollectingFeedback(Locale.ENGLISH).getText("menu.about")).isEqualTo("About");
    assertThat(new CollectingFeedback(Locale.FRENCH).getText("menu.about")).isEqualTo("A propos");
  }

  @Test
  void formatsArgumentsOnceAndKeepsApostrophes() {
    CollectingFeedback fr = new CollectingFeedback(Locale.FRENCH);
    assertThat(fr.getText("validation.wrong.datatype", new String[] {"dwc:year"}))
        .isEqualTo("dwc:year est mappé à des données d'un type incompatible.");
  }

  @Test
  void unknownKeyFallsBackToDefaultValueThenKey() {
    CollectingFeedback feedback = new CollectingFeedback(Locale.ENGLISH);
    assertThat(feedback.getText("no.such.key", "fallback")).isEqualTo("fallback");
    assertThat(feedback.getText("no.such.key")).isEqualTo("no.such.key");
    assertThat(feedback.getTextWithDynamicArgs("no.such.key", "a")).isEqualTo("no.such.key");
  }

  @Test
  void nullLocaleMeansEnglish() {
    assertThat(new CollectingFeedback(null).getText("menu.about")).isEqualTo("About");
  }

  @Test
  void carriesTheCurrentUser() {
    User user = new User();
    user.setEmail("a@b.org");
    assertThat(new CollectingFeedback(Locale.ENGLISH, user).getCurrentUser()).isSameAs(user);
    assertThat(new CollectingFeedback(Locale.ENGLISH).getCurrentUser()).isNull();
  }

  @Test
  void loggingFeedbackStillCollects() {
    LoggingFeedback feedback = new LoggingFeedback(Locale.ENGLISH, "job");
    feedback.addActionError("failed");
    assertThat(feedback.hasActionErrors()).isTrue();
  }
}
