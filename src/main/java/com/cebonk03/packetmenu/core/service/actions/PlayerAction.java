package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * An action that executes a command as the clicking player.
 *
 * <p>The command is dispatched through {@link Bukkit#dispatchCommand} using the
 * player's native {@link CommandSender} instance. Execution is routed through
 * the player's region scheduler for Folia compatibility.
 */
public final class PlayerAction implements MenuAction {

    private final String command;
    private final SchedulerPort scheduler;

    /**
     * Creates a new player action.
     *
     * @param command   the command string to execute (without leading slash)
     * @param scheduler the scheduler port for thread-safe execution
     */
    public PlayerAction(final String command, final SchedulerPort scheduler) {
        this.command = command;
        this.scheduler = scheduler;
    }

    @Override
    public ActionResult execute(final ActionContext context) {
        scheduler.runOnPlayer(context.viewer(), () ->
            Bukkit.dispatchCommand(
                (CommandSender) context.viewer().nativePlayer(),
                command
            )
        );
        return new ActionResult.Success();
    }
}
