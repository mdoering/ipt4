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
package org.gbif.ipt.web.support.feedback;

import org.gbif.ipt.feedback.CollectingFeedback;
import org.gbif.ipt.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * The {@link org.gbif.ipt.feedback.Feedback} of one web request (IPT 3: the action's messages). Messages, warnings and
 * errors added before a redirect are shown after it, once (see {@link FeedbackFlashInterceptor}).
 */
public class RequestFeedback extends CollectingFeedback {

  public static final String MESSAGES = "actionMessages";
  public static final String WARNINGS = "warnings";
  public static final String ERRORS = "actionErrors";

  public RequestFeedback(Locale locale, @Nullable User currentUser, @Nullable Map<String, ?> flash) {
    super(locale, currentUser);
    if (flash != null) {
      strings(flash.get(MESSAGES)).forEach(this::addActionMessage);
      strings(flash.get(WARNINGS)).forEach(this::addActionWarning);
      strings(flash.get(ERRORS)).forEach(this::addActionError);
    }
  }

  private static List<String> strings(Object value) {
    if (value instanceof Collection<?> c) {
      return c.stream().map(String::valueOf).toList();
    }
    return List.of();
  }
}
