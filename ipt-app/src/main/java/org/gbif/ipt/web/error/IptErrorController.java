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
package org.gbif.ipt.web.error;

import org.gbif.ipt.web.support.page.GlobalModel;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Renders IPT 3's error templates for errors the servlet container handles: unknown URLs, exceptions (IPT 3's
 * global exception mapping to error.ftl), and missing rights (401, under /manage the manager variant).
 */
@Controller
public class IptErrorController implements ErrorController {

  private final ObjectProvider<ErrorPage> pages;

  public IptErrorController(ObjectProvider<ErrorPage> pages) {
    this.pages = pages;
  }

  @RequestMapping("/error")
  public ModelAndView error(HttpServletRequest request) {
    HttpStatus status = status(request);
    String view = switch (status) {
      case NOT_FOUND -> "error/404";
      case UNAUTHORIZED -> isManagePath(request) ? "error/401-manager" : "error/401";
      default -> "error/error";
    };
    ModelAndView mav = new ModelAndView(view);
    mav.setStatus(status);
    mav.addObject(GlobalModel.PAGE, pages.getObject());
    Throwable exception = exception(request);
    if (exception != null) {
      mav.addObject("exception", exception);
    }
    return mav;
  }

  private static HttpStatus status(HttpServletRequest request) {
    Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    HttpStatus status = code instanceof Integer i ? HttpStatus.resolve(i) : null;
    return status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
  }

  private static boolean isManagePath(HttpServletRequest request) {
    Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    String path = uri instanceof String s ? s.substring(Math.min(request.getContextPath().length(), s.length())) : "";
    return path.startsWith("/manage");
  }

  /** The exception the page threw, not Spring's "Request processing failed" wrapper. */
  private static Throwable exception(HttpServletRequest request) {
    Object attr = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
    Throwable t = attr instanceof Throwable th ? th : null;
    while (t instanceof ServletException && t.getCause() != null) {
      t = t.getCause();
    }
    return t;
  }
}
