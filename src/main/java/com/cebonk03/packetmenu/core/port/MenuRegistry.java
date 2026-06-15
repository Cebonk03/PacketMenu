package com.cebonk03.packetmenu.core.port;

import com.cebonk03.packetmenu.core.domain.MenuTemplate;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Registry for storing and retrieving loaded {@link MenuTemplate} instances.
 *
 * <p>Used by the inheritance resolver and the menu service to look up
 * templates by their unique identifier.
 */
@NullMarked
public interface MenuRegistry {

    /**
     * Retrieves a registered menu template by its identifier.
     *
     * @param id the menu template identifier
     * @return the matching template, or {@code null} if not found
     */
    @Nullable MenuTemplate getTemplate(String id);

    /**
     * Registers a menu template for future lookups.
     *
     * @param template the template to register
     * @throws IllegalArgumentException if a template with the same id is already registered
     */
    void register(MenuTemplate template);

    /**
     * Checks whether a template with the given identifier is registered.
     *
     * @param id the menu template identifier
     * @return {@code true} if a template with this id is registered
     */
    boolean hasTemplate(String id);

    /**
     * Removes all registered templates.
     */
    void clear();
}
