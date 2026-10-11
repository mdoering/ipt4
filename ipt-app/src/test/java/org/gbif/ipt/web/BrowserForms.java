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
package org.gbif.ipt.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** What a browser posts for a page's form, unchanged or with some fields changed. */
public final class BrowserForms {

  private BrowserForms() {
  }

  /**
   * @param formAction  part of the form's action that identifies it
   * @param model       the object behind the form (prefix + "." paths), or null; empty hidden fields that the
   *                    editors' JavaScript fills on submit (DocBook) get its stored value
   * @param prefix      the model's parameter prefix, e.g. "eml"
   */
  public static MockHttpServletRequestBuilder submit(Document page, String formAction, Object model, String prefix,
                                              Map<String, String> changed) {
    BeanWrapper stored = model == null ? null : new BeanWrapperImpl(model);
    Element form = page.selectFirst("form[action*=" + formAction + "]");
    assertThat(form).as(formAction).isNotNull();
    // the form's action, query included, as a browser posts it
    URI action = URI.create(form.absUrl("action"));
    MockHttpServletRequestBuilder post = post(URI.create(action.getRawPath()
        + (action.getRawQuery() == null ? "" : "?" + action.getRawQuery())));
    Set<String> sent = new HashSet<>();
    for (Element field : form.select("input[name], select[name], textarea[name]")) {
      String name = field.attr("name");
      if (field.hasAttr("disabled") || name.equals("_csrf")) {
        continue;
      }
      if (changed.containsKey(name)) {
        if (sent.add(name)) {
          post.param(name, changed.get(name));
        }
        continue;
      }
      switch (field.tagName()) {
        case "select" -> {
          List<Element> selected = new ArrayList<>(field.select("option[selected]"));
          if (selected.isEmpty() && field.selectFirst("option") != null && !field.hasAttr("multiple")) {
            selected.add(field.selectFirst("option"));
          }
          selected.forEach(o -> post.param(name, o.attr("value")));
        }
        case "textarea" -> post.param(name, field.wholeText());
        default -> {
          String type = field.attr("type").toLowerCase();
          if ((type.equals("checkbox") || type.equals("radio")) && !field.hasAttr("checked")) {
            continue;
          }
          if (type.equals("submit") || type.equals("button") || type.equals("file")) {
            continue;
          }
          String value = field.attr("value");
          String property = name.startsWith(prefix + ".") ? name.substring(prefix.length() + 1) : null;
          if (type.equals("hidden") && value.isEmpty() && stored != null && property != null && !property.contains("[")
              && stored.isReadableProperty(property) && stored.getPropertyValue(property) instanceof String s) {
            value = s;
          }
          post.param(name, value);
        }
      }
    }
    // the clicked button, or fields outside the form element (form="id"), or ones a crafted request adds
    changed.forEach((name, value) -> {
      if (!sent.contains(name)) {
        post.param(name, value);
      }
    });
    return changed.containsKey("save") ? post : post.param("save", "Save");
  }
}
