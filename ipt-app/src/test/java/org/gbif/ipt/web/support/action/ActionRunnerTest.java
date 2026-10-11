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
package org.gbif.ipt.web.support.action;

import org.gbif.ipt.model.Extension;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.web.support.feedback.RequestFeedback;
import org.gbif.ipt.web.support.page.PageContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActionRunnerTest {

  @TempDir Path uploads;

  /** Holds a reference to an installed extension, like IPT 3's ExtensionMapping. */
  public static class Mapping {
    private String name;
    private java.io.File file;
    private java.nio.file.Path path;
    private org.gbif.ipt.model.Source source = new org.gbif.ipt.model.TextFileSource();

    public org.gbif.ipt.model.Source getSource() {
      return source;
    }

    public java.io.File getFile() {
      return file;
    }

    public void setFile(java.io.File file) {
      this.file = file;
    }

    public java.nio.file.Path getPath() {
      return path;
    }

    public void setPath(java.nio.file.Path path) {
      this.path = path;
    }

    private Extension extension = new Extension();

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Extension getExtension() {
      return extension;
    }

    public void setExtension(Extension extension) {
      this.extension = extension;
    }
  }

  public static class ProbePage extends ActionPage implements UploadedFilesAware, ValidationErrorAware {
    private final Mapping mapping = new Mapping();
    private final List<Mapping> mappings = new ArrayList<>(List.of(new Mapping()));
    private final User user = new User();
    private final Resource resource = new Resource();
    List<UploadedFile> files = List.of();
    String methodInPrepare;
    boolean failValidation;

    ProbePage() {
      super(new PageContext(null, Locale.ENGLISH, null, null, "http://localhost", "http://localhost", null,
          new RequestFeedback(Locale.ENGLISH, null, null)));
    }

    @Override
    public void prepare() {
      super.prepare();
      methodInPrepare = getInvokedMethod();
    }

    @Override
    public void validate() {
      if (failValidation) {
        addFieldError("name", "wrong");
      }
    }

    @StrutsParameter(depth = 3)
    public Mapping getMapping() {
      return mapping;
    }

    @StrutsParameter(depth = 3)
    public List<Mapping> getMappings() {
      return mappings;
    }

    @StrutsParameter(depth = 1)
    public User getUser() {
      return user;
    }

    /** Not annotated: only the extra fields below are bindable, like the manage pages' resource. */
    public Resource getResource() {
      return resource;
    }

    @Override
    protected List<String> extraAllowedFields() {
      return List.of("resource.coreType", "resource.creator.role");
    }

    @Override
    public void withUploadedFiles(List<UploadedFile> uploadedFiles) {
      this.files = uploadedFiles;
    }

    @Override
    public String actionErrorOccurred(String currentResultName) {
      return "custom-" + currentResultName;
    }
  }

  private ProbePage page(MockHttpServletRequest request) {
    ProbePage page = new ProbePage();
    page.setServletRequest(request);
    page.setServletResponse(new MockHttpServletResponse());
    return page;
  }

  private ActionRunner runner() {
    return new ActionRunner(new UploadStore(uploads.toFile()));
  }

  @Test
  void sharedObjectsAreNotBoundThroughOtherObjects() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
    request.addParameter("mapping.name", "mine");
    request.addParameter("mapping.extension.title", "EVIL");
    request.addParameter("mappings[0].extension.title", "EVIL");
    request.addParameter("resource.creator.role", "Admin");
    request.addParameter("mapping.source.name", "renamed");
    ProbePage page = page(request);
    page.getResource().setCreator(new User());
    runner().run(page, request, "save", () -> ActionPage.SUCCESS);
    assertThat(page.getMapping().getName()).isEqualTo("mine");
    assertThat(page.getMapping().getExtension().getTitle()).isNull();
    assertThat(page.getMappings().get(0).getExtension().getTitle()).isNull();
    assertThat(page.getResource().getCreator().getRole()).isNotEqualTo(User.Role.Admin);
    // a mapping's source is one of the resource's sources
    assertThat(page.getMapping().getSource().getName()).isNull();
  }

  /** Spring converts strings to files; a source pointed at /etc/passwd could then be downloaded as raw source. */
  @Test
  void filesAndPathsAreNeverBound() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
    request.addParameter("mapping.file", "/etc/passwd");
    request.addParameter("mapping.path", "/etc/passwd");
    ProbePage page = page(request);
    runner().run(page, request, "save", () -> ActionPage.SUCCESS);
    assertThat(page.getMapping().getFile()).isNull();
    assertThat(page.getMapping().getPath()).isNull();
  }

  /** Struts' depth: each "." and "[" below the annotated property is one level. */
  @Test
  void nestingIsLimitedToTheDeclaredDepth() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
    request.addParameter("user.firstname", "Ann");
    request.addParameter("user.address", "x");
    ShallowPage page = new ShallowPage();
    page.setServletRequest(request);
    page.setServletResponse(new MockHttpServletResponse());
    runner().run(page, request, "save", () -> ActionPage.SUCCESS);
    assertThat(page.getUser().getFirstname()).isEqualTo("Ann");
    request = new MockHttpServletRequest("POST", "/x");
    request.addParameter("mappings[0].name", "deep");
    page = new ShallowPage();
    page.setServletRequest(request);
    page.setServletResponse(new MockHttpServletResponse());
    runner().run(page, request, "save", () -> ActionPage.SUCCESS);
    assertThat(page.getMappings().get(0).getName()).isNull();
  }

  public static class ShallowPage extends ActionPage {
    private final User user = new User();
    private final List<Mapping> mappings = new ArrayList<>(List.of(new Mapping()));

    ShallowPage() {
      super(new PageContext(null, Locale.ENGLISH, null, null, "http://localhost", "http://localhost", null,
          new RequestFeedback(Locale.ENGLISH, null, null)));
    }

    @StrutsParameter(depth = 1)
    public User getUser() {
      return user;
    }

    /** depth 1 allows mappings[0], not mappings[0].name */
    @StrutsParameter(depth = 1)
    public List<Mapping> getMappings() {
      return mappings;
    }
  }

  @Test
  void pageOwnedObjectsAndExtraFieldsAreBound() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
    request.addParameter("user.firstname", "Ann");
    request.addParameter("resource.coreType", "occurrence");
    request.addParameter("resource.shortname", "other");
    ProbePage page = page(request);
    runner().run(page, request, "save", () -> ActionPage.SUCCESS);
    assertThat(page.getUser().getFirstname()).isEqualTo("Ann");
    assertThat(page.getResource().getCoreType()).isEqualTo("occurrence");
    assertThat(page.getResource().getShortname()).isNull();
  }

  @Test
  void pagesKnowTheirMethodWhilePreparing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
    ProbePage page = page(request);
    runner().run(page, request, "delete", () -> ActionPage.SUCCESS);
    assertThat(page.methodInPrepare).isEqualTo("delete");
  }

  @Test
  void validationErrorAwarePagesChooseTheResult() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
    ProbePage page = page(request);
    page.failValidation = true;
    assertThat(runner().run(page, request, "save", () -> ActionPage.SUCCESS)).isEqualTo("custom-input");
  }

  @Test
  void uploadsAreStoredUnderTheirOwnNamesAndRemovedAfterwards() throws Exception {
    MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
    request.setMethod("POST");
    request.addFile(new MockMultipartFile("file", "../../config/users.json", "text/plain", "a,b".getBytes()));
    ProbePage page = page(request);
    List<File> seen = new ArrayList<>();
    runner().run(page, request, "add", () -> {
      UploadedFile upload = page.files.get(0);
      File content = (File) upload.getContent();
      seen.add(content);
      assertThat(content.toPath().normalize().startsWith(uploads)).isTrue();
      assertThat(content.getName()).doesNotContain("users");
      assertThat(Files.readString(content.toPath())).isEqualTo("a,b");
      assertThat(upload.getOriginalName()).isEqualTo("../../config/users.json");
      assertThat(upload.getContentType()).isEqualTo("text/plain");
      assertThat(upload.getInputName()).isEqualTo("file");
      return ActionPage.SUCCESS;
    });
    assertThat(seen).hasSize(1);
    assertThat(seen.get(0)).doesNotExist();
  }

  /** Each index a request names makes the binder create every element up to it: requests are held to a budget. */
  @Test
  void listGrowthIsBounded() throws Exception {
    MockHttpServletRequest legit = new MockHttpServletRequest("POST", "/x");
    for (int i = 0; i < 600; i++) {
      legit.addParameter("mappings[" + i + "].name", "m" + i);
    }
    ProbePage page = page(legit);
    runner().run(page, legit, "save", () -> ActionPage.SUCCESS);
    assertThat(page.getMappings()).hasSize(600);

    MockHttpServletRequest huge = new MockHttpServletRequest("POST", "/x");
    huge.addParameter("mappings[50000].name", "far");
    ProbePage refused = page(huge);
    assertThatThrownBy(() -> runner().run(refused, huge, "save", () -> ActionPage.SUCCESS))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(refused.getMappings()).hasSize(1);
  }
}
