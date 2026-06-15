package com.cebonk03.packetmenu.core.service;

import com.cebonk03.packetmenu.core.domain.MenuTemplate;
import com.cebonk03.packetmenu.core.port.MenuLoader;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

/**
 * Thread-safe registry for {@link MenuTemplate} instances.
 *
 * <p>Provides concurrent access to a shared map of named templates. Templates can be
 * registered, unregistered, looked up, and bulk-reloaded from disk without affecting
 * active {@link com.cebonk03.packetmenu.core.domain.MenuSession}s&mdash;sessions hold
 * their own immutable state and reference templates only by identifier.
 *
 * <p>All public methods are thread-safe.
 */
@NullMarked
public final class MenuRegistry {

    private final ConcurrentHashMap<String, MenuTemplate> templates;
    private final MenuLoader menuLoader;
    private final Path menusDirectory;

    /**
     * Creates a new registry backed by the given loader.
     *
     * @param menuLoader     the loader used by {@link #reloadAll()}
     * @param menusDirectory the directory scanned by {@link #reloadAll()}
     */
    public MenuRegistry(final MenuLoader menuLoader, final Path menusDirectory) {
        this.templates = new ConcurrentHashMap<>();
        this.menuLoader = menuLoader;
        this.menusDirectory = menusDirectory;
    }

    /**
     * Registers a menu template.
     *
     * <p>If a template with the same identifier already exists, it is replaced.
     *
     * @param template the template to register
     */
    public void register(final MenuTemplate template) {
        templates.put(template.id(), template);
    }

    /**
     * Removes a menu template from the registry.
     *
     * @param id the identifier of the template to remove
     */
    public void unregister(final String id) {
        templates.remove(id);
    }

    /**
     * Looks up a template by its identifier.
     *
     * @param id the template identifier
     * @return an {@link Optional} containing the template, or empty if not found
     */
    public Optional<MenuTemplate> get(final String id) {
        return Optional.ofNullable(templates.get(id));
    }

    /**
     * Returns an unmodifiable snapshot of all currently registered templates.
     *
     * @return a map of template identifiers to templates
     */
    public Map<String, MenuTemplate> getAll() {
        return Map.copyOf(templates);
    }

    /**
     * Reloads all menu templates from disk via the configured {@link MenuLoader}.
     *
     * <p>The internal template map is atomically replaced with the newly loaded
     * templates. Active {@link com.cebonk03.packetmenu.core.domain.MenuSession}s are
     * <strong>not</strong> invalidated because they hold their own immutable state
     * and reference templates only by identifier string.
     *
     * @return a map of the newly loaded templates (for comparison with previous state)
     */
    public Map<String, MenuTemplate> reloadAll() {
        final Map<String, MenuTemplate> loaded = menuLoader.loadAll(menusDirectory);
        templates.clear();
        templates.putAll(loaded);
        return Map.copyOf(loaded);
    }
}
