package com.cebonk03.packetmenu.core.domain;

/**
 * An action that can be executed within a menu, returning an {@link ActionResult}.
 */
@FunctionalInterface
public interface MenuAction {

    /**
     * Execute this action.
     *
     * @param context the action context
     * @return the result of execution
     */
    ActionResult execute(ActionContext context);
}
