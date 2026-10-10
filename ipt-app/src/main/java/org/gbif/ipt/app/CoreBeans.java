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
package org.gbif.ipt.app;

import org.gbif.ipt.i18n.BundleMessages;
import org.gbif.ipt.i18n.Messages;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Beans for ipt-core classes, which carry no Spring annotations of their own. */
@Configuration(proxyBeanMethods = false)
public class CoreBeans {

  @Bean
  public Messages messages() {
    return new BundleMessages();
  }
}
