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
import org.gbif.ipt.web.support.action.PostActionPage;
import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.config.Constants;
import org.gbif.ipt.config.DataDir;
import org.gbif.ipt.model.IptColorScheme;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.i18n.SimpleTextProvider;

import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gbif.ipt.web.support.action.StrutsParameter;
import org.gbif.ipt.web.support.action.UploadedFilesAware;
import org.gbif.ipt.web.support.action.UploadedFile;

import lombok.Getter;

@PageBean
public class UIManagementPage extends PostActionPage implements UploadedFilesAware {

  @Serial
  private static final long serialVersionUID = 2001100185337026057L;

  private static final Logger LOG = LogManager.getLogger(UIManagementPage.class);

  private final DataDir dataDir;

  private IptColorScheme colorScheme;
  private List<UploadedFile> uploadedFiles = new ArrayList<>();
  @Getter
  private boolean removeLogo;

  @Inject
  public UIManagementPage(
      PageContext ctx,
      DataDir dataDir) {
    super(ctx);
    this.dataDir = dataDir;
  }

  @Override
  public void prepare() {
    super.prepare();
    colorScheme = getCfg().getColorSchemeConfig();
  }

  @Override
  public String save() {
    try {
      getCfg().saveColorSchemeConfig(colorScheme);

      if (removeLogo) {
        dataDir.removeLogoFile();
      }
    } catch (Exception e) {
      LOG.error("Exception while trying to change IPT UI settings", e);
      addActionError(getText("admin.uiManagement.error.failedToSafe"));
      return INPUT;
    }

    addActionMessage(getText("admin.uiManagement.success"));
    return SUCCESS;
  }

  public String uploadLogo() {
    UploadedFile file = uploadedFiles == null || uploadedFiles.isEmpty() ? null : uploadedFiles.get(0);

    if (file != null && file.getContent() != null) {
      // the image type the browser declared; only IPT's logo types (IPT 3 stored any type under its name)
      String type = StringUtils.substringAfterLast(StringUtils.defaultString(file.getContentType()), "/")
          .toLowerCase(Locale.ROOT);
      if ("jpg".equals(type)) {
        type = "jpeg";
      }
      if (!Arrays.asList(Constants.IMAGE_TYPES).contains(type)) {
        addActionError(getText("manage.metadata.additional.logo.type",
            new String[] {String.join(", ", Constants.IMAGE_TYPES).toUpperCase(Locale.ROOT)}));
        return INPUT;
      }
      // remove any previous logo file
      for (String suffix : Constants.IMAGE_TYPES) {
        FileUtils.deleteQuietly(dataDir.appLogoFile(suffix));
      }
      File logoFile = dataDir.appLogoFile(type);

      try {
        File tempFile = (File) file.getContent();
        Files.copy(
            tempFile.toPath(),
            logoFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        );
      } catch (IOException e) {
        LOG.error("Failed to upload the IPT logo: {}", e.getMessage(), e);
      }
    } else {
      LOG.debug("No uploaded file found");
    }

    return INPUT;
  }


  @StrutsParameter(depth = 1)
  public IptColorScheme getColorScheme() {
    return colorScheme;
  }

  @Override
  public void withUploadedFiles(List<UploadedFile> uploadedFiles) {
    this.uploadedFiles = uploadedFiles;
  }

  @StrutsParameter
  public void setRemoveLogo(boolean removeLogo) {
    this.removeLogo = removeLogo;
  }
}
