package com.cebonk03.packetmenu.core.service;

import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.ClickContext;
import com.cebonk03.packetmenu.core.domain.ClickType;
import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.cebonk03.packetmenu.core.domain.RequirementContext;
import com.cebonk03.packetmenu.core.domain.SlotItem;
import com.cebonk03.packetmenu.core.port.PlayerHandle;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Interprets a raw click (slot index + {@link ClickType}) against an active
 * {@link MenuSession}, resolves the target {@link SlotItem}, checks visibility
 * requirements, and delegates to the slot's
 * {@link com.cebonk03.packetmenu.core.domain.ClickHandler}.
 *
 * <p>This class is stateless and thread-safe.
 */
@NullMarked
public final class ClickInterpreter {

    /**
     * Processes a click on a menu slot.
     *
     * <p>The following steps are performed in order:
     * <ol>
     *   <li>Find the {@link SlotItem} matching the given slot index.</li>
     *   <li>If the slot has a
     *   {@link com.cebonk03.packetmenu.core.domain.ViewRequirement},
     *       evaluate it and skip the click if the item is not visible.</li>
     *   <li>If the slot has a
     *   {@link com.cebonk03.packetmenu.core.domain.ClickHandler},
     *       invoke it with a new {@link ClickContext}.</li>
     *   <li>Return {@link ActionResult.Success} on normal completion.</li>
     * </ol>
     *
     * <p>If no slot is found at the given index the click is silently ignored.
     *
     * @param viewer   the player who clicked
     * @param session  the active menu session
     * @param slot     the clicked slot index (validated by the caller)
     * @param clickType the type of click performed
     * @return the outcome of processing
     */
    public ActionResult interpret(
            final PlayerHandle viewer,
            final MenuSession session,
            final int slot,
            final ClickType clickType
    ) {
        // Step 1 — resolve the SlotItem for this slot index
        final SlotItem slotItem = findSlot(session.slots(), slot);
        if (slotItem == null) {
            return new ActionResult.Success();
        }

        // Step 2 — check view requirements (skip invisible items)
        if (slotItem.viewRequirement() != null) {
            final RequirementContext reqCtx = new RequirementContext(
                    viewer, session, slotItem);
            if (!slotItem.viewRequirement().test(reqCtx)) {
                return new ActionResult.Success();
            }
        }

        // Step 3 — delegate to the slot's click handler
        if (slotItem.clickHandler() != null) {
            final ClickContext clickCtx = new ClickContext(
                    session, slotItem, clickType, viewer);
            slotItem.clickHandler().handle(clickCtx);
        }

        return new ActionResult.Success();
    }

    /**
     * Linear scan for a slot matching the given index.
     *
     * <p>Menu slot lists are small (typically &le;54), so a simple linear scan
     * is sufficient.  Returns {@code null} when no match is found.
     *
     * @param slots     the session's live slot list
     * @param slotIndex the target slot index
     * @return the matching {@link SlotItem}, or {@code null}
     */
    @Nullable
    private static SlotItem findSlot(final List<SlotItem> slots, final int slotIndex) {
        for (final SlotItem s : slots) {
            if (s.slot() == slotIndex) {
                return s;
            }
        }
        return null;
    }
}
