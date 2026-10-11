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
/*
 * IPT4: adds the Spring Security CSRF token (meta tags in inc/header.ftl) to same-origin, state-changing jQuery AJAX
 * requests. Cross-origin requests, such as the api.gbif.org lookups on the admin pages, never see the token.
 * iptPostLink(href) sends a link as a POST form with the token: IPT 3's state-changing GET links (delete source,
 * delete mapping) are POSTs in IPT4.
 */
(function (root) {
  'use strict';
  var SAFE_METHODS = /^(GET|HEAD|OPTIONS|TRACE)$/i;

  function csrfPrefilter(header, token) {
    return function (options, originalOptions, xhr) {
      if (options.crossDomain || SAFE_METHODS.test(options.type || 'GET')) {
        return;
      }
      xhr.setRequestHeader(header, token);
    };
  }

  /** A hidden POST form for a link: the link's path as action, its query parameters and the token as fields. */
  function postLinkForm(doc, href, parameterName, token) {
    var url = new URL(href, doc.baseURI);
    var form = doc.createElement('form');
    form.method = 'post';
    form.action = url.origin + url.pathname;
    var fields = [];
    url.searchParams.forEach(function (value, name) {
      fields.push([name, value]);
    });
    if (parameterName && token) {
      fields.push([parameterName, token]);
    }
    fields.forEach(function (field) {
      var input = doc.createElement('input');
      input.type = 'hidden';
      input.name = field[0];
      input.value = field[1];
      form.appendChild(input);
    });
    return form;
  }

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = { csrfPrefilter: csrfPrefilter, postLinkForm: postLinkForm };
  } else if (root.jQuery) {
    root.jQuery(function ($) {
      var token = $('meta[name="_csrf"]').attr('content');
      var header = $('meta[name="_csrf_header"]').attr('content');
      if (token && header) {
        $.ajaxPrefilter(csrfPrefilter(header, token));
      }
      root.iptPostLink = function (href) {
        var form = postLinkForm(document, href, $('meta[name="_csrf_parameter"]').attr('content'), token);
        form.style.display = 'none';
        document.body.appendChild(form);
        form.submit();
      };
      // links that change state without a confirmation dialog, and the cancel link of the publication status
      // (inside a translated message)
      $(document).on('click', 'a[data-post], a[href*="/manage/cancel.do?"]', function (event) {
        event.preventDefault();
        root.iptPostLink(this.getAttribute('href'));
      });
    });
  }
})(this);
