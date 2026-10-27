package vn.fitora.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

class ModuleArchitectureTest {
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("vn.fitora");

    @Test
    void importsTheApplication() {
        assertThat(CLASSES.contain("vn.fitora.FitoraBackendApplication")).isTrue();
    }

    @Test
    void coreDependsOnlyOnJavaAndItself() {
        // Core/fashion currently contain package documentation only.
        // Allow empty selections until their first implementation is introduced.
        classes().that().resideInAPackage("vn.fitora.core..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("java..", "vn.fitora.core..")
                .allowEmptyShould(true).check(CLASSES);
    }

    @Test
    void fashionDependsOnlyOnJavaCoreAndItself() {
        classes().that().resideInAPackage("vn.fitora.fashion..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("java..", "vn.fitora.core..", "vn.fitora.fashion..")
                .allowEmptyShould(true).check(CLASSES);
    }

    @Test
    void adaptersCannotDependOnBootstrap() {
        noClasses().that().resideInAPackage("vn.fitora.infrastructure..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("vn.fitora.bootstrap..")
                .allowEmptyShould(true).check(CLASSES);
    }

    @Test
    void modulePackagesHaveNoCycles() {
        slices().matching("vn.fitora.(*)..").should().beFreeOfCycles()
                .check(CLASSES);
    }

    @Test
    void applicationClassesUseDeclaredModulePackages() {
        classes().that().doNotHaveFullyQualifiedName("vn.fitora.FitoraBackendApplication")
                .should().resideInAnyPackage("vn.fitora.core..", "vn.fitora.fashion..",
                        "vn.fitora.infrastructure..", "vn.fitora.bootstrap..")
                .check(CLASSES);
    }
}
