package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import java.util.Collections;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * An action that opens another menu by its identifier.
 *
 * <p>The menu lookup and opening is performed by the action runner; this action
 * merely packages the target menu ID and optional arguments. Optional arguments
 * are passed as a string-keyed map for template variable substitution.
 */
public final class OpenGuiAction implements MenuAction {

    private final String menuId;
    private final @Nullable Map<String, String> args;

    /**
     * Creates a new open-gui action without arguments.
     *
     * @param menuId the target menu identifier
     */
    public OpenGuiAction(final String menuId) {
        this(menuId, null);
    }

    /**
     * Creates a new open-gui action with optional arguments.
     *
     * @param menuId the target menu identifier
     * @param args   optional argument map for template substitution, or {@code null}
     */
    public OpenGuiAction(final String menuId, final @Nullable Map<String, String> args) {
        this.menuId = menuId;
        this.args = args != null ? Map.copyOf(args) : null;
    }

    /**
     * Returns the target menu identifier.
     *
     * @return menu id
     */
    public String menuId() {
        return menuId;
    }

    /**
     * Returns an immutable view of the optional arguments, or an empty map if none
     * were provided.
     *
     * @return argument map, never {@code null}
     */
    public Map<String, String> args() {
        return args != null ? args : Collections.emptyMap();
    }

    @Override
    public ActionResult execute(final ActionContext context) {
        return new ActionResult.Success();
    }
}
