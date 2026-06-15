package com.cebonk03.packetmenu.core.service;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ClickHandler;
import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.cebonk03.packetmenu.core.domain.MenuTemplate;
import com.cebonk03.packetmenu.core.domain.Requirement;
import com.cebonk03.packetmenu.core.domain.RequirementContext;
import com.cebonk03.packetmenu.core.domain.SlotItem;
import com.cebonk03.packetmenu.core.domain.SlotTemplate;
import com.cebonk03.packetmenu.core.port.PlaceholderPort;
import com.cebonk03.packetmenu.core.port.PlayerHandle;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Creates {@link MenuSession} instances from {@link MenuTemplate}s, applying
 * requirement checks, placeholder resolution, argument substitution, and
 * view-requirement filtering.
 *
 * <p>The factory also schedules dynamic item updates when the template specifies
 * a positive {@link MenuTemplate#updateInterval()}.
 *
 * <p>All public methods are thread-safe &mdash; operations are per-player and
 * each session is independently assembled.
 */
@NullMarked
public final class MenuFactory {

    private final ContainerIdAllocator containerIdAllocator;
    private final PlaceholderPort placeholderPort;
    private final SchedulerPort schedulerPort;
    private final BiConsumer<PlayerHandle, MenuSession> updateHandler;

    /**
     * Creates a new menu factory.
     *
     * @param containerIdAllocator allocates protocol container IDs for new sessions
     * @param placeholderPort      resolves placeholders in text components and strings
     * @param schedulerPort        schedules dynamic update tasks
     * @param updateHandler        invoked on each update tick with the refreshed session;
     *                             may be a no-op if the caller does not require updates
     */
    public MenuFactory(
            final ContainerIdAllocator containerIdAllocator,
            final PlaceholderPort placeholderPort,
            final SchedulerPort schedulerPort,
            final BiConsumer<PlayerHandle, MenuSession> updateHandler
    ) {
        this.containerIdAllocator = containerIdAllocator;
        this.placeholderPort = placeholderPort;
        this.schedulerPort = schedulerPort;
        this.updateHandler = updateHandler;
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Creates a new {@link MenuSession} for the given player from the provided template.
     *
     * <p>The following steps are performed in order:
     * <ol>
     *   <li>Evaluate the template's {@code openRequirement}. If it fails, no session is
     *       created and this method returns {@code null}.</li>
     *   <li>Resolve placeholders and positional arguments ({@code %arg_1%}, …) in the
     *       title, item display names, and lore strings.</li>
     *   <li>Allocate a unique container ID via {@link ContainerIdAllocator}.</li>
     *   <li>Build slot items from the template's slot templates, converting click actions
     *       and click requirements into a single {@link ClickHandler}.</li>
     *   <li>Apply each slot's {@code viewRequirement} to filter out invisible items.</li>
     *   <li>If the template has a positive {@code updateInterval}, schedule a recurring
     *       task that re-evaluates dynamic slot visibility.</li>
     * </ol>
     *
     * @param player   the viewer who will see the menu
     * @param template the menu template describing layout, items, and behaviour
     * @param args     positional arguments substituted into {@code %arg_N%} placeholders
     * @return a new {@link MenuSession}, or {@code null} if the open requirement denied access
     */
    @Nullable
    public MenuSession create(
            final PlayerHandle player,
            final MenuTemplate template,
            final List<String> args
    ) {
        // Step 1 — check open requirement
        if (!checkOpenRequirement(player, template)) {
            return null;
        }

        // Step 2 — resolve title (placeholders first, then positional args)
        final Component title = resolveTitle(template.title(), player, args);

        // Step 3 — allocate container ID
        final int containerId = containerIdAllocator.allocate(player.getUniqueId());

        // Step 4 — build and filter slots
        final List<SlotItem> slots = buildSlots(player, template, args, containerId, title);

        // Step 5 — create session with only visible slots
        final MenuSession session = new MenuSession(
                containerId,
                template.id(),
                template.type(),
                title,
                slots,
                1,                      // initial revision
                true,                   // notify on close
                template.parentMenuId()
        );

        // Step 6 — schedule dynamic updates if configured
        if (template.updateInterval() > 0) {
            scheduleUpdates(player, session, template);
        }

        return session;
    }

    // ---------------------------------------------------------------
    // Open requirement
    // ---------------------------------------------------------------

    private static boolean checkOpenRequirement(
            final PlayerHandle player,
            final MenuTemplate template
    ) {
        if (template.openRequirement() == null) {
            return true;
        }
        final RequirementContext ctx = new RequirementContext(player, null, null);
        return template.openRequirement().test(ctx);
    }

    // ---------------------------------------------------------------
    // Placeholder & argument resolution
    // ---------------------------------------------------------------

    private Component resolveTitle(
            final Component title,
            final PlayerHandle player,
            final List<String> args
    ) {
        Component resolved = placeholderPort.resolve(title, player);
        resolved = resolveArgsInComponent(resolved, args);
        return resolved;
    }

    private Component resolveArgsInComponent(
            final Component component,
            final List<String> args
    ) {
        final String text = PlainTextComponentSerializer.plainText().serialize(component);
        final String resolved = replaceArgs(text, args);
        return Component.text(resolved);
    }

    private static String replaceArgs(final String text, final List<String> args) {
        String result = text;
        for (int i = 0; i < args.size(); i++) {
            result = result.replace("%arg_" + (i + 1) + "%", args.get(i));
        }
        return result;
    }

    // ---------------------------------------------------------------
    // Slot building & view-requirement filtering
    // ---------------------------------------------------------------

    private List<SlotItem> buildSlots(
            final PlayerHandle player,
            final MenuTemplate template,
            final List<String> args,
            final int containerId,
            final Component title
    ) {
        // Build all slot items from the template first
        final List<SlotItem> allSlots = new ArrayList<>(template.slotTemplates().size());
        for (final SlotTemplate slotTemplate : template.slotTemplates()) {
            final ItemStackSnapshot item = resolveItem(slotTemplate.baseItem(), player, args);
            final ClickHandler handler = buildClickHandler(
                    slotTemplate.clickActions(), slotTemplate.clickRequirements());
            allSlots.add(new SlotItem(
                    slotTemplate.slot(),
                    item,
                    handler,
                    slotTemplate.viewRequirement()
            ));
        }

        // Build a temporary session so view-requirement predicates can evaluate
        // against a valid session context.
        final MenuSession tempSession = new MenuSession(
                containerId,
                template.id(),
                template.type(),
                title,
                List.copyOf(allSlots),
                1,
                true,
                template.parentMenuId()
        );

        // Filter out items whose view requirements are not met
        final List<SlotItem> visible = new ArrayList<>(allSlots.size());
        for (final SlotItem slotItem : allSlots) {
            if (slotItem.viewRequirement() == null) {
                visible.add(slotItem);
            } else {
                final RequirementContext ctx = new RequirementContext(
                        player, tempSession, slotItem);
                if (slotItem.viewRequirement().test(ctx)) {
                    visible.add(slotItem);
                }
            }
        }

        return List.copyOf(visible);
    }

    private ItemStackSnapshot resolveItem(
            final ItemStackSnapshot base,
            final PlayerHandle player,
            final List<String> args
    ) {
        // Resolve display name
        final Component resolvedName;
        if (base.displayName() != null) {
            Component name = base.displayName();
            name = placeholderPort.resolve(name, player);
            name = resolveArgsInComponent(name, args);
            resolvedName = name;
        } else {
            resolvedName = null;
        }

        // Resolve lore
        final List<Component> resolvedLore;
        if (!base.lore().isEmpty()) {
            resolvedLore = base.lore().stream()
                    .map(line -> placeholderPort.resolve(line, player))
                    .map(line -> resolveArgsInComponent(line, args))
                    .toList();
        } else {
            resolvedLore = List.of();
        }

        return new ItemStackSnapshot(
                base.materialKey(),
                base.amount(),
                resolvedName,
                resolvedLore,
                base.enchantments(),
                base.itemFlags(),
                base.nbt(),
                base.customModelData(),
                base.durability(),
                base.skullTexture()
        );
    }

    // ---------------------------------------------------------------
    // Click handler construction
    // ---------------------------------------------------------------

    @Nullable
    private static ClickHandler buildClickHandler(
            final List<MenuAction> actions,
            final List<Requirement> requirements
    ) {
        if (actions.isEmpty()) {
            return null;
        }
        return (clickContext) -> {
            // Check click requirements first
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
            // Execute all click actions
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
    // Dynamic update scheduling
    // ---------------------------------------------------------------

    private void scheduleUpdates(
            final PlayerHandle player,
            final MenuSession session,
            final MenuTemplate template
    ) {
        scheduleUpdateTick(player, session, template);
    }

    private void scheduleUpdateTick(
            final PlayerHandle player,
            final MenuSession session,
            final MenuTemplate template
    ) {
        schedulerPort.runDelayedOnPlayer(player, template.updateInterval(), () -> {
            final MenuSession updated = reEvaluate(player, session, template);
            if (updated != session) {
                updateHandler.accept(player, updated);
                // Re-schedule with the updated session for the next tick
                scheduleUpdateTick(player, updated, template);
            } else {
                // Nothing changed — keep scheduling with the same session
                scheduleUpdateTick(player, session, template);
            }
        });
    }

    private MenuSession reEvaluate(
            final PlayerHandle player,
            final MenuSession session,
            final MenuTemplate template
    ) {
        final List<SlotItem> currentSlots = session.slots();
        final List<SlotItem> updatedSlots = new ArrayList<>(currentSlots.size());

        for (final SlotTemplate slotTemplate : template.slotTemplates()) {
            if (!slotTemplate.update()) {
                // Static slot — carry forward its current state if present
                final SlotItem existing = findSlot(currentSlots, slotTemplate.slot());
                if (existing != null) {
                    updatedSlots.add(existing);
                }
                continue;
            }

            // Re-evaluate view requirement for dynamic slots
            final boolean passesRequirement;
            if (slotTemplate.viewRequirement() == null) {
                passesRequirement = true;
            } else {
                final RequirementContext ctx = new RequirementContext(
                        player, session, null);
                passesRequirement = slotTemplate.viewRequirement().test(ctx);
            }

            final SlotItem existing = findSlot(currentSlots, slotTemplate.slot());
            if (passesRequirement) {
                if (existing != null) {
                    // Still visible — keep as-is
                    updatedSlots.add(existing);
                } else {
                    // Was hidden, now visible — reconstruct from template
                    final ItemStackSnapshot item = resolveItem(
                            slotTemplate.baseItem(), player, List.of());
                    final ClickHandler handler = buildClickHandler(
                            slotTemplate.clickActions(), slotTemplate.clickRequirements());
                    updatedSlots.add(new SlotItem(
                            slotTemplate.slot(), item, handler,
                            slotTemplate.viewRequirement()));
                }
            }
            // else: requirement failed, slot stays hidden
        }

        if (slotsEqual(currentSlots, updatedSlots)) {
            return session;
        }

        return new MenuSession(
                session.containerId(),
                session.menuId(),
                session.type(),
                session.title(),
                List.copyOf(updatedSlots),
                session.revisionId() + 1,
                session.notifyOnClose(),
                session.parentMenuId()
        );
    }

    // ---------------------------------------------------------------
    // Equality helpers
    // ---------------------------------------------------------------

    @Nullable
    private static SlotItem findSlot(final List<SlotItem> slots, final int slotIndex) {
        for (final SlotItem s : slots) {
            if (s.slot() == slotIndex) {
                return s;
            }
        }
        return null;
    }

    private static boolean slotsEqual(
            final List<SlotItem> a,
            final List<SlotItem> b
    ) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            final SlotItem sa = a.get(i);
            final SlotItem sb = b.get(i);
            if (sa.slot() != sb.slot()) {
                return false;
            }
            if (!sa.item().equals(sb.item())) {
                return false;
            }
            // ClickHandler and ViewRequirement are intentionally not compared —
            // they are functional interfaces captured from the same template and
            // effectively equal when the items are equal.
        }
        return true;
    }
}
