package com.aegis.identity;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces hexagonal architecture boundaries for the Identity service.
 *
 * <p>Rules:
 * <ul>
 *   <li>domain must be pure Java - no Spring, JPA, Hibernate or infrastructure imports</li>
 *   <li>no layer may depend outward: web and infrastructure depend only inward</li>
 *   <li>controllers must not carry swagger annotations (spec-first contracts live in YAML)</li>
 * </ul>
 */
class ArchitectureTest {

    private static final JavaClasses IMPORTED = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_ARCHIVES)
            .importPackages("com.aegis.identity");

    @Test
    void domainLayerIsFrameworkFree() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.aegis.identity.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..",
                        "io.swagger..", "org.springdoc..", "com.aegis.identity.infrastructure..", "com.aegis.identity.web..")
                .because("the domain layer must be pure Java with zero framework dependencies");
        rule.check(IMPORTED);
    }

    @Test
    void applicationDependsOnlyOnDomainAndApplication() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.aegis.identity.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.aegis.identity.infrastructure..", "com.aegis.identity.web..")
                .because("application services must depend only on domain ports");
        rule.check(IMPORTED);
    }

    @Test
    void webDependsOnlyOnApplicationAndDomain() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.aegis.identity.web..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.aegis.identity.infrastructure..")
                .because("the web layer must not reach directly into infrastructure");
        rule.check(IMPORTED);
    }

    @Test
    void controllersCarryNoSwaggerAnnotations() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.aegis.identity.web.controller..")
                .should().notBeAnnotatedWith("io.swagger.v3.oas.annotations.Operation")
                .andShould().notBeAnnotatedWith("io.swagger.v3.oas.annotations.tags.Tag")
                .andShould().notBeAnnotatedWith("io.swagger.v3.oas.annotations.responses.ApiResponse")
                .andShould().notBeAnnotatedWith("io.swagger.v3.oas.annotations.Parameter")
                .because("OpenAPI contracts are defined spec-first in YAML, controllers must not duplicate them");
        rule.check(IMPORTED);
    }
}
