package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;

/**
 * Broadcasts a {@link Component} message to all online players.
 *
 * <p>The broadcast is executed on the global server tick thread to ensure
 * thread safety with the Bukkit player list.
 */
@NullMarked
public final class BroadcastAction implements MenuAction {

    private final Component message;
    private final SchedulerPort scheduler;

    /**
     * Creates a new broadcast action.
     *
     * @param message   the message to broadcast
     * @param scheduler the scheduler on which to run the broadcast
     */
    public BroadcastAction(final Component message, final SchedulerPort scheduler) {
        this.message = message;
        this.scheduler = scheduler;
    }

    @Override
    public ActionResult execute(final ActionContext context) {
        scheduler.runOnGlobal(() -> Bukkit.broadcast(message));
        return new ActionResult.Success();
    }
}
