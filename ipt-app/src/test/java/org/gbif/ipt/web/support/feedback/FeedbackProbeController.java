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

import org.gbif.ipt.feedback.Feedback;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/** Test-only controller: adds feedback, then redirects or renders. */
@Controller
@Profile("feedback-probe")
public class FeedbackProbeController {

  private final Feedback feedback;

  public FeedbackProbeController(Feedback feedback) {
    this.feedback = feedback;
  }

  @PostMapping("/_feedback/redirect")
  public String redirect() {
    feedback.addActionMessage("Saved <b>it</b>");
    feedback.addActionWarning("Careful");
    return "redirect:/_feedback/show";
  }

  /** Pages redirect to IPT 3's .do URLs, which DoSuffixFilter strips on the way back in. */
  @PostMapping("/_feedback/redirect-do")
  public String redirectDo() {
    feedback.addActionMessage("Saved <b>it</b>");
    return "redirect:/_feedback/show.do";
  }

  @GetMapping("/_feedback/show")
  public String show() {
    return "probe/feedback";
  }

  @GetMapping("/_feedback/field")
  public String field() {
    feedback.addFieldError("email", "Email is required");
    feedback.addActionError("Failed");
    return "probe/feedback";
  }
}
