package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;

/**
 * An action that signals the current menu should be closed.
 *
 * <p>The actual close packet is sent by the action runner; this action merely
 * returns a {@link ActionResult.Success} to indicate that the close flow
 * should be triggered.
 */
public final class CloseAction implements MenuAction {

    @Override
    public ActionResult execute(final ActionContext context) {
        return new ActionResult.Success();
    }
}
