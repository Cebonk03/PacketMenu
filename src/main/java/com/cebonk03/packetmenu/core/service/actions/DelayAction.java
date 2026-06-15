package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;

/**
 * An action that wraps an inner action with a tick delay.
 *
 * <p>When executed, this action returns an {@link ActionResult.Delayed} result
 * that causes the action runner to re-execute the inner action after the
 * specified number of ticks.
 *
 * @param ticks the delay in server ticks
 * @param next  the action to execute after the delay
 */
public record DelayAction(long ticks, MenuAction next) implements MenuAction {

    /**
     * Compact constructor — validates the tick count.
     */
    public DelayAction {
        if (ticks < 0) {
            throw new IllegalArgumentException("ticks must be non-negative, got: " + ticks);
        }
    }

    @Override
    public ActionResult execute(final ActionContext context) {
        return new ActionResult.Delayed(ticks, next);
    }
}
