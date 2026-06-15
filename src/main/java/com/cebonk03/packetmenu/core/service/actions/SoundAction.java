package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import org.bukkit.entity.Player;

/**
 * An action that plays a sound at the player's location.
 *
 * <p>The sound is identified by its Minecraft namespaced key (e.g.
 * {@code minecraft:entity.experience_orb.pickup}). Execution is routed through
 * the player's region scheduler to ensure thread safety.
 */
public final class SoundAction implements MenuAction {

    private final String soundKey;
    private final float volume;
    private final float pitch;
    private final SchedulerPort scheduler;

    /**
     * Creates a new sound action.
     *
     * @param soundKey  the namespaced sound key (e.g. {@code "entity.experience_orb.pickup"})
     * @param volume    the volume (1.0 is normal)
     * @param pitch     the pitch (1.0 is normal)
     * @param scheduler the scheduler port for thread-safe execution
     */
    public SoundAction(
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
        scheduler.runOnPlayer(context.viewer(), () -> {
            final var player = (Player) context.viewer().nativePlayer();
            player.playSound(player.getLocation(), soundKey, volume, pitch);
        });
        return new ActionResult.Success();
    }
}
