package com.cebonk03.packetmenu.core.domain;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * A fully-specified menu definition used to spawn {@link MenuSession}s.
 *
 * @param id                  unique identifier for this menu
 * @param title               the window title
 * @param type                the visual layout
 * @param openCommands        commands that can be used to open this menu
 * @param openRequirement     optional requirement a player must satisfy to open this menu
 * @param slotTemplates       the slot layout templates
 * @param fillerItem          optional item used for empty slots
 * @param updateInterval      ticks between automatic slot updates ({@code 0} = no updates)
 * @param closeOnClickOutside whether the menu closes when the player clicks outside
 * @param parentMenuId        the menu this menu was opened from, if nested
 */
public record MenuTemplate(
    String id,
    Component title,
    MenuType type,
    List<String> openCommands,
    @Nullable Requirement openRequirement,
    List<SlotTemplate> slotTemplates,
    @Nullable ItemStackSnapshot fillerItem,
    int updateInterval,
    boolean closeOnClickOutside,
    @Nullable String parentMenuId
) {

    /**
     * Compact constructor that defensively copies mutable collections.
     */
    public MenuTemplate {
        openCommands = List.copyOf(openCommands);
        slotTemplates = List.copyOf(slotTemplates);
    }
}
