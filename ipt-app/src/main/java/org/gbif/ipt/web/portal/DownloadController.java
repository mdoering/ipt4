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

import org.gbif.ipt.web.support.CurrentUser;
import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.web.support.action.ActionResults;
import org.gbif.ipt.web.support.action.ActionRunner;
import org.gbif.ipt.web.support.action.PageController;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The data dir files of IPT 3's portal package ({@code ResourceFileAction}, {@code AppFileAction}, {@code DCATAction}):
 * Struts' stream result with the same content types, {@code Content-Disposition} names and conditional GET.
 */
@Controller
public class DownloadController extends PageController {

  private static final Map<String, String> PORTAL = ActionResults.PORTAL;

  private final ObjectProvider<ResourceFilePage> resourceFilePages;
  private final ObjectProvider<AppFilePage> appFilePages;
  private final ObjectProvider<DCATPage> dcatPages;
  private final PortalGuard guard;
  private final CurrentUser currentUser;

  public DownloadController(ActionRunner runner, ObjectProvider<ResourceFilePage> resourceFilePages,
                            ObjectProvider<AppFilePage> appFilePages, ObjectProvider<DCATPage> dcatPages,
                            PortalGuard guard, CurrentUser currentUser) {
    super(runner);
    this.resourceFilePages = resourceFilePages;
    this.appFilePages = appFilePages;
    this.dcatPages = dcatPages;
    this.guard = guard;
    this.currentUser = currentUser;
  }

  @GetMapping("/eml")
  public ModelAndView eml(HttpServletRequest request, HttpServletResponse response) throws Exception {
    ResourceFilePage page = resourceFilePages.getObject();
    return resourceFile(request, response, page, "eml", page::eml, true);
  }

  @GetMapping("/metadata")
  public ModelAndView metadata(HttpServletRequest request, HttpServletResponse response) throws Exception {
    ResourceFilePage page = resourceFilePages.getObject();
    return resourceFile(request, response, page, "metadata", page::metadata, true);
  }

  @GetMapping("/logo")
  public ModelAndView logo(HttpServletRequest request, HttpServletResponse response) throws Exception {
    ResourceFilePage page = resourceFilePages.getObject();
    return resourceFile(request, response, page, "logo", page::logo, true);
  }

  @GetMapping("/publicationlog")
  public ModelAndView publicationLog(HttpServletRequest request, HttpServletResponse response) throws Exception {
    ResourceFilePage page = resourceFilePages.getObject();
    return resourceFile(request, response, page, "publicationLog", page::publicationLog, true);
  }

  @GetMapping("/sourcelog")
  public ModelAndView sourceLog(HttpServletRequest request, HttpServletResponse response) throws Exception {
    ResourceFilePage page = resourceFilePages.getObject();
    return resourceFile(request, response, page, "sourceLog", page::sourceLog, true);
  }

  /** IPT 3 streamed RTF and archives without {@code contentCharSet}. */
  @GetMapping("/rtf")
  public ModelAndView rtf(HttpServletRequest request, HttpServletResponse response) throws Exception {
    ResourceFilePage page = resourceFilePages.getObject();
    return resourceFile(request, response, page, "rtf", page::rtf, false);
  }

  @GetMapping("/archive")
  public ModelAndView archive(HttpServletRequest request, HttpServletResponse response) throws Exception {
    ResourceFilePage page = resourceFilePages.getObject();
    return resourceFile(request, response, page, "archive", page::archive, false);
  }

  @GetMapping("/appLogo")
  public ModelAndView appLogo(HttpServletRequest request, HttpServletResponse response) throws Exception {
    AppFilePage page = appFilePages.getObject();
    String blocked = guard.check(request, currentUser.get());
    if (blocked != null) {
      return render(page, blocked, PORTAL);
    }
    String result = runner.run(page, request, "logo", page::logo);
    return stream(request, response, page, result, page.getData(), page.getMimeType(), page.getFilename(),
        page.getInputStream(), true);
  }

  /** IPT 3: {@code noCache}, {@code text/turtle} in UTF-8. */
  @GetMapping("/dcat")
  public void dcat(HttpServletResponse response) throws Exception {
    DCATPage page = dcatPages.getObject();
    page.execute();
    response.setContentType("text/turtle;charset=UTF-8");
    response.setHeader("Pragma", "No-cache");
    response.setHeader("Cache-Control", "no-cache");
    response.setDateHeader("Expires", 0);
    try (InputStream in = page.getDcatInfo(); OutputStream out = response.getOutputStream()) {
      in.transferTo(out);
    }
  }

  private ModelAndView resourceFile(HttpServletRequest request, HttpServletResponse response, ResourceFilePage page,
                                    String methodName, ActionRunner.ActionMethod method, boolean charset)
      throws Exception {
    String blocked = guard.check(request, currentUser.get());
    if (blocked != null) {
      return render(page, blocked, PORTAL);
    }
    String result = runner.run(page, request, methodName, method);
    return stream(request, response, page, result, page.getData(), page.getMimeType(), page.getFilename(),
        page.getInputStream(), charset);
  }

  /** Struts' {@code StreamResult}; anything but success renders the result's page. */
  private ModelAndView stream(HttpServletRequest request, HttpServletResponse response, ActionPage page,
                              String result, File data, String mimeType, String filename, InputStream in,
                              boolean charset) throws IOException {
    try (InputStream closing = in) {
      if (!ActionPage.SUCCESS.equals(result)) {
        return render(page, result, PORTAL);
      }
      response.setContentType(charset ? mimeType + ";charset=" + StandardCharsets.UTF_8.name() : mimeType);
      response.setHeader("Content-Disposition", "filename=\"" + filename + "\"");
      response.setContentLengthLong(data.length());
      if (!"HEAD".equals(request.getMethod())) {
        closing.transferTo(response.getOutputStream());
      }
      return null;
    }
  }
}
