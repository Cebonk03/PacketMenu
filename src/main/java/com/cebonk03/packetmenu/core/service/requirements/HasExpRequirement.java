package com.cebonk03.packetmenu.core.service.requirements;

import com.cebonk03.packetmenu.core.domain.Requirement;
import com.cebonk03.packetmenu.core.domain.RequirementContext;
import org.bukkit.entity.Player;

/**
 * A requirement that passes when the viewer has at least the specified number
 * of experience levels.
 *
 * <p>Accesses the native Bukkit {@link Player} via
 * {@link com.cebonk03.packetmenu.core.port.PlayerHandle#nativePlayer()}.
 */
public final class HasExpRequirement implements Requirement {

    private final int levels;

    /**
     * Creates a new {@code HasExpRequirement}.
     *
     * @param levels the minimum number of XP levels required
     */
    public HasExpRequirement(final int levels) {
        this.levels = levels;
    }

    @Override
    public boolean test(final RequirementContext context) {
        final Object raw = context.viewer().nativePlayer();
        if (!(raw instanceof Player player)) {
            return false;
        }
        return player.getLevel() >= levels;
    }
}
