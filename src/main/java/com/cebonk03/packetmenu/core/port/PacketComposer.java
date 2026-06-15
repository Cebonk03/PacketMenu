package com.cebonk03.packetmenu.core.port;

import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import com.cebonk03.packetmenu.core.domain.MenuSession;

/**
 * Port for composing and sending packet-based inventory windows.
 *
 * <p>Implementations translate high-level menu operations into the underlying
 * packet library (e.g. PacketEvents), keeping the core domain completely
 * unaware of the wire protocol.
 */
public interface PacketComposer {

    /**
     * Opens a new menu window for the given player using the session data.
     *
     * @param player  the target player
     * @param session the menu session describing the window to open
     */
    void openWindow(PlayerHandle player, MenuSession session);

    /**
     * Sends the complete set of items for the menu session to fill the window.
     *
     * @param player  the target player
     * @param session the menu session whose items should be sent
     */
    void sendItems(PlayerHandle player, MenuSession session);

    /**
     * Sets a single slot inside the currently open container window.
     *
     * @param player      the target player
     * @param containerId the container window id
     * @param slot        the slot index within the container
     * @param item        the item stack to place in the slot
     */
    void setSlot(PlayerHandle player, int containerId, int slot, ItemStackSnapshot item);

    /**
     * Closes the specified container window for the player.
     *
     * @param player      the target player
     * @param containerId the container window id to close
     */
    void closeWindow(PlayerHandle player, int containerId);
}
