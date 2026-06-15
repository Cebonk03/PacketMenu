package com.cebonk03.packetmenu.adapter.config;

import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.domain.MenuTemplate;
import com.cebonk03.packetmenu.core.domain.MenuType;
import com.cebonk03.packetmenu.core.domain.Requirement;

import com.cebonk03.packetmenu.core.domain.SlotTemplate;
import com.cebonk03.packetmenu.core.domain.ViewRequirement;
import com.cebonk03.packetmenu.core.port.ActionParser;
import com.cebonk03.packetmenu.core.port.MenuLoader;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import com.cebonk03.packetmenu.util.TextUtil;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/**
 * {@link MenuLoader} implementation that reads DeluxeMenus-style YAML
 * configuration files using the Configurate-YAML 4.1.2 library.
 *
 * <p>Supports both {@code slot} (single integer) and {@code slots}
 * (list of integers) per item definition, MiniMessage with legacy {@code §}
 * colour fallback, NBT compound strings, enchantments, item flags, skull
 * textures, and custom model data.  YAML anchors and references are resolved
 * transparently by the Configurate engine.
 *
 * <p>All validation is performed eagerly at parse time: slot bounds,
 * {@code priority &ge; 0}, material existence, and
 * {@code update_interval &le; 1200}.  Circular {@code parentMenuId} references
 * across all loaded menus are detected in a second pass.
 *
 * <p>This loader is stateless and thread-safe.
 */
@NullMarked
public final class ConfigurateMenuLoader implements MenuLoader {

    private static final int MAX_UPDATE_INTERVAL = 1200;
    private static final int MIN_UPDATE_INTERVAL = 0;

    private final SchedulerPort scheduler;
    private final @Nullable ActionParser actionParser;

    /**
     * Creates a new {@code ConfigurateMenuLoader}.
     *
     * @param scheduler    the scheduler port (used for async loading)
     * @param actionParser the action parser for parsing click action strings,
     *                     may be {@code null}
     */
    public ConfigurateMenuLoader(
            final SchedulerPort scheduler,
            final @Nullable ActionParser actionParser
    ) {
        this.scheduler = scheduler;
        this.actionParser = actionParser;
    }

    // ── MenuLoader implementation ──────────────────────────────────────────────

    @Override
    public MenuTemplate load(final Path path) {
        try {
            return loadInternal(path);
        } catch (final InvalidMenuException e) {
            throw new RuntimeException(
                "Failed to load menu from " + path.toAbsolutePath() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, MenuTemplate> loadAll(final Path directory) {
        final List<Path> yamlFiles = discoverYamlFiles(directory);
        // Sort for deterministic ordering
        Collections.sort(yamlFiles);

        final Map<String, MenuTemplate> templates = new HashMap<>();

        // First pass — parse every file
        for (final Path file : yamlFiles) {
            final String id = fileStem(file);
            try {
                final MenuTemplate template = loadInternal(file);
                templates.put(id, template);
            } catch (final InvalidMenuException e) {
                throw new RuntimeException(
                    "Failed to load menu '" + id + "': " + e.getMessage(), e);
            }
        }

        // Second pass — detect circular parentMenuId references
        try {
            detectCircularLinks(templates);
        } catch (final InvalidMenuException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return Collections.unmodifiableMap(templates);
    }

    // ── Single-file loading ────────────────────────────────────────────────────

    private MenuTemplate loadInternal(final Path path) throws InvalidMenuException {
        final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
            .indent(2)
            .path(path)
            .build();

        final ConfigurationNode root;
        final String fileName = fileName(path);
        try {
            root = loader.load();
        } catch (final ConfigurateException e) {
            throw new InvalidMenuException(
                fileName, "root",
                "Failed to parse YAML: " + e.getMessage());
        }
        final String id = fileStem(path);

        // ── menu_title ──────────────────────────────────────────────────────
        final Component title = parseTitle(root, fileName, id);

        // ── menu_type ───────────────────────────────────────────────────────
        final MenuType type = parseMenuType(root, fileName);

        // ── open_command ────────────────────────────────────────────────────
        final List<String> openCommands = parseOpenCommands(root);

        // ── open_requirement (stub — real parsing belongs to a later task) ──
        final @Nullable Requirement openRequirement = parseOpenRequirement(root, fileName);

        // ── items ───────────────────────────────────────────────────────────
        final List<SlotTemplate> slotTemplates = parseItems(root, type, fileName);

        // ── filler_item ─────────────────────────────────────────────────────
        final @Nullable ItemStackSnapshot fillerItem = parseFillerItem(root, fileName);

        // ── update_interval ─────────────────────────────────────────────────
        final int updateInterval = parseUpdateInterval(root, fileName);

        // ── close_on_click_outside ──────────────────────────────────────────
        final boolean closeOnClickOutside =
            root.node("close_on_click_outside").getBoolean(true);

        // ── parent_menu ─────────────────────────────────────────────────────
        final @Nullable String parentMenuId = parseOptionalString(root, "parent_menu");

        return new MenuTemplate(
            id, title, type, openCommands, openRequirement,
            slotTemplates, fillerItem, updateInterval,
            closeOnClickOutside, parentMenuId);
    }

    // ── Menu-level attribute parsers ───────────────────────────────────────────

    private static Component parseTitle(
            final ConfigurationNode root,
            final String fileName,
            final String fallbackId
    ) throws InvalidMenuException {
        final ConfigurationNode node = root.node("menu_title");
        if (node.virtual()) {
            return Component.text(fallbackId);
        }
        final String raw = node.getString();
        if (raw == null || raw.isBlank()) {
            return Component.text(fallbackId);
        }
        return TextUtil.parseMiniMessage(raw);
    }

    private static MenuType parseMenuType(
            final ConfigurationNode root,
            final String fileName
    ) throws InvalidMenuException {
        final ConfigurationNode node = root.node("menu_type");
        final String raw = node.getString("CHEST");
        final String upper = raw.toUpperCase();

        // Try direct enum match first (e.g. GENERIC_9x3)
        for (final MenuType mt : MenuType.values()) {
            if (mt.name().equalsIgnoreCase(raw)) {
                return mt;
            }
        }

        // DeluxeMenus-style CHEST with optional rows
        if ("CHEST".equals(upper)) {
            final int rows = root.node("rows").getInt(6);
            return byRows(clamp(rows, 1, 6));
        }

        throw new InvalidMenuException(
            fileName, nodeLine(node), "menu_type",
            "Unsupported menu_type '" + raw + "'. Expected one of: "
                + "CHEST, GENERIC_9x1..GENERIC_9x6, GENERIC_3x3");
    }

    private static MenuType byRows(final int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    private static List<String> parseOpenCommands(final ConfigurationNode root) {
        final ConfigurationNode node = root.node("open_command");
        if (node.virtual()) {
            return List.of();
        }
        if (node.isList()) {
            final List<? extends ConfigurationNode> children = node.childrenList();
            final List<String> commands = new ArrayList<>(children.size());
            for (final ConfigurationNode child : children) {
                final String cmd = child.getString();
                if (cmd != null && !cmd.isBlank()) {
                    commands.add(normalizeCommand(cmd));
                }
            }
            return List.copyOf(commands);
        }
        // Single string
        final String value = node.getString();
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(normalizeCommand(value));
    }

    private static String normalizeCommand(final String cmd) {
        final String trimmed = cmd.trim();
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    private static @Nullable Requirement parseOpenRequirement(
            final ConfigurationNode root,
            final String fileName
    ) throws InvalidMenuException {
        final ConfigurationNode node = root.node("open_requirement");
        if (node.virtual()) {
            return null;
        }
        // Stub — full requirement parsing will be implemented in a later task.
        // For now return a pass-through requirement.
        return ctx -> true;
    }

    private static int parseUpdateInterval(
            final ConfigurationNode root,
            final String fileName
    ) throws InvalidMenuException {
        final ConfigurationNode node = root.node("update_interval");
        final int interval = node.getInt(0);
        if (interval < MIN_UPDATE_INTERVAL || interval > MAX_UPDATE_INTERVAL) {
            throw new InvalidMenuException(
                fileName, nodeLine(node), "update_interval",
                "update_interval must be between " + MIN_UPDATE_INTERVAL
                    + " and " + MAX_UPDATE_INTERVAL + ", got: " + interval);
        }
        return interval;
    }

    private static @Nullable String parseOptionalString(
            final ConfigurationNode root,
            final String key
    ) {
        final ConfigurationNode node = root.node(key);
        if (node.virtual()) {
            return null;
        }
        final String value = node.getString();
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    // ── Items parsing ─────────────────────────────────────────────────────────

    private List<SlotTemplate> parseItems(
            final ConfigurationNode root,
            final MenuType type,
            final String fileName
    ) throws InvalidMenuException {
        final ConfigurationNode itemsNode = root.node("items");
        if (itemsNode.virtual()) {
            return List.of();
        }

        final List<SlotTemplate> result = new ArrayList<>();
        for (final Map.Entry<Object, ? extends ConfigurationNode> entry
                : itemsNode.childrenMap().entrySet()) {
            final String itemName = entry.getKey().toString();
            final ConfigurationNode itemNode = entry.getValue();
            result.addAll(parseSingleItem(itemNode, type, fileName, itemName));
        }
        return List.copyOf(result);
    }

    private List<SlotTemplate> parseSingleItem(
            final ConfigurationNode node,
            final MenuType type,
            final String fileName,
            final String itemName
    ) throws InvalidMenuException {
        final List<Integer> slots = parseSlots(node, type, fileName, itemName);
        final ItemStackSnapshot snapshot = parseItemStack(node, fileName, itemName);
        final int priority = parsePriority(node, fileName, itemName);
        final boolean update = node.node("update").getBoolean(false);
        final int updateInterval = clamp(
            node.node("update_interval").getInt(0), 0, MAX_UPDATE_INTERVAL);

        final @Nullable ViewRequirement viewReq = parseViewRequirement(node, fileName, itemName);
        final List<MenuAction> clickActions = parseActions(node, fileName, itemName);
        final List<Requirement> clickReqs = parseClickRequirements(node, fileName, itemName);

        final List<SlotTemplate> templates = new ArrayList<>(slots.size());
        for (final int slot : slots) {
            templates.add(new SlotTemplate(
                slot, priority, snapshot, viewReq,
                clickActions, clickReqs, update, updateInterval));
        }
        return templates;
    }

    // ── Slot parsing (slot / slots) ────────────────────────────────────────────

    private static List<Integer> parseSlots(
            final ConfigurationNode node,
            final MenuType type,
            final String fileName,
            final String itemName
    ) throws InvalidMenuException {
        final ConfigurationNode slotNode = node.node("slot");
        final ConfigurationNode slotsNode = node.node("slots");

        if (!slotNode.virtual()) {
            final int slot = slotNode.getInt(-1);
            if (slot < 0 || slot >= type.size()) {
                throw new InvalidMenuException(
                    fileName, nodeLine(slotNode),
                    "items." + itemName + ".slot",
                    "Slot " + slot + " out of bounds for " + type
                        + " (valid: 0-" + (type.size() - 1) + ")");
            }
            return List.of(slot);
        }

        if (!slotsNode.virtual()) {
            final List<? extends ConfigurationNode> children = slotsNode.childrenList();
            if (children.isEmpty()) {
                throw new InvalidMenuException(
                    fileName, nodeLine(slotsNode),
                    "items." + itemName + ".slots",
                    "slots list is empty");
            }
            final List<Integer> result = new ArrayList<>(children.size());
            for (final ConfigurationNode child : children) {
                final int s = child.getInt(-1);
                if (s < 0 || s >= type.size()) {
                    throw new InvalidMenuException(
                        fileName, nodeLine(child),
                        "items." + itemName + ".slots",
                        "Slot " + s + " out of bounds for " + type
                            + " (valid: 0-" + (type.size() - 1) + ")");
                }
                result.add(s);
            }
            return List.copyOf(result);
        }

        throw new InvalidMenuException(
            fileName, "items." + itemName,
            "Item '" + itemName + "' must define either 'slot' or 'slots'");
    }

    private static int parsePriority(
            final ConfigurationNode node,
            final String fileName,
            final String itemName
    ) throws InvalidMenuException {
        final ConfigurationNode prioNode = node.node("priority");
        if (prioNode.virtual()) {
            return 0;
        }
        final int priority = prioNode.getInt(0);
        if (priority < 0) {
            throw new InvalidMenuException(
                fileName, nodeLine(prioNode),
                "items." + itemName + ".priority",
                "priority must be >= 0, got: " + priority);
        }
        return priority;
    }

    // ── ItemStack parsing ──────────────────────────────────────────────────────

    private static ItemStackSnapshot parseItemStack(
            final ConfigurationNode node,
            final String fileName,
            final String itemName
    ) throws InvalidMenuException {
        final NamespacedKey materialKey = parseMaterial(node, fileName, itemName);

        final int amount = Math.max(1, node.node("amount").getInt(1));

        final Component displayName = parseDisplayName(node, fileName, itemName);

        final List<Component> lore = parseLore(node, fileName, itemName);

        final Map<Enchantment, Integer> enchantments =
            parseEnchantments(node, fileName, itemName);

        final Set<ItemFlag> itemFlags = parseItemFlags(node, fileName, itemName);

        final @Nullable String nbt = parseOptionalString(node, "nbt");

        final int customModelData = node.node("custom_model_data").getInt(0);

        final int durability = node.node("durability").getInt(0);

        final @Nullable String skullTexture = parseOptionalString(node, "skull_texture");

        return new ItemStackSnapshot(
            materialKey, amount, displayName, lore,
            enchantments, itemFlags, nbt, customModelData,
            durability, skullTexture);
    }

    private static NamespacedKey parseMaterial(
            final ConfigurationNode node,
            final String fileName,
            final String itemName
    ) throws InvalidMenuException {
        final ConfigurationNode matNode = node.node("material");
        if (matNode.virtual()) {
            throw new InvalidMenuException(
                fileName, "items." + itemName + ".material",
                "material is required");
        }
        final String raw = matNode.getString("");
        if (raw.isBlank()) {
            throw new InvalidMenuException(
                fileName, nodeLine(matNode),
                "items." + itemName + ".material",
                "material name must not be blank");
        }
        Material material = Material.matchMaterial(raw);
        if (material == null) {
            material = Material.getMaterial(raw.toUpperCase());
        }
        if (material == null) {
            throw new InvalidMenuException(
                fileName, nodeLine(matNode),
                "items." + itemName + ".material",
                "Unknown material '" + raw + "'");
        }
        return material.getKey();
    }

    private static Component parseDisplayName(
            final ConfigurationNode node,
            final String fileName,
            final String itemName
    ) throws InvalidMenuException {
        final ConfigurationNode nameNode = node.node("display_name");
        if (nameNode.virtual()) {
            return Component.empty();
        }
        final String raw = nameNode.getString();
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        try {
            return TextUtil.parseMiniMessage(raw);
        } catch (final IllegalArgumentException e) {
            throw new InvalidMenuException(
                fileName, nodeLine(nameNode),
                "items." + itemName + ".display_name",
                "Failed to parse display name: " + e.getMessage());
        }
    }

    private static List<Component> parseLore(
            final ConfigurationNode node,
            final String fileName,
            final String itemName
    ) throws InvalidMenuException {
        final ConfigurationNode loreNode = node.node("lore");
        if (loreNode.virtual() || !loreNode.isList()) {
            return List.of();
        }
        final List<? extends ConfigurationNode> lines = loreNode.childrenList();
        final List<Component> result = new ArrayList<>(lines.size());
        for (final ConfigurationNode line : lines) {
            final String raw = line.getString();
            if (raw == null) {
                result.add(Component.empty());
            } else {
                try {
                    result.add(TextUtil.parseMiniMessage(raw));
                } catch (final IllegalArgumentException e) {
                    throw new InvalidMenuException(
                        fileName, nodeLine(line),
                        "items." + itemName + ".lore",
                        "Failed to parse lore line: " + e.getMessage());
                }
            }
        }
        return List.copyOf(result);
    }

    private static Map<Enchantment, Integer> parseEnchantments(
            final ConfigurationNode node,
            final String fileName,
            final String itemName
    ) throws InvalidMenuException {
        final ConfigurationNode enchNode = node.node("enchantments");
        if (enchNode.virtual() || !enchNode.isList()) {
            return Map.of();
        }
        final Map<Enchantment, Integer> result = new HashMap<>();
        for (final ConfigurationNode entry : enchNode.childrenList()) {
            if (!entry.isMap()) {
                continue;
            }
            for (final Map.Entry<Object, ? extends ConfigurationNode> kv
                    : entry.childrenMap().entrySet()) {
                final String enchName = kv.getKey().toString();
                final int level = kv.getValue().getInt(1);
                final Enchantment ench = Enchantment.getByKey(
                    NamespacedKey.fromString(enchName.toLowerCase()));
                if (ench == null) {
                    throw new InvalidMenuException(
                        fileName, nodeLine(kv.getValue()),
                        "items." + itemName + ".enchantments",
                        "Unknown enchantment '" + enchName + "'");
                }
                result.put(ench, Math.max(1, level));
            }
        }
        return Map.copyOf(result);
    }

    private static Set<ItemFlag> parseItemFlags(
            final ConfigurationNode node,
            final String fileName,
            final String itemName
    ) throws InvalidMenuException {
        final ConfigurationNode flagNode = node.node("item_flags");
        if (flagNode.virtual() || !flagNode.isList()) {
            return Set.of();
        }
        final Set<ItemFlag> result = new HashSet<>();
        for (final ConfigurationNode child : flagNode.childrenList()) {
            final String raw = child.getString();
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                result.add(ItemFlag.valueOf(raw.toUpperCase()));
            } catch (final IllegalArgumentException e) {
                throw new InvalidMenuException(
                    fileName, nodeLine(child),
                    "items." + itemName + ".item_flags",
                    "Unknown item flag '" + raw + "'");
            }
        }
        return Set.copyOf(result);
    }

    // ── Requirement / Action stubs ─────────────────────────────────────────────

    private static @Nullable ViewRequirement parseViewRequirement(
            final ConfigurationNode node,
            final String fileName,
            final String itemName
    ) {
        final ConfigurationNode reqNode = node.node("view_requirement");
        if (reqNode.virtual()) {
            return null;
        }
        // Stub — always visible. Full parsing belongs to a later task.
        return ctx -> true;
    }

    private List<MenuAction> parseActions(
            final ConfigurationNode node,
            final String fileName,
            final String itemName
    ) throws InvalidMenuException {
        if (actionParser == null) {
            return List.of();
        }
        final ConfigurationNode actionsNode = node.node("actions");
        if (actionsNode.virtual() || !actionsNode.isList()) {
            return List.of();
        }
        final List<? extends ConfigurationNode> children = actionsNode.childrenList();
        final List<MenuAction> result = new ArrayList<>(children.size());
        for (final ConfigurationNode child : children) {
            final String raw = child.getString();
            if (raw == null || raw.isBlank()) {
                continue;
            }
            final MenuAction action = actionParser.parse(raw);
            if (action != null) {
                result.add(action);
            }
        }
        return List.copyOf(result);
    }

    private static List<Requirement> parseClickRequirements(
            final ConfigurationNode node,
            final String fileName,
            final String itemName
    ) {
        final ConfigurationNode reqNode = node.node("click_requirements");
        if (reqNode.virtual()) {
            return List.of();
        }
        // Stub — full parsing belongs to a later task.
        // Return a single requirement that always passes.
        return List.of(ctx -> true);
    }

    // ── Filler item ────────────────────────────────────────────────────────────

    private static @Nullable ItemStackSnapshot parseFillerItem(
            final ConfigurationNode root,
            final String fileName
    ) throws InvalidMenuException {
        final ConfigurationNode fillerNode = root.node("filler_item");
        if (fillerNode.virtual()) {
            return null;
        }
        // filler_item follows the same structure as a regular item without slot/slots
        return parseItemStack(fillerNode, fileName, "filler_item");
    }

    // ── Circular link detection ────────────────────────────────────────────────

    private static void detectCircularLinks(
            final Map<String, MenuTemplate> templates
    ) throws InvalidMenuException {
        final Set<String> visited = new HashSet<>();
        final Set<String> inStack = new HashSet<>();

        for (final String id : templates.keySet()) {
            if (!visited.contains(id)) {
                detectCycle(id, templates, visited, inStack, new ArrayList<>());
            }
        }
    }

    private static void detectCycle(
            final String currentId,
            final Map<String, MenuTemplate> templates,
            final Set<String> visited,
            final Set<String> inStack,
            final List<String> path
    ) throws InvalidMenuException {
        visited.add(currentId);
        inStack.add(currentId);
        path.add(currentId);

        final MenuTemplate template = templates.get(currentId);
        final @Nullable String parentId = template.parentMenuId();

        if (parentId != null && templates.containsKey(parentId)) {
            if (inStack.contains(parentId)) {
                // Found a cycle — build a readable chain
                final int cycleStart = path.indexOf(parentId);
                final List<String> cycle = path.subList(cycleStart, path.size());
                cycle.add(parentId);
                throw new InvalidMenuException(
                    currentId, "parent_menu",
                    "Circular menu link detected: " + String.join(" -> ", cycle));
            }
            if (!visited.contains(parentId)) {
                detectCycle(parentId, templates, visited, inStack, path);
            }
        }

        path.remove(path.size() - 1);
        inStack.remove(currentId);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static List<Path> discoverYamlFiles(final Path directory) {
        final List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return files;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                directory, entry -> {
                    final String name = entry.getFileName().toString().toLowerCase();
                    return name.endsWith(".yml") || name.endsWith(".yaml");
                })) {
            for (final Path entry : stream) {
                files.add(entry);
            }
        } catch (final IOException e) {
            throw new RuntimeException(
                "Failed to list YAML files in " + directory.toAbsolutePath(), e);
        }
        return files;
    }

    private static String fileStem(final Path path) {
        final String name = fileName(path);
        final int dot = name.lastIndexOf('.');
        return (dot == -1) ? name : name.substring(0, dot);
    }

    private static String fileName(final Path path) {
        final java.nio.file.Path namePath = path.getFileName();
        if (namePath == null) {
            return path.toString();
        }
        return namePath.toString();
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Attempts to extract a line number from a configuration node.
     *
     * @param node the configuration node
     * @return the line number, or {@code null} if unavailable
     */
    private static @Nullable Integer nodeLine(final ConfigurationNode node) {
        // Configurate 4.x stores line numbers internally but does not expose
        // them through a stable public API. This helper returns null when the
        // line number is unavailable; callers can still identify the error
        // location via the node path.
        return null;
    }
}
