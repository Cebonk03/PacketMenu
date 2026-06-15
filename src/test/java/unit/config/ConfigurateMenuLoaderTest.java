package unit.config;

import com.cebonk03.packetmenu.adapter.config.ConfigurateMenuLoader;
import com.cebonk03.packetmenu.adapter.config.InvalidMenuException;
import com.cebonk03.packetmenu.core.domain.MenuTemplate;
import com.cebonk03.packetmenu.core.domain.MenuType;
import com.cebonk03.packetmenu.core.port.ActionParser;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ConfigurateMenuLoader}.
 *
 * <p>Tests cover valid menu parsing, error handling for invalid configurations,
 * minimal menu definitions, and MiniMessage/legacy formatting support.
 */
@DisplayName("ConfigurateMenuLoader")
final class ConfigurateMenuLoaderTest {

    private static final String RESOURCE_DIR = "src/test/resources/menus";

    private ConfigurateMenuLoader loader;
    private SchedulerPort scheduler;

    @BeforeEach
    void setUp() {
        scheduler = Mockito.mock(SchedulerPort.class);
        final ActionParser actionParser = Mockito.mock(ActionParser.class);
        loader = new ConfigurateMenuLoader(scheduler, actionParser);
    }

    // ── Valid menu ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("valid menu parsing")
    final class ValidMenuTest {

        @Test
        @DisplayName("should parse all fields from a full-featured menu YAML")
        void parseFullFeaturedMenu() throws IOException {
            final Path menuPath = Path.of(RESOURCE_DIR, "valid-menu.yml");
            assertTrue(Files.exists(menuPath), "Test YAML file must exist");

            final MenuTemplate template = loader.load(menuPath);

            assertAll("valid-menu fields",
                () -> assertEquals("valid-menu", template.id()),
                () -> assertNotNull(template.title(), "title should not be null"),
                () -> assertEquals(MenuType.GENERIC_9x3, template.type()),
                () -> assertTrue(template.openCommands().contains("menu"),
                    "should contain open command 'menu'"),
                () -> assertTrue(template.openCommands().contains("coolmenu"),
                    "should contain open command 'coolmenu'"),
                () -> assertEquals(10, template.updateInterval()),
                () -> assertFalse(template.closeOnClickOutside()),
                () -> assertNull(template.parentMenuId())
            );
        }

        @Test
        @DisplayName("should parse all item properties including enchantments, flags, NBT")
        void parseItemProperties() throws IOException {
            final Path menuPath = Path.of(RESOURCE_DIR, "valid-menu.yml");
            final MenuTemplate template = loader.load(menuPath);

            assertEquals(3, template.slotTemplates().size(),
                "should have 3 slot templates (diamond_sword + 3 pickaxe slots + skull)");

            // Diamond sword item
            final var swordSlot = template.slotTemplates().stream()
                .filter(s -> s.slot() == 4)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Slot 4 not found"));

            assertEquals(10, swordSlot.priority());
            assertEquals("minecraft:diamond_sword",
                swordSlot.baseItem().materialKey().asString());
            assertNotNull(swordSlot.baseItem().displayName());
            assertFalse(swordSlot.baseItem().displayName().toString().isEmpty(),
                "display name should not be empty");
            assertEquals(2, swordSlot.baseItem().lore().size(),
                "should have 2 lore lines");
            assertFalse(swordSlot.baseItem().enchantments().isEmpty(),
                "should have enchantments");
            assertEquals(2, swordSlot.baseItem().itemFlags().size(),
                "should have 2 item flags");
            assertEquals(1001, swordSlot.baseItem().customModelData());
            assertNotNull(swordSlot.baseItem().nbt(), "NBT should be present");
            assertTrue(swordSlot.update());
            assertEquals(5, swordSlot.updateInterval());

            // Skull item
            final var skullSlot = template.slotTemplates().stream()
                .filter(s -> s.slot() == 8)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Slot 8 not found"));

            assertEquals("minecraft:player_head",
                skullSlot.baseItem().materialKey().asString());
            assertNotNull(skullSlot.baseItem().skullTexture(),
                "skull texture should be present");
        }
    }

    // ── Invalid menu ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("invalid menu parsing")
    final class InvalidMenuTest {

        @Test
        @DisplayName("should throw RuntimeException for slot out of bounds")
        void badSlot() {
            final Path menuPath = Path.of(RESOURCE_DIR, "invalid-menu.yml");
            final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loader.load(menuPath),
                "Expected RuntimeException for bad slot");

            assertInstanceOf(InvalidMenuException.class, ex.getCause(),
                "cause should be InvalidMenuException");
            assertTrue(ex.getMessage().contains("Slot 99 out of bounds"),
                "message should mention slot out of bounds");
        }

    }

    // ── Minimal menu ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("minimal menu parsing")
    final class MinimalMenuTest {

        @Test
        @DisplayName("should parse menu with only title and type")
        void parseMinimal() throws IOException {
            final Path menuPath = Path.of(RESOURCE_DIR, "minimal-menu.yml");
            assertTrue(Files.exists(menuPath));

            final MenuTemplate template = loader.load(menuPath);

            assertAll("minimal menu fields",
                () -> assertEquals("minimal-menu", template.id()),
                () -> assertNotNull(template.title(), "title should not be null"),
                () -> assertEquals(MenuType.GENERIC_9x1, template.type()),
                () -> assertTrue(template.openCommands().isEmpty(),
                    "should have no open commands"),
                () -> assertEquals(0, template.slotTemplates().size(),
                    "should have no items"),
                () -> assertNull(template.fillerItem()),
                () -> assertEquals(0, template.updateInterval()),
                () -> assertTrue(template.closeOnClickOutside()),
                () -> assertNull(template.openRequirement())
            );
        }
    }

    // ── Formatting ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("formatting parsing")
    final class FormattingTest {

        @Test
        @DisplayName("should parse MiniMessage tags in titles and lore")
        void parseMiniMessageFormatting() throws IOException {
            final Path menuPath = Path.of(RESOURCE_DIR, "formatting-menu.yml");
            assertTrue(Files.exists(menuPath));

            final MenuTemplate template = loader.load(menuPath);

            assertAll("MiniMessage parsing",
                () -> assertNotNull(template.title(),
                    "title should parse"),
                () -> assertFalse(template.slotTemplates().isEmpty(),
                    "should have items")
            );

            // MiniMessage item
            final var mmItem = template.slotTemplates().stream()
                .filter(s -> s.slot() == 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Slot 0 not found"));

            assertEquals("minecraft:diamond",
                mmItem.baseItem().materialKey().asString());
            assertFalse(mmItem.baseItem().displayName().toString().isEmpty(),
                "display name should not be empty after parsing");
            assertEquals(3, mmItem.baseItem().lore().size(),
                "should have 3 lore lines");

            // Legacy item
            final var legacyItem = template.slotTemplates().stream()
                .filter(s -> s.slot() == 1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Slot 1 not found"));

            assertEquals("minecraft:stone",
                legacyItem.baseItem().materialKey().asString());
            assertFalse(legacyItem.baseItem().displayName().toString().isEmpty(),
                "legacy display name should parse");
            assertEquals(2, legacyItem.baseItem().lore().size(),
                "should have 2 legacy lore lines");
        }
    }

    // ── Error cases via temp files ─────────────────────────────────────────────

    @Nested
    @DisplayName("error conditions with temp files")
    final class ErrorConditionTest {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("should reject unknown menu type")
        void unknownMenuType() throws IOException {
            final Path menuFile = tempDir.resolve("badtype.yml");
            Files.writeString(menuFile, "menu_title: Test\nmenu_type: INVALID_TYPE\n");

            final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loader.load(menuFile));
            assertInstanceOf(InvalidMenuException.class, ex.getCause());
        }

        @Test
        @DisplayName("should reject missing material")
        void missingMaterial() throws IOException {
            final Path menuFile = tempDir.resolve("nomaterial.yml");
            Files.writeString(menuFile,
                "menu_title: Test\nmenu_type: CHEST\nrows: 1\nitems:\n  test:\n    slot: 0\n");

            final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loader.load(menuFile));
            assertInstanceOf(InvalidMenuException.class, ex.getCause());
            assertTrue(ex.getCause().getMessage().contains("material is required"));
        }

        @Test
        @DisplayName("should reject negative priority")
        void negativePriority() throws IOException {
            final Path menuFile = tempDir.resolve("negpriority.yml");
            Files.writeString(menuFile,
                "menu_title: Test\nmenu_type: CHEST\nrows: 1\nitems:\n  test:\n"
                    + "    material: STONE\n    slot: 0\n    priority: -1\n");

            final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loader.load(menuFile));
            assertInstanceOf(InvalidMenuException.class, ex.getCause());
            assertTrue(ex.getCause().getMessage().contains("priority must be >= 0"));
        }

        @Test
        @DisplayName("should reject update_interval > 1200")
        void updateIntervalTooHigh() throws IOException {
            final Path menuFile = tempDir.resolve("highinterval.yml");
            Files.writeString(menuFile,
                "menu_title: Test\nmenu_type: CHEST\nrows: 1\n"
                    + "update_interval: 9999\nitems:\n  test:\n"
                    + "    material: STONE\n    slot: 0\n");

            final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loader.load(menuFile));
            assertInstanceOf(InvalidMenuException.class, ex.getCause());
            assertTrue(ex.getCause().getMessage().contains("update_interval must be between"));
        }

        @Test
        @DisplayName("should reject empty slots list")
        void emptySlotsList() throws IOException {
            final Path menuFile = tempDir.resolve("emptyslots.yml");
            Files.writeString(menuFile,
                "menu_title: Test\nmenu_type: CHEST\nrows: 1\nitems:\n  test:\n"
                    + "    material: STONE\n    slots: []\n");

            final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loader.load(menuFile));
            assertInstanceOf(InvalidMenuException.class, ex.getCause());
            assertTrue(ex.getCause().getMessage().contains("slots list is empty"));
        }

        @Test
        @DisplayName("should reject item with neither slot nor slots")
        void missingSlotDefinition() throws IOException {
            final Path menuFile = tempDir.resolve("noslot.yml");
            Files.writeString(menuFile,
                "menu_title: Test\nmenu_type: CHEST\nrows: 1\nitems:\n  test:\n"
                    + "    material: STONE\n");

            final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loader.load(menuFile));
            assertInstanceOf(InvalidMenuException.class, ex.getCause());
            assertTrue(ex.getCause().getMessage().contains("must define either 'slot' or 'slots'"));
        }
    }

    // ── Edge cases ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("edge cases")
    final class EdgeCaseTest {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("should use id as title fallback when menu_title is missing")
        void titleFallback() throws IOException {
            final Path menuFile = tempDir.resolve("notitle.yml");
            Files.writeString(menuFile, "menu_type: CHEST\n");

            final MenuTemplate template = loader.load(menuFile);
            assertEquals("notitle", template.id());
        }

        @Test
        @DisplayName("should handle null action parser gracefully")
        void nullActionParser() throws IOException {
            final Path menuPath = Path.of(RESOURCE_DIR, "valid-menu.yml");
            final ConfigurateMenuLoader nullParserLoader =
                new ConfigurateMenuLoader(scheduler, null);

            final MenuTemplate template = nullParserLoader.load(menuPath);
            assertNotNull(template);
            assertTrue(template.slotTemplates().stream()
                .allMatch(s -> s.clickActions().isEmpty()),
                "click actions should be empty when parser is null");
        }
    }
}
