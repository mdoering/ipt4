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

import org.gbif.ipt.web.support.action.PageBean;
import org.gbif.ipt.web.support.page.PageContext;
import org.gbif.ipt.web.support.action.ActionPage;
import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;
import org.gbif.ipt.i18n.SimpleTextProvider;
import org.gbif.ipt.task.StatusReport;

import jakarta.inject.Inject;
import java.io.Serial;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gbif.ipt.web.support.action.StrutsParameter;

import lombok.Getter;

@PageBean
public class PublishingStatusApiPage extends ActionPage {

  @Serial
  private static final long serialVersionUID = -6781737879827279405L;

  private final ResourceManager resourceManager;
  private final ResourcePublicationManager resourcePublicationManager;

  @Getter
  private Date publicationStartedDate;
  private String r;
  private String status;
  private StatusReport report;
  private Map<String, StatusReport> filteredReports;
  private List<String> resources;
  private long since;

  @Inject
  public PublishingStatusApiPage(
      PageContext ctx,
                                   ResourceManager resourceManager, ResourcePublicationManager resourcePublicationManager) {
    super(ctx);
    this.resourceManager = resourceManager;
    this.resourcePublicationManager = resourcePublicationManager;
  }

  @Override
  public String execute() {
    publicationStartedDate = new Date();
    Map<String, StatusReport> allReports = resourcePublicationManager.getProcessReports();

    if (r != null) {
      report = allReports.get(r);
      filteredReports = null;
    } else {
      filteredReports = new HashMap<>();
      for (Map.Entry<String, StatusReport> entry : allReports.entrySet()) {
        StatusReport sr = entry.getValue();
        if (shouldInclude(sr)) {
          filteredReports.put(entry.getKey(), sr);
        }
      }
      report = null;

      if ("completed".equalsIgnoreCase(status)) {
        filteredReports = filteredReports.entrySet().stream()
            .filter(e -> e.getValue().isCompleted())
            .filter(e -> e.getValue().getTimestamp() > since)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      }
    }

    resources = resourceManager.list().stream()
        .map(Resource::getShortname)
        .collect(Collectors.toList());

    return SUCCESS;
  }

  private boolean shouldInclude(StatusReport sr) {
    if ("running".equalsIgnoreCase(status)) {
      return sr != null && !sr.isCompleted();
    } else if ("completed".equalsIgnoreCase(status)) {
      return sr != null && sr.isCompleted();
    }

    return true;
  }
  public Map<String, StatusReport> getReports() {
    return filteredReports;
  }
  public List<String> getResources() {
    return resources;
  }
  public StatusReport getReport() {
    return report;
  }

  @StrutsParameter
  public void setR(String r) {
    this.r = r;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setSince(long since) {
    this.since = since;
  }

}
