package com.cebonk03.packetmenu.core.port;

import net.kyori.adventure.text.Component;
import java.util.UUID;

/**
 * Abstraction over a Minecraft player, hiding the underlying server platform.
 *
 * <p>Implementations wrap the native player object (Bukkit {@code Player}) and
 * expose only the capabilities that PacketMenu requires, keeping the core
 * free of server-specific imports.
 */
public interface PlayerHandle {

    /**
     * Returns the native platform player object.
     *
     * @return the underlying player instance, typed as {@link Object}
     */
    Object nativePlayer();

    /**
     * Sends a chat or action-bar message to this player.
     *
     * @param message the message component
     */
    void sendMessage(Component message);

    /**
     * Checks whether this player has the given permission node.
     *
     * @param permission the permission node to check
     * @return {@code true} if the player has the permission
     */
    boolean hasPermission(String permission);

    /**
     * Returns the unique identifier of this player.
     *
     * @return the player's {@link UUID}
     */
    UUID getUniqueId();

    /**
     * Returns the current display name of this player.
     *
     * @return the player name
     */
    String getName();
}
