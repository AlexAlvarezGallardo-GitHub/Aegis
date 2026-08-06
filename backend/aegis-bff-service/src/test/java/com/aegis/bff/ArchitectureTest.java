package com.aegis.bff;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces hexagonal architecture boundaries for the BFF service.
 */
class ArchitectureTest {

    private static final JavaClasses IMPORTED = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_ARCHIVES)
            .importPackages("com.aegis.bff");

    @Test
    void domainLayerIsFrameworkFree() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.aegis.bff.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..",
                        "io.swagger..", "org.springdoc..", "com.aegis.bff.infrastructure..", "com.aegis.bff.web..")
                .because("the domain layer must be pure Java with zero framework dependencies");
        rule.check(IMPORTED);
    }

    @Test
    void applicationDependsOnlyOnDomainAndApplication() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.aegis.bff.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.aegis.bff.infrastructure..", "com.aegis.bff.web..")
                .because("application services must depend only on domain ports");
        rule.check(IMPORTED);
    }
}
