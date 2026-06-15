package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.port.EconomyPort;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import org.jspecify.annotations.Nullable;

/**
 * An action that withdraws a specified amount from the player's balance via
 * {@link EconomyPort}.
 *
 * <p>If the economy port is unavailable (e.g. Vault not installed) or the player
 * has insufficient funds, a {@link ActionResult.Failure} is returned. Only the
 * balance check is performed synchronously; the actual withdrawal is routed
 * through the player's scheduler for thread safety.
 */
public final class TakeMoneyAction implements MenuAction {

    private final @Nullable EconomyPort economy;
    private final double amount;
    private final SchedulerPort scheduler;

    /**
     * Creates a new take-money action.
     *
     * @param economy   the economy port, or {@code null} if Vault is absent
     * @param amount    the amount to withdraw
     * @param scheduler the scheduler port for thread-safe execution
     */
    public TakeMoneyAction(
        final @Nullable EconomyPort economy,
        final double amount,
        final SchedulerPort scheduler
    ) {
        this.economy = economy;
        this.amount = amount;
        this.scheduler = scheduler;
    }

    @Override
    public ActionResult execute(final ActionContext context) {
        if (economy == null) {
            return new ActionResult.Failure("Economy is not available");
        }
        if (!economy.has(context.viewer(), amount)) {
            return new ActionResult.Failure("Insufficient funds");
        }
        scheduler.runOnPlayer(context.viewer(), () ->
            economy.withdraw(context.viewer(), amount)
        );
        return new ActionResult.Success();
    }
}
