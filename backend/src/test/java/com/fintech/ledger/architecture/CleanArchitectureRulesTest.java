package com.fintech.ledger.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit architectural constraints enforcing Clean Architecture / DDD rules:
 * - Domain layer is pure and must never depend on external frameworks or infrastructure.
 * - Application layer depends only on Domain, never on Infrastructure or Web.
 * - Layered onion dependency rule is validated automatically on every build.
 */
@AnalyzeClasses(packages = "com.fintech.ledger", importOptions = {ImportOption.DoNotIncludeTests.class})
public class CleanArchitectureRulesTest {

    @ArchTest
    public static final ArchRule domainMustNotDependOnExternalFrameworks =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "org.hibernate..",
                            "..application..",
                            "..infrastructure.."
                    )
                    .because("Domain layer must be completely pure, framework-agnostic, and self-contained (Clean Architecture)");

    @ArchTest
    public static final ArchRule applicationMustNotDependOnInfrastructure =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("Application layer must depend only on Domain abstractions (Ports), never on concrete Infrastructure");

    @ArchTest
    public static final ArchRule controllersMustResideInWebPackage =
            classes()
                    .that().haveSimpleNameEndingWith("Controller")
                    .should().resideInAPackage("..infrastructure.web.controller..")
                    .because("REST API controllers must be located strictly in the primary web presentation package");
}
