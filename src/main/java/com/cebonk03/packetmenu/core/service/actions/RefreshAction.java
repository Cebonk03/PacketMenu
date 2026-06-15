package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;

/**
 * An action that signals the menu should re-evaluate its view requirements and
 * refresh the displayed slots.
 *
 * <p>The actual refresh logic is handled by the action runner; this action
 * merely returns {@link ActionResult.Success} to indicate that the refresh
 * flow should be triggered.
 */
public final class RefreshAction implements MenuAction {

    @Override
    public ActionResult execute(final ActionContext context) {
        return new ActionResult.Success();
    }
}
