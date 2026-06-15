package com.cebonk03.packetmenu.core.domain;

/**
 * Handles a click interaction within a menu slot.
 */
@FunctionalInterface
public interface ClickHandler {

    /**
     * Invoked when a player clicks on the slot associated with this handler.
     *
     * @param context the click context carrying session, slot, click type, and viewer
     */
    void handle(ClickContext context);
}
