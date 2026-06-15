package com.cebonk03.packetmenu.core.domain;

import org.jspecify.annotations.Nullable;

/**
 * A concrete item placed in a specific slot of an open menu session.
 *
 * @param slot             the inventory slot index
 * @param item             the immutable item data
 * @param clickHandler     optional handler invoked when the slot is clicked
 * @param viewRequirement  optional predicate that controls visibility
 */
public record SlotItem(
    int slot,
    ItemStackSnapshot item,
    @Nullable ClickHandler clickHandler,
    @Nullable ViewRequirement viewRequirement
) {
}
