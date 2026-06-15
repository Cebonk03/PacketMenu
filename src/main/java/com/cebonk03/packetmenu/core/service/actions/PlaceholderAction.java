package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.port.PlaceholderPort;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

/**
 * Resolves placeholders in a {@link Component} message for the viewer and
 * sends the resolved message to them.
 *
 * <p>Placeholder resolution is delegated to the injected {@link PlaceholderPort}.
 */
@NullMarked
public final class PlaceholderAction implements MenuAction {

    private final Component message;
    private final PlaceholderPort placeholderPort;

    /**
     * Creates a new placeholder action.
     *
     * @param message         the message containing placeholders to resolve
     * @param placeholderPort the port for resolving placeholders
     */
    public PlaceholderAction(final Component message, final PlaceholderPort placeholderPort) {
        this.message = message;
        this.placeholderPort = placeholderPort;
    }

    @Override
    public ActionResult execute(final ActionContext context) {
        final Component resolved = placeholderPort.resolve(message, context.viewer());
        context.viewer().sendMessage(resolved);
        return new ActionResult.Success();
    }
}
