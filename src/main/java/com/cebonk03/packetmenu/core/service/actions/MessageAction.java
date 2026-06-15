package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import net.kyori.adventure.text.Component;

/**
 * An action that sends a {@link Component} message to the viewer.
 *
 * <p>The message is sent via {@link com.cebonk03.packetmenu.core.port.PlayerHandle#sendMessage(Component)},
 * which dispatches it to the player's chat or action bar depending on the server
 * configuration.
 */
public final class MessageAction implements MenuAction {

    private final Component message;

    /**
     * Creates a new message action.
     *
     * @param message the component to send to the player
     */
    public MessageAction(final Component message) {
        this.message = message;
    }

    @Override
    public ActionResult execute(final ActionContext context) {
        context.viewer().sendMessage(message);
        return new ActionResult.Success();
    }
}
