package com.cebonk03.packetmenu.core.domain;

import com.cebonk03.packetmenu.core.port.PlayerHandle;
import org.jspecify.annotations.Nullable;

/**
 * Data carrier for evaluating a {@link Requirement} or {@link ViewRequirement}.
 *
 * @param viewer  the player
 * @param session the active menu session
 * @param slot    the slot being evaluated, if applicable
 */
public record RequirementContext(
    PlayerHandle viewer,
    MenuSession session,
    @Nullable SlotItem slot
) {
}
