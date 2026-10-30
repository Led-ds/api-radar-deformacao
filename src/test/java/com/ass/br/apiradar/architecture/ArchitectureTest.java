package com.ass.br.apiradar.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Testes arquiteturais automatizados com ArchUnit para garantir conformidade
 * com Clean Architecture, Monólito Modular e prevenção de acoplamento circular.
 */
public class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.ass.br.apiradar");
    }

    @Test
    @DisplayName("Garante ausência absoluta de dependências circulares entre os módulos implementados (shared, territory, catalog, interferometry)")
    void noCircularDependenciesBetweenImplementedModules() {
        JavaClasses modularClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(
                        "com.ass.br.apiradar.shared",
                        "com.ass.br.apiradar.territory",
                        "com.ass.br.apiradar.catalog",
                        "com.ass.br.apiradar.interferometry"
                );

        slices().matching("com.ass.br.apiradar.(*)..")
                .should().beFreeOfCycles()
                .check(modularClasses);
    }

    @Test
    @DisplayName("O módulo shared não pode depender dos módulos territory, catalog ou interferometry")
    void sharedModuleDoesNotDependOnOtherModules() {
        noClasses().that().resideInAPackage("com.ass.br.apiradar.shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.ass.br.apiradar.territory..",
                        "com.ass.br.apiradar.catalog..",
                        "com.ass.br.apiradar.interferometry.."
                )
                .check(importedClasses);
    }

    @Test
    @DisplayName("O módulo territory não pode depender dos módulos catalog ou interferometry")
    void territoryDoesNotDependOnCatalogOrInterferometry() {
        noClasses().that().resideInAPackage("com.ass.br.apiradar.territory..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.ass.br.apiradar.catalog..",
                        "com.ass.br.apiradar.interferometry.."
                )
                .check(importedClasses);
    }

    @Test
    @DisplayName("O módulo catalog não pode depender do módulo interferometry")
    void catalogDoesNotDependOnInterferometry() {
        noClasses().that().resideInAPackage("com.ass.br.apiradar.catalog..")
                .should().dependOnClassesThat().resideInAPackage("com.ass.br.apiradar.interferometry..")
                .check(importedClasses);
    }

    @Test
    @DisplayName("A camada de domínio pura do territory não deve depender de classes do Spring ou Web")
    void territoryDomainDoesNotDependOnSpringOrWeb() {
        noClasses().that().resideInAPackage("com.ass.br.apiradar.territory.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web..",
                        "org.springframework.stereotype..",
                        "jakarta.servlet.."
                )
                .check(importedClasses);
    }

    @Test
    @DisplayName("A camada de domínio pura do interferometry não deve depender de classes do Spring ou Web")
    void interferometryDomainDoesNotDependOnSpringOrWeb() {
        noClasses().that().resideInAPackage("com.ass.br.apiradar.interferometry.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web..",
                        "org.springframework.stereotype..",
                        "jakarta.servlet.."
                )
                .check(importedClasses);
    }
}
