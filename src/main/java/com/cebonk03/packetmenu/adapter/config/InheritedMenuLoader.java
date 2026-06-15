package com.cebonk03.packetmenu.adapter.config;

import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import com.cebonk03.packetmenu.core.domain.MenuTemplate;
import com.cebonk03.packetmenu.core.domain.Requirement;
import com.cebonk03.packetmenu.core.domain.SlotTemplate;
import com.cebonk03.packetmenu.core.port.MenuLoader;
import com.cebonk03.packetmenu.core.port.MenuRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/**
 * A {@link MenuLoader} decorator that adds template inheritance via the
 * {@code extends} field in YAML menu definitions.
 *
 * <p>When a menu definition contains an {@code extends} key pointing to
 * another menu template's identifier, the loader merges the parent template's
 * fields into the child's. The child's own values take priority for direct
 * fields, while slots are merged by {@code (slot, priority)} pairs &mdash;
 * the child wins when both sides have the same pair.
 *
 * <p>Inheritance is resolved recursively up to {@link #MAX_DEPTH} levels.
 * Cycle detection prevents infinite loops.
 *
 * <p>Merge rules:
 * <ul>
 *   <li>Parent template serves as the base</li>
 *   <li>Child's {@code title}, {@code type}, {@code openCommands},
 *       {@code fillerItem}, {@code updateInterval}, and
 *       {@code closeOnClickOutside} override the parent's</li>
 *   <li>Child's {@code openRequirement} overrides the parent's</li>
 *   <li>Slots are merged by {@code (slot, priority)} pair &mdash;
 *       same-pair child wins</li>
 * </ul>
 */
@NullMarked
public final class InheritedMenuLoader implements MenuLoader {

    /** Maximum depth for recursive extends resolution. */
    static final int MAX_DEPTH = 10;

    private final MenuLoader baseLoader;
    private final MenuRegistry registry;

    /**
     * Creates an {@code InheritedMenuLoader}.
     *
     * @param baseLoader the underlying loader used to parse individual menu
     *                   YAML files (typically a {@code ConfigurateMenuLoader})
     * @param registry   the registry in which parent templates are looked up
     */
    public InheritedMenuLoader(
            final MenuLoader baseLoader,
            final MenuRegistry registry
    ) {
        this.baseLoader = baseLoader;
        this.registry = registry;
    }

    /**
     * Raw metadata extracted from a menu YAML file before full parsing.
     *
     * @param id        the menu's unique identifier
     * @param extendsId the optional parent menu identifier, or {@code null}
     */
    private record RawMenuDef(String id, @Nullable String extendsId) {
    }

    // ── MenuLoader implementation ─────────────────────────────────────────

    @Override
    public MenuTemplate load(final Path path) {
        // Phase 1: read the raw extends field from the YAML
        final RawMenuDef raw = readRawMenuDef(path);
        if (raw == null) {
            throw new RuntimeException(
                "Failed to read menu definition: " + path.toAbsolutePath());
        }

        // Phase 2: parse the full template via the base loader
        final MenuTemplate child = baseLoader.load(path);

        // Phase 3: resolve inheritance if an extends field was present
        if (raw.extendsId() == null) {
            return child;
        }

        try {
            final Set<String> chain = new LinkedHashSet<>();
            chain.add(child.id());
            return resolveInheritance(child, raw.extendsId(), chain, 0);
        } catch (final InvalidMenuException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, MenuTemplate> loadAll(final Path directory) {
        final List<Path> files = discoverYamlFiles(directory);

        // Phase 1: read raw id + extends from every YAML file
        final Map<String, RawMenuDef> rawDefs = new LinkedHashMap<>();
        for (final Path file : files) {
            final RawMenuDef def = readRawMenuDef(file);
            if (def != null) {
                rawDefs.put(def.id(), def);
            }
        }

        // Phase 2: parse all templates via the base loader
        final Map<String, MenuTemplate> templates = baseLoader.loadAll(directory);

        // Phase 3: resolve inheritance for each template
        final Map<String, MenuTemplate> resolved = new LinkedHashMap<>(templates.size());
        for (final Map.Entry<String, MenuTemplate> entry : templates.entrySet()) {
            final MenuTemplate template = entry.getValue();
            final RawMenuDef def = rawDefs.get(template.id());
            if (def != null && def.extendsId() != null) {
                try {
                    final Set<String> chain = new LinkedHashSet<>();
                    chain.add(template.id());
                    resolved.put(
                        entry.getKey(),
                        resolveInheritance(template, def.extendsId(), chain, 0));
                } catch (final InvalidMenuException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            } else {
                resolved.put(entry.getKey(), template);
            }
        }

        return Map.copyOf(resolved);
    }

    // ── Inheritance resolution ────────────────────────────────────────────

    /**
     * Resolves the full inheritance chain for a child template.
     *
     * @param child    the template whose inheritance to resolve
     * @param parentId the identifier of the immediate parent
     * @param chain    the set of template ids already visited in this chain
     * @param depth    the current resolution depth
     * @return a fully-resolved {@link MenuTemplate}
     * @throws InvalidMenuException if the chain contains a cycle, a parent is
     *                              missing, or the depth limit is exceeded
     */
    private MenuTemplate resolveInheritance(
            final MenuTemplate child,
            final String parentId,
            final Set<String> chain,
            final int depth
    ) throws InvalidMenuException {
        // Depth limit
        if (depth >= MAX_DEPTH) {
            throw new InvalidMenuException(
                "Max inheritance depth (" + MAX_DEPTH + ") exceeded for menu '"
                    + child.id() + "'. Chain: " + chain);
        }

        // Cycle detection
        if (!chain.add(parentId)) {
            throw new InvalidMenuException(
                "Circular inheritance detected for menu '" + child.id()
                    + "'. Parent '" + parentId + "' already in chain: " + chain);
        }

        // Look up parent in registry
        final MenuTemplate parent = registry.getTemplate(parentId);
        if (parent == null) {
            throw new InvalidMenuException(
                "Parent template '" + parentId + "' not found for menu '"
                    + child.id() + "'. Ensure the parent template is loaded "
                    + "and registered before the child.");
        }

        // Merge resolved parent into child
        return merge(parent, child);
    }

    // ── Merge strategy ────────────────────────────────────────────────────

    /**
     * Merges a parent template into a child template.
     *
     * <p>The parent serves as the base. The child's scalar fields override
     * the parent's. Slots are merged by {@code (slot, priority)} pair with
     * the child winning ties.
     *
     * @param parent the resolved parent template
     * @param child  the child template whose values take priority
     * @return a new {@link MenuTemplate} representing the merged result
     */
    private static MenuTemplate merge(
            final MenuTemplate parent,
            final MenuTemplate child
    ) {
        final @Nullable Requirement openRequirement = child.openRequirement() != null
            ? child.openRequirement()
            : parent.openRequirement();
        final @Nullable ItemStackSnapshot fillerItem = child.fillerItem() != null
            ? child.fillerItem()
            : parent.fillerItem();
        final int updateInterval = child.updateInterval() != 0
            ? child.updateInterval()
            : parent.updateInterval();

        final List<SlotTemplate> mergedSlots =
            mergeSlots(parent.slotTemplates(), child.slotTemplates());

        return new MenuTemplate(
            child.id(),
            child.title(),
            child.type(),
            child.openCommands(),
            openRequirement,
            mergedSlots,
            fillerItem,
            updateInterval,
            child.closeOnClickOutside(),
            child.parentMenuId()
        );
    }

    /**
     * Merges parent and child slot lists.
     *
     * <p>Parent slots form the base. Child slots with the same
     * {@code (slot, priority)} pair override the parent's. The result is
     * sorted by priority descending (highest first).
     *
     * @param parentSlots the parent's slot templates
     * @param childSlots  the child's slot templates
     * @return a merged, sorted list of slot templates
     */
    private static List<SlotTemplate> mergeSlots(
            final List<SlotTemplate> parentSlots,
            final List<SlotTemplate> childSlots
    ) {
        // Key: (slot, priority) pair serialized as a long
        final Map<Long, SlotTemplate> merged = new LinkedHashMap<>();

        for (final SlotTemplate slot : parentSlots) {
            merged.put(slotKey(slot.slot(), slot.priority()), slot);
        }
        for (final SlotTemplate slot : childSlots) {
            merged.put(slotKey(slot.slot(), slot.priority()), slot);
        }

        final List<SlotTemplate> result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparingInt(SlotTemplate::priority).reversed());
        return List.copyOf(result);
    }

    /**
     * Creates a composite key from a slot index and priority.
     *
     * @param slot     the inventory slot index
     * @param priority the rendering priority
     * @return a long combining both values
     */
    private static long slotKey(final int slot, final int priority) {
        return ((long) slot << 32) | (priority & 0xFFFFFFFFL);
    }

    // ── Raw YAML reading ──────────────────────────────────────────────────

    /**
     * Reads the {@code id} and optional {@code extends} field from a YAML
     * menu definition file without performing full template parsing.
     *
     * @param path the YAML file path
     * @return a {@link RawMenuDef} with the extracted values, or
     *         {@code null} if the file cannot be read
     */
    private static @Nullable RawMenuDef readRawMenuDef(final Path path) {
        try {
            final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .file(path.toFile())
                .build();
            final ConfigurationNode root = loader.load();
            final String id = root.node("id").getString();
            if (id == null || id.isBlank()) {
                return null;
            }
            String extendsId = root.node("extends").getString();
            if (extendsId != null && extendsId.isBlank()) {
                extendsId = null;
            }
            return new RawMenuDef(id.trim(), extendsId);
        } catch (final IOException e) {
            return null;
        }
    }

    /**
     * Discovers all {@code .yml} and {@code .yaml} files in a directory.
     *
     * @param directory the directory to scan
     * @return a sorted list of YAML file paths
     */
    private static List<Path> discoverYamlFiles(final Path directory) {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> {
                    final String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".yml") || name.endsWith(".yaml");
                })
                .sorted()
                .toList();
        } catch (final IOException e) {
            return List.of();
        }
    }
}
