package com.cebonk03.packetmenu.core.domain;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * An active menu session bound to a specific viewer.
 *
 * <p>This record is immutable; all {@code with*} methods return a new instance.
 *
 * @param containerId   the container id assigned by the Minecraft protocol
 * @param menuId        the identifier of the {@link MenuTemplate} that spawned this session
 * @param type          the visual layout of the menu
 * @param title         the window title shown to the player
 * @param slots         the live slot state for this session
 * @param revisionId    incremented every time the slot layout changes
 * @param notifyOnClose whether to notify the server when this menu closes
 * @param parentMenuId  the menu that opened this one, if any (for nested menus)
 */
public record MenuSession(
    int containerId,
    String menuId,
    MenuType type,
    Component title,
    List<SlotItem> slots,
    int revisionId,
    boolean notifyOnClose,
    @Nullable String parentMenuId
) {

    /**
     * Compact constructor that defensively copies the mutable slot list.
     */
    public MenuSession {
        slots = List.copyOf(slots);
    }

    /**
     * Returns a new session with the revision id incremented by one.
     *
     * @return updated session
     */
    public MenuSession withRevision() {
        return new MenuSession(
            containerId, menuId, type, title, slots, revisionId + 1,
            notifyOnClose, parentMenuId
        );
    }

    /**
     * Returns a new session where the slot at the given index has its item replaced.
     *
     * <p>If no slot with the given index exists, a new {@link SlotItem} is appended.
     *
     * @param slot the slot index
     * @param item the replacement item snapshot
     * @return updated session
     */
    public MenuSession withItem(final int slot, final ItemStackSnapshot item) {
        final List<SlotItem> newSlots = new ArrayList<>(slots);
        boolean replaced = false;
        for (int i = 0; i < newSlots.size(); i++) {
            if (newSlots.get(i).slot() == slot) {
                newSlots.set(
                    i,
                    new SlotItem(
                        slot, item,
                        newSlots.get(i).clickHandler(),
                        newSlots.get(i).viewRequirement()
                    )
                );
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            newSlots.add(new SlotItem(slot, item, null, null));
        }
        return new MenuSession(
            containerId, menuId, type, title, List.copyOf(newSlots),
            revisionId, notifyOnClose, parentMenuId
        );
    }
}
