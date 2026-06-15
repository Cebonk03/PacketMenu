package com.cebonk03.packetmenu.core.domain;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A template describing how a slot should be rendered and behave within a {@link MenuTemplate}.
 *
 * @param slot              the inventory slot index
 * @param priority          rendering priority (higher values render on top)
 * @param baseItem          the default item shown in this slot
 * @param viewRequirement   optional visibility predicate
 * @param clickActions      actions triggered when the slot is clicked
 * @param clickRequirements requirements that must be met before click actions fire
 * @param update            whether this slot should be re-evaluated periodically
 * @param updateInterval    ticks between re-evaluations (only meaningful when {@code update} is true)
 */
public record SlotTemplate(
    int slot,
    int priority,
    ItemStackSnapshot baseItem,
    @Nullable ViewRequirement viewRequirement,
    List<MenuAction> clickActions,
    List<Requirement> clickRequirements,
    boolean update,
    int updateInterval
) {

    /**
     * Compact constructor that defensively copies mutable collections.
     */
    public SlotTemplate {
        clickActions = List.copyOf(clickActions);
        clickRequirements = List.copyOf(clickRequirements);
    }
}
