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

import org.gbif.ipt.config.DataDir;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Keeps uploaded files in the data dir's {@code tmp/uploads} under names of its own; the name the browser sent is
 * only passed on as {@link UploadedFile#getOriginalName()}.
 */
@Component
public class UploadStore {

  private final File dir;

  @Autowired
  public UploadStore(DataDir dataDir) {
    this(dataDir.tmpFile("uploads"));
  }

  public UploadStore(File dir) {
    this.dir = dir;
  }

  public UploadedFile store(MultipartFile upload) throws IOException {
    if (!dir.isDirectory() && !dir.mkdirs()) {
      throw new IOException("Cannot create upload directory " + dir);
    }
    File file = new File(dir, "upload-" + UUID.randomUUID());
    upload.transferTo(file);
    return new StoredFile(file, upload.getOriginalFilename(), upload.getContentType(), upload.getName());
  }

  private record StoredFile(File file, String originalName, String contentType, String inputName)
      implements UploadedFile {

    @Override
    public Long length() {
      return file.length();
    }

    @Override
    public String getName() {
      return file.getName();
    }

    @Override
    public String getOriginalName() {
      return originalName;
    }

    @Override
    public boolean isFile() {
      return file.isFile();
    }

    @Override
    public boolean delete() {
      return file.delete();
    }

    @Override
    public String getAbsolutePath() {
      return file.getAbsolutePath();
    }

    @Override
    public Object getContent() {
      return file;
    }

    @Override
    public String getContentType() {
      return contentType;
    }

    @Override
    public String getInputName() {
      return inputName;
    }
  }
}
