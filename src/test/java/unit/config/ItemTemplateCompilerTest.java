package unit.config;

import com.cebonk03.packetmenu.adapter.config.ItemTemplateCompiler;
import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import java.io.BufferedReader;
import java.io.StringReader;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Unit tests for {@link ItemTemplateCompiler}.
 *
 * <p>Tests exercise the public {@code compile()} API exclusively, since
 * individual parsing helpers are package-private.
 */
@DisplayName("ItemTemplateCompiler")
final class ItemTemplateCompilerTest {

    // ── Helper: parse YAML string into ConfigurationNode ───────────────────────

    private static ConfigurationNode parseYaml(final String yaml) {
        try {
            final var loader = YamlConfigurationLoader.builder()
                .source(() -> new BufferedReader(new StringReader(yaml)))
                .build();
            return loader.load();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to parse test YAML", e);
        }
    }

    // ── Material resolution ───────────────────────────────────────────────────

    @Nested
    @DisplayName("material resolution")
    final class MaterialResolutionTest {

        @Test
        @DisplayName("should resolve standard material name")
        void standardMaterial() {
            final ConfigurationNode node = parseYaml("material: STONE\n");
            final ItemStackSnapshot item = ItemTemplateCompiler.compile(node);
            assertEquals("minecraft:stone", item.materialKey().asString());
        }

        @Test
        @DisplayName("should resolve namespaced material key")
        void namespacedMaterial() {
            final ConfigurationNode node = parseYaml("material: minecraft:iron_ingot\n");
            final ItemStackSnapshot item = ItemTemplateCompiler.compile(node);
            assertEquals("minecraft:iron_ingot", item.materialKey().asString());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for blank material")
        void blankMaterial() {
            final ConfigurationNode node = parseYaml("material: ''\n");
            assertThrows(IllegalArgumentException.class,
                () -> ItemTemplateCompiler.compile(node));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for missing material")
        void missingMaterial() {
            final ConfigurationNode node = parseYaml("other_field: 1\n");
            assertThrows(IllegalArgumentException.class,
                () -> ItemTemplateCompiler.compile(node));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for unknown material")
        void unknownMaterial() {
            final ConfigurationNode node = parseYaml("material: NOT_A_REAL_MATERIAL_XYZ\n");
            assertThrows(IllegalArgumentException.class,
                () -> ItemTemplateCompiler.compile(node));
        }
    }

    // ── Display name ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("display name parsing")
    final class DisplayNameTest {

        @Test
        @DisplayName("should use empty component when display_name is absent")
        void absentDisplayName() {
            final ConfigurationNode node = parseYaml("material: STONE\n");
            final ItemStackSnapshot item = ItemTemplateCompiler.compile(node);
            assertEquals(Component.empty(), item.displayName());
        }

        @Test
        @DisplayName("should parse MiniMessage display name")
        void miniMessageDisplayName() {
            final ConfigurationNode node = parseYaml(
                "material: STONE\ndisplay_name: '<red>Hello'\n");
            final ItemStackSnapshot item = ItemTemplateCompiler.compile(node);
            assertNotNull(item.displayName());
            assertFalse(item.displayName().toString().isEmpty());
        }
    }

    // ── Lore ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("lore parsing")
    final class LoreTest {

        @Test
        @DisplayName("should return empty list when lore is absent")
        void absentLore() {
            final ConfigurationNode node = parseYaml("material: STONE\n");
            assertTrue(ItemTemplateCompiler.compile(node).lore().isEmpty());
        }

        @Test
        @DisplayName("should parse multiple lore lines")
        void multipleLoreLines() {
            final ConfigurationNode node = parseYaml(
                "material: STONE\nlore:\n  - 'Line 1'\n  - '<red>Line 2'\n");
            assertEquals(2, ItemTemplateCompiler.compile(node).lore().size());
        }
    }

    // ── Enchantments ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("enchantment parsing")
    final class EnchantmentTest {

        @Test
        @DisplayName("should return empty map when enchantments are absent")
        void absentEnchantments() {
            final ConfigurationNode node = parseYaml("material: STONE\n");
            assertTrue(ItemTemplateCompiler.compile(node).enchantments().isEmpty());
        }

        @Test
        @DisplayName("should parse valid enchantments")
        void validEnchantments() {
            final ConfigurationNode node = parseYaml(
                "material: STONE\nenchantments:\n"
                    + "  sharpness: 5\n"
                    + "  unbreaking: 3\n");
            final var enchantments = ItemTemplateCompiler.compile(node).enchantments();
            assertEquals(2, enchantments.size());
            assertEquals(Integer.valueOf(5), enchantments.get(
                Enchantment.getByKey(NamespacedKey.minecraft("sharpness"))));
            assertEquals(Integer.valueOf(3), enchantments.get(
                Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"))));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for unknown enchantment")
        void unknownEnchantment() {
            final ConfigurationNode node = parseYaml(
                "material: STONE\nenchantments:\n  fake_enchant: 1\n");
            assertThrows(IllegalArgumentException.class,
                () -> ItemTemplateCompiler.compile(node));
        }
    }

    // ── Item flags ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("item flag parsing")
    final class ItemFlagTest {

        @Test
        @DisplayName("should return empty set when flags are absent")
        void absentFlags() {
            final ConfigurationNode node = parseYaml("material: STONE\n");
            assertTrue(ItemTemplateCompiler.compile(node).itemFlags().isEmpty());
        }

        @Test
        @DisplayName("should parse valid item flags")
        void validFlags() {
            final ConfigurationNode node = parseYaml(
                "material: STONE\nitem_flags:\n"
                    + "  - HIDE_ATTRIBUTES\n"
                    + "  - HIDE_ENCHANTS\n");
            final var flags = ItemTemplateCompiler.compile(node).itemFlags();
            assertEquals(2, flags.size());
            assertTrue(flags.contains(ItemFlag.HIDE_ATTRIBUTES));
            assertTrue(flags.contains(ItemFlag.HIDE_ENCHANTS));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for unknown flag")
        void unknownFlag() {
            final ConfigurationNode node = parseYaml(
                "material: STONE\nitem_flags:\n  - FAKE_FLAG\n");
            assertThrows(IllegalArgumentException.class,
                () -> ItemTemplateCompiler.compile(node));
        }
    }

    // ── Full compilation ────────────────────────────────────────────────────

    @Nested
    @DisplayName("full item compilation")
    final class FullCompilationTest {

        @Test
        @DisplayName("should compile a full item with all fields")
        void fullItem() {
            final ConfigurationNode node = parseYaml(
                "material: DIAMOND_SWORD\n"
                    + "amount: 2\n"
                    + "display_name: '<gold>Golden Sword'\n"
                    + "lore:\n"
                    + "  - '<gray>A mighty sword'\n"
                    + "  - '<dark_gray>+10 damage'\n"
                    + "enchantments:\n"
                    + "  sharpness: 7\n"
                    + "  fire_aspect: 2\n"
                    + "item_flags:\n"
                    + "  - HIDE_ATTRIBUTES\n"
                    + "custom_model_data: 42\n"
                    + "durability: 100\n"
                    + "nbt: '{test:true}'\n"
                    + "skull_texture: 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6IiJ9fX0='\n");

            final ItemStackSnapshot item = ItemTemplateCompiler.compile(node);

            assertAll("full item snapshot",
                () -> assertEquals("minecraft:diamond_sword",
                    item.materialKey().asString()),
                () -> assertEquals(2, item.amount()),
                () -> assertFalse(item.displayName().toString().isEmpty()),
                () -> assertEquals(2, item.lore().size()),
                () -> assertEquals(2, item.enchantments().size()),
                () -> assertEquals(1, item.itemFlags().size()),
                () -> assertEquals(42, item.customModelData()),
                () -> assertEquals(100, item.durability()),
                () -> assertEquals("{test:true}", item.nbt()),
                () -> assertNotNull(item.skullTexture())
            );
        }

        @Test
        @DisplayName("should compile item with only required material")
        void minimalItem() {
            final ConfigurationNode node = parseYaml("material: STONE\n");

            final ItemStackSnapshot item = ItemTemplateCompiler.compile(node);

            assertAll("minimal item",
                () -> assertEquals("minecraft:stone", item.materialKey().asString()),
                () -> assertEquals(1, item.amount()),
                () -> assertEquals(Component.empty(), item.displayName()),
                () -> assertTrue(item.lore().isEmpty()),
                () -> assertTrue(item.enchantments().isEmpty()),
                () -> assertTrue(item.itemFlags().isEmpty()),
                () -> assertNull(item.nbt()),
                () -> assertEquals(0, item.customModelData()),
                () -> assertEquals(0, item.durability()),
                () -> assertNull(item.skullTexture())
            );
        }

        @Test
        @DisplayName("should throw for null ConfigurationNode")
        void nullNode() {
            assertThrows(IllegalArgumentException.class,
                () -> ItemTemplateCompiler.compile(null));
        }
    }
}
