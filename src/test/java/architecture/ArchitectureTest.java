package architecture;

import com.cebonk03.packetmenu.bootstrap.PacketMenuPlugin;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture tests enforcing hexagonal architecture boundaries for PacketMenu.
 *
 * <p>Verifies that the project follows clean hexagonal layering:
 * domain → core (port + service) → adapter → bootstrap.
 */
@AnalyzeClasses(packagesOf = PacketMenuPlugin.class)
class ArchitectureTest {

    // ── Rule 1: Hexagonal Layer Boundaries ──────────────────────────────────────

    @ArchTest
    static final ArchRule HEXAGONAL_LAYERS_ARE_RESPECTED =
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("..core.domain..")
            .layer("Core").definedBy("..core.port..", "..core.service..")
            .layer("Adapter").definedBy("..adapter..")
            .layer("Bootstrap").definedBy("..bootstrap..")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Core", "Adapter", "Bootstrap")
            .whereLayer("Core").mayOnlyBeAccessedByLayers("Adapter", "Bootstrap")
            .whereLayer("Adapter").mayOnlyBeAccessedByLayers("Bootstrap")
            .because("Hexagonal architecture: domain → core → adapter → bootstrap");

    // ── Rule 2: No Bukkit in Core Layer ────────────────────────────────────────

    @ArchTest
    static final ArchRule CORE_MUST_NOT_DEPEND_ON_BUKKIT =
        noClasses()
            .that().resideInAnyPackage("..core..")
            .should().dependOnClassesThat().resideInAnyPackage("org.bukkit..")
            .because("Core layer must be platform-agnostic and not depend on Bukkit API");

    // ── Rule 3: No PacketEvents in Core Layer ───────────────────────────────────

    @ArchTest
    static final ArchRule CORE_MUST_NOT_DEPEND_ON_PACKETEVENTS =
        noClasses()
            .that().resideInAnyPackage("..core..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "com.github.retrooper.packetevents..")
            .because("Core layer must not depend on PacketEvents library");

    // ── Rule 4a: Naming Convention — Adapter Classes ────────────────────────────

    @ArchTest
    static final ArchRule ADAPTER_CLASSES_SHOULD_END_WITH_ROLE_SUFFIX =
        classes()
            .that().resideInAnyPackage("..adapter..")
            // Known exceptions with alternative naming patterns:
            .and().doNotHaveSimpleName("InvalidMenuException")    // exception class
            .and().doNotHaveSimpleName("DeluxeActionParser")      // parser class
            .and().doNotHaveSimpleName("ItemTemplateCompiler")    // compiler class
            .and().doNotHaveSimpleName("VersionCapabilities")     // capabilities class
            .and().doNotHaveSimpleName("PaperPlayerHandle")       // handle/wrapper class
            .and().doNotHaveSimpleName("PaperSessionManager")     // manager class
            .should().haveSimpleNameEndingWith("Composer")
            .orShould().haveSimpleNameEndingWith("Adapter")
            .orShould().haveSimpleNameEndingWith("Loader")
            .orShould().haveSimpleNameEndingWith("Bus")
            .because("Adapter implementation classes should end with a recognizable"
                + " role suffix: Composer, Adapter, Loader, or Bus");

    // ── Rule 4b: Naming Convention — Service Classes ────────────────────────────

    @ArchTest
    static final ArchRule SERVICE_CLASSES_SHOULD_HAVE_MEANINGFUL_NAMES =
        classes()
            .that().resideInAnyPackage("..core.service..")
            .should().haveSimpleNameNotEndingWith("Util")
            .andShould().haveSimpleNameNotEndingWith("Helper")
            .andShould().haveSimpleNameNotEndingWith("ServiceImpl")
            .because("Service classes should have meaningful domain-specific names,"
                + " not generic suffixes like Util, Helper, or ServiceImpl");

    // ── Rule 5: No Cyclic Dependencies ──────────────────────────────────────────

    @ArchTest
    static final ArchRule NO_CYCLIC_DEPENDENCIES_BETWEEN_PACKAGES =
        slices()
            .matching("com.cebonk03.packetmenu.(*)..")
            .should().beFreeOfCycles()
            .because("Package dependency graph must be acyclic to maintain"
                + " clean separation of concerns");
}
