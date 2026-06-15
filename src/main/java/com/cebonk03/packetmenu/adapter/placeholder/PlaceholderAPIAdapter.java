package com.cebonk03.packetmenu.adapter.placeholder;

import com.cebonk03.packetmenu.core.port.PlaceholderPort;
import com.cebonk03.packetmenu.core.port.PlayerHandle;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * {@link PlaceholderPort} implementation backed by PlaceholderAPI.
 *
 * <p>Detects the presence of PlaceholderAPI at runtime via
 * {@link Bukkit#getPluginManager()}. If the plugin is available,
 * components are serialised to MiniMessage format, expanded through
 * {@link PlaceholderAPI#setPlaceholders(org.bukkit.OfflinePlayer, String)},
 * and deserialised back into a {@link Component}.
 *
 * <p>When PlaceholderAPI is absent (either not installed or not loaded)
 * all methods act as no-ops and return their input unchanged. This ensures
 * the plugin remains functional without a hard dependency.
 */
@NullMarked
public final class PlaceholderAPIAdapter implements PlaceholderPort {

    private static final boolean PLACEHOLDER_API_PRESENT;

    static {
        PLACEHOLDER_API_PRESENT = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    @Override
    public Component resolve(final Component component, final PlayerHandle player) {
        if (!PLACEHOLDER_API_PRESENT) {
            return component;
        }
        final String serialized = MiniMessage.miniMessage().serialize(component);
        final String resolved = PlaceholderAPI.setPlaceholders(
                (Player) player.nativePlayer(),
                serialized
        );
        return MiniMessage.miniMessage().deserialize(resolved);
    }

    @Override
    public String resolveString(final String raw, final PlayerHandle player) {
        if (!PLACEHOLDER_API_PRESENT) {
            return raw;
        }
        return PlaceholderAPI.setPlaceholders(
                (Player) player.nativePlayer(),
                raw
        );
    }
}
