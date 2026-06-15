package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import org.bukkit.Bukkit;

/**
 * An action that executes a command as the server console.
 *
 * <p>The command is dispatched through {@link Bukkit#dispatchCommand} with the
 * console sender, ensuring it runs with full operator privileges. Execution is
 * routed through the global server scheduler to guarantee thread safety.
 */
public final class ConsoleAction implements MenuAction {

    private final String command;
    private final SchedulerPort scheduler;

    /**
     * Creates a new console action.
     *
     * @param command   the command string to execute (without leading slash)
     * @param scheduler the scheduler port for thread-safe execution
     */
    public ConsoleAction(final String command, final SchedulerPort scheduler) {
        this.command = command;
        this.scheduler = scheduler;
    }

    @Override
    public ActionResult execute(final ActionContext context) {
        scheduler.runOnGlobal(() ->
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
        );
        return new ActionResult.Success();
    }
}
