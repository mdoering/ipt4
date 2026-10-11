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
package org.gbif.ipt.web.admin;

import org.gbif.ipt.task.StatusReport;
import org.gbif.ipt.task.TaskMessage;
import org.gbif.ipt.web.support.StrutsJson;
import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.web.support.action.ActionRunner;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IPT 3's BulkPublicationAction, PublishAllResourcesAction, PublishingStatusAction (which ran without any access
 * check), PublishingStatusApiAction and DeleteVersionAction.
 */
@Controller
public class PublishingController extends AdminPageController {

  private static final String BULK = "admin/bulk_publication";

  private final ObjectProvider<BulkPublicationPage> bulkPages;
  private final ObjectProvider<PublishAllResourcesPage> publishAllPages;
  private final ObjectProvider<PublishingStatusPage> statusPages;
  private final ObjectProvider<PublishingStatusApiPage> statusApiPages;
  private final ObjectProvider<DeleteVersionPage> deleteVersionPages;

  public PublishingController(ActionRunner runner, ObjectProvider<BulkPublicationPage> bulkPages,
                              ObjectProvider<PublishAllResourcesPage> publishAllPages,
                              ObjectProvider<PublishingStatusPage> statusPages,
                              ObjectProvider<PublishingStatusApiPage> statusApiPages,
                              ObjectProvider<DeleteVersionPage> deleteVersionPages) {
    super(runner);
    this.bulkPages = bulkPages;
    this.publishAllPages = publishAllPages;
    this.statusPages = statusPages;
    this.statusApiPages = statusApiPages;
    this.deleteVersionPages = deleteVersionPages;
  }

  @GetMapping("/admin/bulk-publication")
  public ModelAndView bulkPublication(HttpServletRequest request) throws Exception {
    BulkPublicationPage page = bulkPages.getObject();
    return admin(request, page, "execute", page::execute, Map.of(ActionPage.SUCCESS, BULK));
  }

  /** Publishes and answers once the publications finished, as IPT 3 did; the page polls bulkReport meanwhile. */
  @PostMapping("/admin/publishAll")
  public ModelAndView publishAll(HttpServletRequest request) throws Exception {
    PublishAllResourcesPage page = publishAllPages.getObject();
    return admin(request, page, "execute", page::execute, Map.of(ActionPage.SUCCESS, BULK));
  }

  @GetMapping("/admin/bulkReport")
  public ModelAndView bulkReport(HttpServletRequest request) throws Exception {
    PublishingStatusPage page = statusPages.getObject();
    return admin(request, page, "execute", page::execute,
        Map.of(ActionPage.SUCCESS, "admin/bulk_report", ActionPage.CANCEL, "admin/bulk_report"));
  }

  @GetMapping("/admin-api/publication-report")
  public ResponseEntity<byte[]> publicationReport(HttpServletRequest request) throws Exception {
    PublishingStatusApiPage page = statusApiPages.getObject();
    runner.run(page, request, "execute", page::execute);
    return StrutsJson.json(page.getReport() == null ? null : report(page.getReport()));
  }

  @GetMapping("/admin-api/publication-reports")
  public ResponseEntity<byte[]> publicationReports(HttpServletRequest request) throws Exception {
    PublishingStatusApiPage page = statusApiPages.getObject();
    runner.run(page, request, "execute", page::execute);
    Map<String, Object> reports = new TreeMap<>();
    if (page.getReports() != null) {
      page.getReports().forEach((shortname, report) -> reports.put(shortname, report(report)));
    }
    return StrutsJson.json(reports);
  }

  /** A version of the resource page; IPT 3 used a GET link and returned to {@code /resource}. */
  @PostMapping("/admin/deleteVersion")
  public ModelAndView deleteVersion(HttpServletRequest request) throws Exception {
    DeleteVersionPage page = deleteVersionPages.getObject();
    ModelAndView mav = admin(request, page, "execute", page::execute, Map.of(ActionPage.SUCCESS, "redirect:/resource"));
    if ("redirect:/resource".equals(mav.getViewName())) {
      mav.setViewName("redirect:/resource?r=" + URLEncoder.encode(page.getShortname(), StandardCharsets.UTF_8));
    }
    return mav;
  }

  /**
   * The properties of IPT 3's StatusReport JSON, without the exception object itself (its message and stack trace are
   * there).
   */
  private static Map<String, Object> report(StatusReport report) {
    Map<String, Object> json = new TreeMap<>();
    json.put("completed", report.isCompleted());
    json.put("exceptionMessage", report.getExceptionMessage());
    json.put("exceptionStacktrace", report.getExceptionStacktrace());
    json.put("messages", report.getMessages() == null ? List.of()
        : report.getMessages().stream().map(PublishingController::message).toList());
    json.put("state", report.getState());
    json.put("step", report.getStep());
    json.put("timestamp", report.getTimestamp());
    return json;
  }

  private static Map<String, Object> message(TaskMessage message) {
    Map<String, Object> json = new LinkedHashMap<>();
    json.put("date", message.getDate());
    Map<String, Object> level = new TreeMap<>();
    level.put("intLevel", message.getLevel().intLevel());
    level.put("name", message.getLevel().name());
    level.put("standardLevel", message.getLevel().getStandardLevel().name());
    json.put("level", level);
    json.put("message", message.getMessage());
    json.put("timestamp", message.getTimestamp());
    return json;
  }
}
