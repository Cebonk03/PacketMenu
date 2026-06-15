package com.cebonk03.packetmenu.core.port;

import net.kyori.adventure.text.Component;

/**
 * Port for resolving placeholders in text components and strings.
 *
 * <p>Implementations delegate to a placeholder expansion library (e.g.
 * PlaceholderAPI) and return the fully resolved result for the given player.
 */
public interface PlaceholderPort {

    /**
     * Resolves placeholders found in the given Adventure {@link Component}.
     *
     * @param component the component potentially containing placeholders
     * @param player    the player context for resolution
     * @return the resolved component with placeholders replaced
     */
    Component resolve(Component component, PlayerHandle player);

    /**
     * Resolves placeholders in a raw string.
     *
     * @param raw    the raw string potentially containing placeholders
     * @param player the player context for resolution
     * @return the resolved string with placeholders replaced
     */
    String resolveString(String raw, PlayerHandle player);
}
