package com.cebonk03.packetmenu.core.service;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.ClickHandler;
import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.cebonk03.packetmenu.core.domain.MenuTemplate;
import com.cebonk03.packetmenu.core.domain.PaginatedMenuTemplate;
import com.cebonk03.packetmenu.core.domain.Requirement;
import com.cebonk03.packetmenu.core.domain.RequirementContext;
import com.cebonk03.packetmenu.core.domain.SlotItem;
import com.cebonk03.packetmenu.core.domain.SlotTemplate;
import com.cebonk03.packetmenu.core.port.PlayerHandle;
import com.cebonk03.packetmenu.core.service.actions.OpenGuiAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Generates paginated {@link MenuSession} instances from a base template and a
 * flat list of data items.
 *
 * <p>This service partitions the provided data items into pages of at most
 * {@link PaginatedMenuTemplate#itemsPerPage()} items each, then produces one
 * {@link MenuSession} per page. Each page session includes:
 *
 * <ul>
 *   <li>The base template's slot items (converted from {@link SlotTemplate}s)</li>
 *   <li>The data items belonging to that page</li>
 *   <li>Navigation items (previous page, next page, page indicator) injected at
 *       the slots specified by the pagination template</li>
 * </ul>
 *
 * <p>The container id on every produced session is set to {@code 0}. The caller
 * is responsible for allocating real container ids (e.g. via
 * {@link ContainerIdAllocator}) before opening a session.
 *
 * <p>All methods in this class are thread-safe &mdash; no shared mutable state
 * is captured or mutated.
 */
@NullMarked
public final class Paginator {

    private static final int INITIAL_REVISION = 1;
    private static final boolean NOTIFY_ON_CLOSE = true;

    private Paginator() {
    }

    /**
     * Partitions the given data items into pages and returns one
     * {@link MenuSession} per page.
     *
     * <p>Edge-case behaviour:
     * <ul>
     *   <li>{@code allDataItems} is empty &rarr; returns a single session
     *       with no data items and no navigation items</li>
     *   <li>A single page of data &rarr; navigation buttons are present but
     *       non-interactive (no {@link ClickHandler}); the page indicator
     *       still shows the page count</li>
     * </ul>
     *
     * @param player       the viewer for whom the pages are generated
     * @param baseTemplate the menu template providing structural slots and metadata
     * @param allDataItems the complete list of data items to paginate
     * @param pagination   the pagination configuration
     * @return an immutable list of page sessions, one per page
     */
    public static List<MenuSession> generatePages(
            final PlayerHandle player,
            final MenuTemplate baseTemplate,
            final List<SlotItem> allDataItems,
            final PaginatedMenuTemplate pagination
    ) {
        final List<List<SlotItem>> pages = partition(allDataItems, pagination.itemsPerPage());
        final int totalPages = pages.size();

        // Extract base slots once from the template (shared across all pages)
        final List<SlotItem> baseSlots = buildBaseSlots(baseTemplate);

        // Edge case: no data items → single page with no navigation
        if (totalPages == 0) {
            return List.of(createSession(
                    baseTemplate,
                    baseSlots,
                    Collections.emptyList(),
                    1, 1,
                    pagination,
                    true   // suppress all navigation
            ));
        }

        final List<MenuSession> sessions = new ArrayList<>(totalPages);
        for (int i = 0; i < totalPages; i++) {
            final int pageNum = i + 1;
            sessions.add(createSession(
                    baseTemplate,
                    baseSlots,
                    pages.get(i),
                    pageNum,
                    totalPages,
                    pagination,
                    false  // show navigation
            ));
        }

        return Collections.unmodifiableList(sessions);
    }

    // ---------------------------------------------------------------
    // Session construction
    // ---------------------------------------------------------------

    /**
     * Creates a single page session by merging base slots, data items, and
     * navigation items in priority order.
     */
    private static MenuSession createSession(
            final MenuTemplate baseTemplate,
            final List<SlotItem> baseSlots,
            final List<SlotItem> dataItems,
            final int currentPage,
            final int totalPages,
            final PaginatedMenuTemplate pagination,
            final boolean suppressNavigation
    ) {
        final String pageMenuId = baseTemplate.id() + ":page_" + currentPage;
        final List<SlotItem> slots = new ArrayList<>(baseSlots);

        // Data items at default priority (added after base, before navigation)
        slots.addAll(dataItems);

        if (!suppressNavigation) {
            // Navigation items at high priority (added last to override conflicts)
            addPrevButton(slots, currentPage, totalPages, pagination, pageMenuId);
            addNextButton(slots, currentPage, totalPages, pagination, pageMenuId);
            addPageIndicator(slots, currentPage, totalPages, pagination);
        }

        return new MenuSession(
                0,                              // container id (caller must allocate)
                pageMenuId,
                baseTemplate.type(),
                baseTemplate.title(),
                List.copyOf(slots),
                INITIAL_REVISION,
                NOTIFY_ON_CLOSE,
                baseTemplate.parentMenuId()
        );
    }

    // ---------------------------------------------------------------
    // Base slot conversion
    // ---------------------------------------------------------------

    /**
     * Converts the template's {@link SlotTemplate}s into a flat list of
     * {@link SlotItem}s, preserving click handlers and view requirements.
     */
    private static List<SlotItem> buildBaseSlots(final MenuTemplate template) {
        final List<SlotTemplate> slotTemplates = template.slotTemplates();
        if (slotTemplates.isEmpty()) {
            return List.of();
        }
        final List<SlotItem> slots = new ArrayList<>(slotTemplates.size());
        for (final SlotTemplate st : slotTemplates) {
            slots.add(new SlotItem(
                    st.slot(),
                    st.baseItem(),
                    buildClickHandler(st.clickActions(), st.clickRequirements()),
                    st.viewRequirement()
            ));
        }
        return slots;
    }

    // ---------------------------------------------------------------
    // Navigation items
    // ---------------------------------------------------------------

    /**
     * Adds (or replaces) the "previous page" button at the configured slot.
     *
     * <p>On the first page the button is rendered with a {@code null} click
     * handler so it is non-interactive.
     */
    private static void addPrevButton(
            final List<SlotItem> slots,
            final int currentPage,
            final int totalPages,
            final PaginatedMenuTemplate pagination,
            final String currentMenuId
    ) {
        final boolean isFirstPage = currentPage == 1;
        final @Nullable ClickHandler handler;
        if (isFirstPage) {
            handler = null;
        } else {
            final String targetMenuId = buildTargetMenuId(currentMenuId, currentPage - 1);
            handler = createNavigationHandler(targetMenuId);
        }

        slots.add(new SlotItem(
                pagination.prevPageSlot(),
                pagination.prevPageItem(),
                handler,
                null
        ));
    }

    /**
     * Adds (or replaces) the "next page" button at the configured slot.
     *
     * <p>On the last page the button is rendered with a {@code null} click
     * handler so it is non-interactive.
     */
    private static void addNextButton(
            final List<SlotItem> slots,
            final int currentPage,
            final int totalPages,
            final PaginatedMenuTemplate pagination,
            final String currentMenuId
    ) {
        final boolean isLastPage = currentPage == totalPages;
        final @Nullable ClickHandler handler;
        if (isLastPage) {
            handler = null;
        } else {
            final String targetMenuId = buildTargetMenuId(currentMenuId, currentPage + 1);
            handler = createNavigationHandler(targetMenuId);
        }

        slots.add(new SlotItem(
                pagination.nextPageSlot(),
                pagination.nextPageItem(),
                handler,
                null
        ));
    }

    /**
     * Adds (or replaces) the page indicator at the configured slot.
     *
     * <p>The indicator's display name is set to {@code "Page X of Y"} while
     * all other visual properties are taken from the template's
     * {@link PaginatedMenuTemplate#pageIndicatorItem()}.
     */
    private static void addPageIndicator(
            final List<SlotItem> slots,
            final int currentPage,
            final int totalPages,
            final PaginatedMenuTemplate pagination
    ) {
        final ItemStackSnapshot template = pagination.pageIndicatorItem();
        final Component pageDisplayName = Component.text(
                "Page " + currentPage + " of " + totalPages);

        final ItemStackSnapshot indicator = new ItemStackSnapshot(
                template.materialKey(),
                template.amount(),
                pageDisplayName,
                template.lore(),
                template.enchantments(),
                template.itemFlags(),
                template.nbt(),
                template.customModelData(),
                template.durability(),
                template.skullTexture()
        );

        slots.add(new SlotItem(
                pagination.pageIndicatorSlot(),
                indicator,
                null,   // page indicator is not clickable
                null
        ));
    }

    // ---------------------------------------------------------------
    // Click handler construction
    // ---------------------------------------------------------------

    /**
     * Creates a {@link ClickHandler} that opens the target page when clicked.
     */
    private static ClickHandler createNavigationHandler(final String targetMenuId) {
        return clickContext -> {
            final var guiAction = new OpenGuiAction(targetMenuId);
            final var result = guiAction.execute(new ActionContext(
                    clickContext.viewer(),
                    clickContext.session(),
                    clickContext.slot(),
                    clickContext.clickType(),
                    guiAction
            ));
            if (result instanceof ActionResult.Failure) {
                // Navigation failure - will be handled when menu system is wired
            }
        };
    }

    /**
     * Builds a {@link ClickHandler} from a list of actions and requirements,
     * matching the pattern used by {@code MenuFactory}.
     */
    @Nullable
    private static ClickHandler buildClickHandler(
            final List<MenuAction> actions,
            final List<Requirement> requirements
    ) {
        if (actions.isEmpty()) {
            return null;
        }
        return clickContext -> {
            final RequirementContext reqCtx = new RequirementContext(
                    clickContext.viewer(),
                    clickContext.session(),
                    clickContext.slot()
            );
            for (final Requirement req : requirements) {
                if (!req.test(reqCtx)) {
                    return;
                }
            }
            final ActionContext actionCtx = new ActionContext(
                    clickContext.viewer(),
                    clickContext.session(),
                    clickContext.slot(),
                    clickContext.clickType(),
                    null
            );
            for (final MenuAction action : actions) {
                action.execute(actionCtx);
            }
        };
    }

    // ---------------------------------------------------------------
    // Partitioning
    // ---------------------------------------------------------------

    /**
     * Splits a list of items into sub-lists of at most {@code pageSize} items each.
     *
     * @param items    the items to partition (may be empty)
     * @param pageSize the maximum size of each partition (&ge; 1)
     * @return a list of partitions, never {@code null}
     */
    private static List<List<SlotItem>> partition(
            final List<SlotItem> items,
            final int pageSize
    ) {
        if (items.isEmpty()) {
            return List.of();
        }
        final int totalItems = items.size();
        final int pageCount = (totalItems + pageSize - 1) / pageSize; // ceiling division
        final List<List<SlotItem>> pages = new ArrayList<>(pageCount);
        for (int i = 0; i < totalItems; i += pageSize) {
            final int end = Math.min(i + pageSize, totalItems);
            pages.add(List.copyOf(items.subList(i, end)));
        }
        return pages;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /**
     * Builds the menu id for the previous or next page given the current page's
     * menu id and the target page number.
     *
     * <p>The current menu id is expected to follow the pattern
     * {@code <baseId>:page_<N>}. This method extracts the base id and
     * replaces the page suffix.
     */
    private static String buildTargetMenuId(
            final String currentMenuId,
            final int targetPage
    ) {
        final int colonIdx = currentMenuId.lastIndexOf(":page_");
        final String baseId;
        if (colonIdx == -1) {
            baseId = currentMenuId;
        } else {
            baseId = currentMenuId.substring(0, colonIdx);
        }
        return baseId + ":page_" + targetPage;
    }
}
