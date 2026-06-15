package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Plays a sound to all players in the same world as the viewer.
 *
 * <p>The sound is played on the global server tick thread via the provided
 * {@link SchedulerPort}.
 */
@NullMarked
public final class BroadcastSoundWorldAction implements MenuAction {

    private final String soundKey;
    private final float volume;
    private final float pitch;
    private final SchedulerPort scheduler;

    /**
     * Creates a new sound broadcast action.
     *
     * @param soundKey  the namespaced sound key (e.g. {@code "entity.experience_orb.pickup"})
     * @param volume    the volume (1.0 is normal)
     * @param pitch     the pitch (1.0 is normal)
     * @param scheduler the scheduler on which to run the playback
     */
    public BroadcastSoundWorldAction(
        final String soundKey,
        final float volume,
        final float pitch,
        final SchedulerPort scheduler
    ) {
        this.soundKey = soundKey;
        this.volume = volume;
        this.pitch = pitch;
        this.scheduler = scheduler;
    }

    @Override
    public ActionResult execute(final ActionContext context) {
        final Player viewer = (Player) context.viewer().nativePlayer();

        scheduler.runOnGlobal(() -> {
            for (final Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(viewer.getWorld())) {
                    player.playSound(player.getLocation(), soundKey, volume, pitch);
                }
            }
        });

        return new ActionResult.Success();
    }
}
