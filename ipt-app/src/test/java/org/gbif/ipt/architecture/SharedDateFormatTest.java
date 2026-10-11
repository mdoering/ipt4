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
package org.gbif.ipt.architecture;

import java.text.DateFormat;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * A DateFormat is not thread-safe: shared by concurrent requests it throws or, worse, formats wrong dates (the home
 * page's resource table failed this way under load). Use a ThreadLocal, a local instance or java.time.
 */
@AnalyzeClasses(packages = "org.gbif.ipt", importOptions = ImportOption.DoNotIncludeTests.class)
class SharedDateFormatTest {

  @ArchTest
  static final ArchRule noSharedDateFormats =
      noFields().that().areStatic().should().haveRawType(assignableTo(DateFormat.class));
}
