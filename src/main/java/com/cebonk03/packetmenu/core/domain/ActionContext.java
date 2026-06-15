package com.cebonk03.packetmenu.core.domain;

import com.cebonk03.packetmenu.core.port.PlayerHandle;
import org.jspecify.annotations.Nullable;

/**
 * Data carrier for executing a {@link MenuAction}.
 *
 * @param viewer       the player
 * @param session      the active menu session
 * @param slot         the slot involved, if applicable
 * @param clickType    the click that triggered this action
 * @param sourceAction the action that spawned this one, if any (e.g. for chained actions)
 */
public record ActionContext(
    PlayerHandle viewer,
    MenuSession session,
    @Nullable SlotItem slot,
    ClickType clickType,
    @Nullable MenuAction sourceAction
) {
}
