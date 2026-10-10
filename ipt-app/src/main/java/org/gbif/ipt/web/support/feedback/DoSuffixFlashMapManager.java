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

import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Pages redirect to IPT 3's {@code .do} URLs, but DoSuffixFilter hands the follow-up request on without the suffix;
 * flash attributes match either form.
 */
public class DoSuffixFlashMapManager extends SessionFlashMapManager {

  private static final String DO = ".do";

  @Override
  protected boolean isFlashMapForRequest(FlashMap flashMap, HttpServletRequest request) {
    String target = flashMap.getTargetRequestPath();
    if (target != null && target.endsWith(DO)) {
      flashMap.setTargetRequestPath(target.substring(0, target.length() - DO.length()));
    }
    return super.isFlashMapForRequest(flashMap, request);
  }
}
