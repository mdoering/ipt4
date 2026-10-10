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
package org.gbif.ipt.web.manage;

import org.gbif.ipt.app.PortalFixture;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("manage-probe")
class ManageGuardTest {

  static final String OWNER = "owner@example.org";
  static final String OTHER = "other@example.org";

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = PortalFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired ResourceManager resources;

  @BeforeEach
  void managers() throws Exception {
    for (String email : new String[]{OWNER, OTHER}) {
      if (users.get(email) == null) {
        User u = new User();
        u.setEmail(email);
        u.setFirstname("M");
        u.setLastname("Anager");
        u.setRole(User.Role.Manager);
        u.setPassword("Secret-pass-1");
        users.create(u);
      }
    }
    Resource res1 = resources.get("res1");
    res1.getManagers().removeIf(m -> OWNER.equals(m.getEmail()));
    res1.addManager(users.get(OWNER));
  }

  private MockHttpServletResponse probe(String query, String as, MockHttpSession session) throws Exception {
    return mvc.perform(get(java.net.URI.create("/manage/_probe.do" + query)).session(session).with(user(as))).andReturn().getResponse();
  }

  @Test
  void managersOfTheResourceGetThrough() throws Exception {
    assertThat(probe("?r=res1&x=a%26b", OWNER, new MockHttpSession()).getRedirectedUrl())
        .isEqualTo("/manage/_ok?r=res1&x=a%26b");
  }

  @Test
  void otherManagersAndUnknownResourcesAreStopped() throws Exception {
    MockHttpServletResponse other = probe("?r=res1", OTHER, new MockHttpSession());
    assertThat(other.getStatus()).isEqualTo(401);
    assertThat(probe("?r=nope", OWNER, new MockHttpSession()).getStatus()).isEqualTo(404);
    assertThat(probe("?r=", OWNER, new MockHttpSession()).getStatus()).isEqualTo(404);
  }

  @Test
  void theSessionsResourceIsCheckedToo() throws Exception {
    MockHttpSession session = new MockHttpSession();
    assertThat(probe("?r=res1", OWNER, session).getRedirectedUrl()).startsWith("/manage/_ok?r=res1");
    // the same browser, now someone else (or the owner after losing the rights)
    assertThat(probe("", OTHER, session).getStatus()).isEqualTo(401);
    assertThat(probe("", OWNER, session).getStatus()).isEqualTo(404);
  }

  @Test
  void aRefusedResourceIsNotRemembered() throws Exception {
    MockHttpSession session = new MockHttpSession();
    assertThat(probe("?r=res1", OTHER, session).getStatus()).isEqualTo(401);
    assertThat(probe("", OTHER, session).getStatus()).isEqualTo(404);
  }
}
