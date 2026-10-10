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
package org.gbif.ipt.web.support;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Struts' stream result as IPT 3 configured it for data dir files. */
public final class FileStreams {

  private FileStreams() {
  }

  /**
   * @param charset whether IPT 3 declared {@code contentCharSet} UTF-8 for this download
   */
  public static void write(HttpServletRequest request, HttpServletResponse response, File data, String mimeType,
                           String filename, InputStream in, boolean charset) throws IOException {
    response.setContentType(charset ? mimeType + ";charset=" + StandardCharsets.UTF_8.name() : mimeType);
    response.setHeader("Content-Disposition", "filename=\"" + filename + "\"");
    response.setContentLengthLong(data.length());
    if (!"HEAD".equals(request.getMethod())) {
      in.transferTo(response.getOutputStream());
    }
  }
}
