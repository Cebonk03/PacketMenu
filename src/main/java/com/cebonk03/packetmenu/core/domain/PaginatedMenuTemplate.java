package com.cebonk03.packetmenu.core.domain;

/**
 * Template configuration for paginated menu layouts.
 *
 * <p>This record holds the static configuration that controls how data items are
 * split across pages within a menu &mdash; items per page, navigation slot positions,
 * and the item snapshots used for navigation buttons and the page indicator.
 *
 * <p>The actual page-generation logic lives in {@code Paginator}, which consumes
 * this template along with a base {@link MenuTemplate} and a flat list of data
 * {@link SlotItem}s to produce one {@link MenuSession} per page.
 *
 * @param itemsPerPage       maximum number of data items on each page (must be &ge; 1)
 * @param nextPageSlot       the inventory slot index for the "next page" button
 * @param prevPageSlot       the inventory slot index for the "previous page" button
 * @param pageIndicatorSlot  the inventory slot index for the page indicator
 * @param nextPageItem       the {@link ItemStackSnapshot} used for the "next page" button
 * @param prevPageItem       the {@link ItemStackSnapshot} used for the "previous page" button
 * @param pageIndicatorItem  the {@link ItemStackSnapshot} used as a base for the page indicator
 */
public record PaginatedMenuTemplate(
    int itemsPerPage,
    int nextPageSlot,
    int prevPageSlot,
    int pageIndicatorSlot,
    ItemStackSnapshot nextPageItem,
    ItemStackSnapshot prevPageItem,
    ItemStackSnapshot pageIndicatorItem
) {

    /**
     * Compact constructor that validates the pagination parameters.
     *
     * @throws IllegalArgumentException if {@code itemsPerPage} is less than 1
     */
    public PaginatedMenuTemplate {
        if (itemsPerPage < 1) {
            throw new IllegalArgumentException(
                "itemsPerPage must be at least 1, got " + itemsPerPage);
        }
    }
}
