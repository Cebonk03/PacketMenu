package com.cebonk03.packetmenu.adapter.paper;

import com.cebonk03.packetmenu.core.port.PlayerHandle;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Paper implementation of {@link PlayerHandle}.
 *
 * <p>Wraps a Bukkit {@link Player} and delegates all operations to the
 * underlying Paper player instance.
 */
@NullMarked
public final class PaperPlayerHandle implements PlayerHandle {

    private final Player player;

    /**
     * Creates a new handle wrapping the given Paper player.
     *
     * @param player the Bukkit/Paper player to wrap
     */
    public PaperPlayerHandle(Player player) {
        this.player = player;
    }

    @Override
    public Object nativePlayer() {
        return player;
    }

    @Override
    public void sendMessage(Component message) {
        player.sendMessage(message);
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getName();
    }
}
