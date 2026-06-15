package com.cebonk03.packetmenu.core.domain;

import com.cebonk03.packetmenu.core.port.PlayerHandle;

/**
 * Data carrier carrying information about a click interaction.
 *
 * @param session   the active menu session
 * @param slot      the slot that was clicked
 * @param clickType the type of click performed
 * @param viewer    the player who clicked
 */
public record ClickContext(
    MenuSession session,
    SlotItem slot,
    ClickType clickType,
    PlayerHandle viewer
) {
}
