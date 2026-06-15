package unit.config;

import com.cebonk03.packetmenu.adapter.config.ConfigurateMenuLoader;
import com.cebonk03.packetmenu.adapter.config.InheritedMenuLoader;
import com.cebonk03.packetmenu.adapter.config.InvalidMenuException;
import com.cebonk03.packetmenu.core.domain.MenuTemplate;
import com.cebonk03.packetmenu.core.port.ActionParser;
import com.cebonk03.packetmenu.core.port.MenuRegistry;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InheritedMenuLoader}.
 *
 * <p>Tests cover inheritance merge, field override, cycle detection,
 * missing parent handling, and the {@code loadAll()} flow.
 */
@DisplayName("InheritedMenuLoader")
final class InheritedMenuLoaderTest {

    private ConfigurateMenuLoader realLoader;
    private MenuRegistry registry;
    private SchedulerPort scheduler;
    private ActionParser actionParser;

    @BeforeEach
    void setUp() {
        scheduler = mock(SchedulerPort.class);
        actionParser = mock(ActionParser.class);
        realLoader = new ConfigurateMenuLoader(scheduler, actionParser);
        registry = mock(MenuRegistry.class);
    }

    // ── Inheritance merge via loadAll() ────────────────────────────────────────

    @Nested
    @DisplayName("inheritance merge via loadAll()")
    final class LoadAllMergeTest {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("child menu title overrides parent title")
        void childTitleOverridesParent() throws Exception {
            createMenuFile(tempDir, "parent.yml",
                "id: parent\n"
                    + "menu_title: Parent Title\n"
                    + "menu_type: CHEST\n"
                    + "rows: 1\n");
            createMenuFile(tempDir, "child.yml",
                "id: child\n"
                    + "extends: parent\n"
                    + "menu_title: Child Title\n"
                    + "menu_type: CHEST\n"
                    + "rows: 1\n");

            final MenuTemplate parentTmpl = realLoader.load(tempDir.resolve("parent.yml"));
            when(registry.getTemplate("parent")).thenReturn(parentTmpl);

            final var inheritedLoader = new InheritedMenuLoader(realLoader, registry);
            final Map<String, MenuTemplate> results = inheritedLoader.loadAll(tempDir);

            final MenuTemplate child = results.get("child");
            assertNotNull(child);
            assertEquals(Component.text("Child Title"), child.title());
        }

        @Test
        @DisplayName("child slots merge with parent slots by (slot, priority)")
        void childSlotsMergeWithParent() throws Exception {
            createMenuFile(tempDir, "parent.yml",
                "id: parent\n"
                    + "menu_title: Parent\n"
                    + "menu_type: CHEST\n"
                    + "rows: 3\n"
                    + "items:\n"
                    + "  a:\n"
                    + "    material: STONE\n"
                    + "    slot: 0\n"
                    + "    priority: 10\n"
                    + "  b:\n"
                    + "    material: DIRT\n"
                    + "    slot: 1\n"
                    + "    priority: 5\n");
            createMenuFile(tempDir, "child.yml",
                "id: child\n"
                    + "extends: parent\n"
                    + "menu_title: Child\n"
                    + "menu_type: CHEST\n"
                    + "rows: 3\n"
                    + "items:\n"
                    + "  a:\n"
                    + "    material: DIAMOND\n"
                    + "    slot: 0\n"
                    + "    priority: 10\n"
                    + "  c:\n"
                    + "    material: EMERALD\n"
                    + "    slot: 5\n"
                    + "    priority: 8\n");

            final MenuTemplate parentTmpl = realLoader.load(tempDir.resolve("parent.yml"));
            when(registry.getTemplate("parent")).thenReturn(parentTmpl);

            final var inheritedLoader = new InheritedMenuLoader(realLoader, registry);
            final Map<String, MenuTemplate> results = inheritedLoader.loadAll(tempDir);

            final MenuTemplate child = results.get("child");
            assertNotNull(child);
            assertEquals(3, child.slotTemplates().size(),
                "merged child should have 3 slots (parent's DIRT at slot 1 + "
                    + "child's DIAMOND at slot 0 + EMERALD at slot 5)");
        }
    }

    // ── Cycle detection ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("cycle detection")
    final class CycleDetectionTest {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("should detect direct self-cycle (A extends A)")
        void selfCycle() throws Exception {
            final Path menuFile = tempDir.resolve("selfcycle.yml");
            Files.writeString(menuFile,
                "id: selfcycle\n"
                    + "extends: selfcycle\n"
                    + "menu_title: Self\n"
                    + "menu_type: CHEST\n");

            realLoader.load(menuFile);
            when(registry.getTemplate("selfcycle")).thenReturn(null);

            final var inheritedLoader = new InheritedMenuLoader(realLoader, registry);

            final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> inheritedLoader.load(menuFile));
            assertInstanceOf(InvalidMenuException.class, ex.getCause());
        }
    }

    // ── Missing parent ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("missing parent handling")
    final class MissingParentTest {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("should throw when parent template is not registered")
        void missingParent() throws Exception {
            final Path menuFile = tempDir.resolve("orphan.yml");
            Files.writeString(menuFile,
                "id: orphan\n"
                    + "extends: non-existent\n"
                    + "menu_title: Orphan\n"
                    + "menu_type: CHEST\n");

            when(registry.getTemplate("non-existent")).thenReturn(null);

            final var inheritedLoader = new InheritedMenuLoader(realLoader, registry);

            final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> inheritedLoader.load(menuFile));
            assertInstanceOf(InvalidMenuException.class, ex.getCause());
            assertTrue(ex.getCause().getMessage().contains("not found"));
        }
    }

    // ── No inheritance ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("no inheritance")
    final class NoInheritanceTest {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("menu without extends field passes through unchanged")
        void noExtendsPassThrough() throws Exception {
            createMenuFile(tempDir, "simple.yml",
                "id: simple\n"
                    + "menu_title: Simple\n"
                    + "menu_type: CHEST\n"
                    + "rows: 1\n");

            final var inheritedLoader = new InheritedMenuLoader(realLoader, registry);
            final Map<String, MenuTemplate> results = inheritedLoader.loadAll(tempDir);

            final MenuTemplate tmpl = results.get("simple");
            assertNotNull(tmpl);
            assertEquals("simple", tmpl.id());
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private static void createMenuFile(
            final Path dir,
            final String fileName,
            final String content
    ) throws Exception {
        Files.writeString(dir.resolve(fileName), content);
    }
}
