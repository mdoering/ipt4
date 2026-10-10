package org.gbif.ipt.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** ipt-core must stay usable without any web framework (see docs/design.md, section 1). */
@AnalyzeClasses(packages = "org.gbif", importOptions = ImportOption.DoNotIncludeTests.class)
class CoreBoundaryTest {

  @ArchTest
  static final ArchRule coreDoesNotUseWebFrameworks =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "jakarta.servlet..",
              "org.springframework.web.servlet..",
              "org.springframework.boot..",
              "org.apache.struts2..",
              "com.opensymphony.xwork2..")
          .allowEmptyShould(true);
}
