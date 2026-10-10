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
package org.gbif.ipt.web.portal;

import org.gbif.ipt.web.support.page.GlobalModel;
import org.gbif.ipt.web.support.page.PageContext;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class AboutController {

  @GetMapping("/about")
  public String about(@ModelAttribute(name = GlobalModel.PAGE_CONTEXT, binding = false) PageContext ctx, Model model) {
    model.addAttribute(GlobalModel.PAGE, new AboutPage(ctx));
    return "about";
  }
}
