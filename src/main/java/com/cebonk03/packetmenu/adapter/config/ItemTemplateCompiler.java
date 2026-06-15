package com.cebonk03.packetmenu.adapter.config;

import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import com.cebonk03.packetmenu.util.TextUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.ConfigurationNode;

/**
 * Compiles YAML configuration nodes (Configurate {@link ConfigurationNode}) into
 * immutable {@link ItemStackSnapshot} records.
 *
 * <p>All parsing helper methods are made available at the package level for
 * reuse by other classes in the {@code adapter.config} package (e.g.
 * {@code ConfigurateMenuLoader}).</p>
 *
 * <p>This compiler performs fail-fast validation with descriptive error messages
 * that include the full configuration path to the offending value.</p>
 */
@NullMarked
public final class ItemTemplateCompiler {

    private ItemTemplateCompiler() {
    }

    /**
     * Compiles a {@link ConfigurationNode} into an {@link ItemStackSnapshot}.
     *
     * <p>Expected YAML structure:</p>
     * <pre>{@code
     * material: DIAMOND_SWORD
     * amount: 1
     * display_name: "<gold>Legendary Sword"
     * lore:
     *   - "<gray>A legendary sword"
     * enchantments:
     *   sharpness: 5
     * item_flags:
     *   - HIDE_ATTRIBUTES
     * custom_model_data: 1001
     * durability: 0
     * skull_texture: "eyJ0ZXh0dXJlcyI6..."
     * nbt: "{some_nbt}"
     * }</pre>
     *
     * @param node the YAML configuration node describing an item
     * @return a fully populated immutable item snapshot
     * @throws IllegalArgumentException if required values are missing or invalid
     */
    public static ItemStackSnapshot compile(final ConfigurationNode node) {
        if (node == null) {
            throw new IllegalArgumentException("ConfigurationNode must not be null");
        }

        final NamespacedKey materialKey = resolveMaterialKey(node.node("material"));
        final int amount = node.node("amount").getInt(1);
        final Component displayName = parseDisplayName(node.node("display_name"));
        final List<Component> lore = parseLore(node.node("lore"));
        final Map<Enchantment, Integer> enchantments = parseEnchantments(node.node("enchantments"));
        final Set<ItemFlag> itemFlags = parseItemFlags(node.node("item_flags"));
        final String nbt = node.node("nbt").getString();
        final int customModelData = node.node("custom_model_data").getInt(0);
        final int durability = node.node("durability").getInt(0);
        final String skullTexture = node.node("skull_texture").getString();

        return new ItemStackSnapshot(
            materialKey,
            amount,
            displayName,
            lore,
            enchantments,
            itemFlags,
            nbt,
            customModelData,
            durability,
            skullTexture
        );
    }

    // ── Package-private parsing helpers ──────────────────────────────────────────

    /**
     * Resolves the material name from the given node into a {@link NamespacedKey}.
     *
     * <p>Supports both plain material names ({@code STONE}) and namespaced
     * keys ({@code minecraft:stone}). The {@code minecraft:} prefix is stripped
     * before lookup.</p>
     *
     * @param node the configuration node containing the material name
     * @return the resolved material key
     * @throws IllegalArgumentException if the material name is missing, blank,
     *                                  or does not correspond to a known material
     */
    static NamespacedKey resolveMaterialKey(final ConfigurationNode node) {
        final String raw = node.getString();
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(
                "Missing or blank 'material' at path '%s'".formatted(formatPath(node))
            );
        }

        String materialName = raw;
        if (materialName.startsWith("minecraft:")) {
            materialName = materialName.substring("minecraft:".length());
        }

        final Material material = Material.matchMaterial(materialName);
        if (material == null) {
            throw new IllegalArgumentException(
                "Unknown material '%s' at path '%s'".formatted(raw, formatPath(node))
            );
        }

        return material.getKey();
    }

    /**
     * Parses a display name from the given node.
     *
     * <p>The value is parsed as a MiniMessage string with legacy color code
     * fallback via {@link TextUtil#parseMiniMessage}.</p>
     *
     * @param node the configuration node containing the display name
     * @return the parsed component, or {@link Component#empty()} if absent
     */
    static Component parseDisplayName(final ConfigurationNode node) {
        final String raw = node.getString();
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        return TextUtil.parseMiniMessage(raw);
    }

    /**
     * Parses a list of lore lines from the given node.
     *
     * <p>Each line is parsed through {@link TextUtil#parseMiniMessage} to
     * support both MiniMessage and legacy color codes.</p>
     *
     * @param node the configuration node containing the lore list
     * @return an immutable list of parsed lore components (may be empty)
     */
    static List<Component> parseLore(final ConfigurationNode node) {
        final List<? extends ConfigurationNode> children = node.childrenList();
        if (children.isEmpty()) {
            return List.of();
        }
        final List<Component> lore = new ArrayList<>(children.size());
        for (final ConfigurationNode child : children) {
            final String line = child.getString();
            if (line == null || line.isBlank()) {
                continue;
            }
            lore.add(TextUtil.parseMiniMessage(line));
        }
        return Collections.unmodifiableList(lore);
    }

    /**
     * Parses an enchantment map from the given node.
     *
     * <p>Expected format:</p>
     * <pre>{@code
     * enchantments:
     *   sharpness: 5
     *   unbreaking: 3
     * }</pre>
     *
     * <p>Enchantment keys are looked up via
     * {@link Enchantment#getByKey(NamespacedKey)} using
     * {@link NamespacedKey#minecraft(String)}.</p>
     *
     * @param node the configuration node containing enchantment definitions
     * @return an immutable map of enchantment to level (may be empty)
     * @throws IllegalArgumentException if an enchantment key is unknown
     */
    static Map<Enchantment, Integer> parseEnchantments(final ConfigurationNode node) {
        final Map<Object, ? extends ConfigurationNode> children = node.childrenMap();
        if (children.isEmpty()) {
            return Map.of();
        }
        final Map<Enchantment, Integer> result = new HashMap<>(children.size());
        for (final Map.Entry<Object, ? extends ConfigurationNode> entry : children.entrySet()) {
            final String key = entry.getKey().toString();
            final int level = entry.getValue().getInt(1);
            final Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(key));
            if (enchantment == null) {
                throw new IllegalArgumentException(
                    "Unknown enchantment '%s' at path '%s'".formatted(key, formatPath(node))
                );
            }
            result.put(enchantment, level);
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Parses a set of item flags from the given node.
     *
     * <p>Expected format:</p>
     * <pre>{@code
     * item_flags:
     *   - HIDE_ATTRIBUTES
     *   - HIDE_ENCHANTS
     * }</pre>
     *
     * @param node the configuration node containing item flag names
     * @return an immutable set of parsed item flags (may be empty)
     * @throws IllegalArgumentException if a flag name is unknown
     */
    static Set<ItemFlag> parseItemFlags(final ConfigurationNode node) {
        final List<? extends ConfigurationNode> children = node.childrenList();
        if (children.isEmpty()) {
            return Set.of();
        }
        final Set<ItemFlag> flags = EnumSet.noneOf(ItemFlag.class);
        for (final ConfigurationNode child : children) {
            final String raw = child.getString();
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                flags.add(ItemFlag.valueOf(raw.toUpperCase()));
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Unknown item flag '%s' at path '%s'".formatted(raw, formatPath(node))
                );
            }
        }
        return Collections.unmodifiableSet(flags);
    }

    // ── Private utilities ────────────────────────────────────────────────────────

    /**
     * Formats the full path of a configuration node as a dot-separated string
     * suitable for error messages.
     *
     * @param node the configuration node
     * @return a dot-separated path string, or {@code "<root>"} if the path is empty
     */
    private static String formatPath(final ConfigurationNode node) {
        final Object[] path = node.path().array();
        if (path == null || path.length == 0) {
            return "<root>";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.length; i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(path[i]);
        }
        return sb.toString();
    }
}
