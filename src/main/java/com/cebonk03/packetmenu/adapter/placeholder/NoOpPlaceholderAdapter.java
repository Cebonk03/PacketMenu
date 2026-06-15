package com.cebonk03.packetmenu.adapter.placeholder;

import com.cebonk03.packetmenu.core.port.PlaceholderPort;
import com.cebonk03.packetmenu.core.port.PlayerHandle;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

/**
 * No-op implementation of {@link PlaceholderPort} used when PlaceholderAPI is
 * not installed on the server.
 *
 * <p>All methods return their input unchanged — no placeholder expansion is
 * performed.
 */
@NullMarked
public final class NoOpPlaceholderAdapter implements PlaceholderPort {

    @Override
    public Component resolve(final Component component, final PlayerHandle player) {
        return component;
    }

    @Override
    public String resolveString(final String raw, final PlayerHandle player) {
        return raw;
    }
}
