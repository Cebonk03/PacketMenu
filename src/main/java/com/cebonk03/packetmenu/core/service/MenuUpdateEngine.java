package com.cebonk03.packetmenu.core.service;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.AnimatedSlotTemplate;
import com.cebonk03.packetmenu.core.domain.ClickHandler;
import com.cebonk03.packetmenu.core.domain.ClickType;
import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.cebonk03.packetmenu.core.domain.MenuTemplate;
import com.cebonk03.packetmenu.core.domain.RequirementContext;
import com.cebonk03.packetmenu.core.domain.SlotItem;
import com.cebonk03.packetmenu.core.domain.SlotTemplate;
import com.cebonk03.packetmenu.core.port.PacketComposer;
import com.cebonk03.packetmenu.core.port.PlayerHandle;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tracks active menu sessions that contain dynamic or animated slots and
 * periodically re-evaluates their state, sending packet-based slot updates
 * for items that have changed.
 *
 * <p>This engine works entirely with the {@link PacketComposer} port and does
 * <strong>not</strong> use any Bukkit Inventory API. It supports two kinds of
 * dynamic slot behaviour:
 * <ul>
 *   <li><em>Dynamic slots</em> — {@link SlotTemplate}s whose
 *       {@link SlotTemplate#update()} flag is {@code true} are re-evaluated
 *       for view-requirement changes.</li>
 *   <li><em>Animated slots</em> — {@link AnimatedSlotTemplate} instances
 *       associated with a slot index cycle through frames at a fixed tick
 *       interval.</li>
 * </ul>
 *
 * <p>Thread safety is guaranteed through {@link ConcurrentHashMap} storage,
 * {@link AtomicBoolean} cancellation flags, and scheduling all work on the
 * player's region thread via {@link SchedulerPort#runDelayedOnPlayer}.
 *
 * <p>Call {@link #unregisterAll()} during plugin {@code onDisable()} to
 * cancel all pending tasks cleanly.
 */
@NullMarked
public final class MenuUpdateEngine {

    private static final ItemStackSnapshot AIR_ITEM = new ItemStackSnapshot(
            NamespacedKey.minecraft("air"),
            0,
            Component.empty(),
            List.of(),
            Map.of(),
            Collections.emptySet(),
            null,
            0,
            0,
            null
    );

    private final SchedulerPort schedulerPort;
    private final PacketComposer packetComposer;
    private final ConcurrentHashMap<UUID, PlayerState> playerStates;
    private final ConcurrentHashMap<String, Map<Integer, AnimatedSlotTemplate>> menuAnimations;

    /**
     * Creates a new update engine.
     *
     * @param schedulerPort the scheduler used for per-player tick scheduling
     * @param packetComposer the composer used to send {@code SetSlot} packets
     */
    public MenuUpdateEngine(
            final SchedulerPort schedulerPort,
            final PacketComposer packetComposer
    ) {
        this.schedulerPort = schedulerPort;
        this.packetComposer = packetComposer;
        this.playerStates = new ConcurrentHashMap<>();
        this.menuAnimations = new ConcurrentHashMap<>();
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Starts a per-player update task for the given session and template.
     *
     * <p>If the player already has an active update task it is cancelled and
     * replaced. The task runs at the interval specified by
     * {@link MenuTemplate#updateInterval()} and re-evaluates dynamic and
     * animated slots on each tick, sending {@code SetSlot} packets for any
     * items that changed.
     *
     * @param player   the target player
     * @param session  the active menu session
     * @param template the menu template that defines dynamic and static slots
     */
    public void register(
            final PlayerHandle player,
            final MenuSession session,
            final MenuTemplate template
    ) {
        final UUID uuid = player.getUniqueId();
        final PlayerState state = new PlayerState(session, template);
        final PlayerState old = playerStates.put(uuid, state);
        if (old != null) {
            old.cancelled.set(true);
        }
        scheduleNextTick(player, state);
    }

    /**
     * Cancels the update task for the given player.
     *
     * <p>The pending scheduled runnable will exit without performing any
     * further work when it next executes.
     *
     * @param player the target player
     */
    public void unregister(final PlayerHandle player) {
        final PlayerState state = playerStates.remove(player.getUniqueId());
        if (state != null) {
            state.cancelled.set(true);
        }
    }

    /**
     * Cancels all pending update tasks and clears internal state.
     *
     * <p>Safe to call during plugin {@code onDisable()}. Delegates to
     * {@link SchedulerPort#cancelAllTasks()} to halt any currently
     * scheduled runnables at the scheduler level.
     */
    public void unregisterAll() {
        playerStates.values().forEach(state -> state.cancelled.set(true));
        playerStates.clear();
        menuAnimations.clear();
        schedulerPort.cancelAllTasks();
    }

    /**
     * Registers animated slot templates for a given menu identifier.
     *
     * <p>When {@link #register} is later called with a template whose
     * {@link MenuTemplate#id()} matches {@code menuId}, the engine will
     * automatically cycle through the animated frames for each registered
     * slot index.
     *
     * @param menuId        the menu identifier
     * @param animatedSlots slot-index-to-template mapping for this menu
     */
    public void setAnimatedSlots(
            final String menuId,
            final Map<Integer, AnimatedSlotTemplate> animatedSlots
    ) {
        menuAnimations.put(menuId, Map.copyOf(animatedSlots));
    }

    // ---------------------------------------------------------------
    // Internal scheduling
    // ---------------------------------------------------------------

    /**
     * Schedules the next update tick for a player state.
     *
     * <p>Uses {@link SchedulerPort#runDelayedOnPlayer} with the template's
     * update interval (minimum 1 tick) and re-schedules itself after each
     * completed tick. The runnable exits immediately if the player has been
     * unregistered (cancelled flag set).
     */
    private void scheduleNextTick(
            final PlayerHandle player,
            final PlayerState state
    ) {
        final long interval = Math.max(1L, state.template.updateInterval());
        schedulerPort.runDelayedOnPlayer(player, interval, () -> {
            if (state.cancelled.get()) {
                return;
            }
            tick(player, state);
            scheduleNextTick(player, state);
        });
    }

    /**
     * Executes one update cycle for a player's session.
     *
     * <ol>
     *   <li>Rebuilds the slot list from the template, computing animation
     *       frames and re-evaluating view requirements.</li>
     *   <li>Compares the rebuilt list with the current session slots by
     *       {@link SlotItem#item()} equality.</li>
     *   <li>Sends {@code SetSlot} packets for any items that changed and
     *       clears slots that were removed.</li>
     *   <li>Updates the stored session with the new slot list and an
     *       incremented revision id.</li>
     * </ol>
     */
    private void tick(
            final PlayerHandle player,
            final PlayerState state
    ) {
        final MenuSession session = state.session;
        final MenuTemplate template = state.template;
        final Map<Integer, AnimatedSlotTemplate> animations =
                menuAnimations.getOrDefault(template.id(), Map.of());
        final long currentTick = state.currentTick.getAndIncrement();

        final List<SlotItem> currentSlots = session.slots();
        final List<SlotItem> updatedSlots = rebuildSlots(
                player, session, template, animations, currentTick, currentSlots
        );

        if (slotsEqual(currentSlots, updatedSlots)) {
            return;
        }

        // Send SetSlot for new or changed items
        for (final SlotItem updated : updatedSlots) {
            final SlotItem current = findSlot(currentSlots, updated.slot());
            if (current == null || !current.item().equals(updated.item())) {
                packetComposer.setSlot(
                        player, session.containerId(), updated.slot(), updated.item()
                );
            }
        }

        // Send air for slots that were removed (became invisible)
        for (final SlotItem current : currentSlots) {
            if (findSlot(updatedSlots, current.slot()) == null) {
                packetComposer.setSlot(
                        player, session.containerId(), current.slot(), AIR_ITEM
                );
            }
        }

        state.session = new MenuSession(
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
    // Slot rebuilding
    // ---------------------------------------------------------------

    /**
     * Rebuilds the live slot list from the template, applying animation
     * frames and view-requirement filtering.
     *
     * @param player       the viewer
     * @param session      the current session (used for requirement context)
     * @param template     the menu template
     * @param animations   animated slot templates by slot index for this menu
     * @param currentTick  the current tick counter for animation frame selection
     * @param currentSlots the current live slot list (for carrying forward
     *                     static slots)
     * @return a new list of visible slot items
     */
    private static List<SlotItem> rebuildSlots(
            final PlayerHandle player,
            final MenuSession session,
            final MenuTemplate template,
            final Map<Integer, AnimatedSlotTemplate> animations,
            final long currentTick,
            final List<SlotItem> currentSlots
    ) {
        final List<SlotItem> result = new ArrayList<>();

        for (final SlotTemplate slotTemplate : template.slotTemplates()) {
            final int slotIndex = slotTemplate.slot();
            final SlotItem existing = findSlot(currentSlots, slotIndex);
            final AnimatedSlotTemplate anim = animations.get(slotIndex);

            if (anim != null) {
                // Animated slot — compute current frame
                final AnimatedSlotTemplate.AnimationFrame frame =
                        anim.currentFrame(currentTick);
                if (frame == null) {
                    continue;
                }

                final boolean visible = frame.viewRequirement() == null
                        || frame.viewRequirement().test(
                                new RequirementContext(player, session, existing));

                if (visible) {
                    final ClickHandler handler =
                            buildAnimatedClickHandler(frame.actions());
                    result.add(new SlotItem(
                            slotIndex,
                            frame.item(),
                            handler,
                            frame.viewRequirement()
                    ));
                }
            } else if (slotTemplate.update()) {
                // Dynamic slot — re-evaluate view requirement
                final boolean passesRequirement;
                if (slotTemplate.viewRequirement() == null) {
                    passesRequirement = true;
                } else {
                    passesRequirement = slotTemplate.viewRequirement().test(
                            new RequirementContext(player, session, existing));
                }

                if (passesRequirement && existing != null) {
                    result.add(existing);
                }
                // else: requirement failed or no existing — slot stays hidden
            } else {
                // Static slot — carry forward if present in the current list
                if (existing != null) {
                    result.add(existing);
                }
            }
        }

        return result;
    }

    // ---------------------------------------------------------------
    // Click handler building
    // ---------------------------------------------------------------

    /**
     * Builds a {@link ClickHandler} that dispatches to per-click-type
     * actions from an animated frame.
     *
     * @param frameActions actions mapped by click type
     * @return a click handler that delegates based on the click type
     */
    private static ClickHandler buildAnimatedClickHandler(
            final Map<ClickType, List<MenuAction>> frameActions
    ) {
        return (clickContext) -> {
            final List<MenuAction> actions = frameActions.get(clickContext.clickType());
            if (actions == null || actions.isEmpty()) {
                return;
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
    // Equality helpers
    // ---------------------------------------------------------------

    /**
     * Finds a slot item by slot index via linear scan.
     *
     * @param slots     the list to search
     * @param slotIndex the target slot index
     * @return the matching slot item, or {@code null}
     */
    @Nullable
    private static SlotItem findSlot(
            final List<SlotItem> slots,
            final int slotIndex
    ) {
        for (final SlotItem s : slots) {
            if (s.slot() == slotIndex) {
                return s;
            }
        }
        return null;
    }

    /**
     * Compares two slot lists for equality based on slot index and item identity.
     *
     * <p>Only {@link SlotItem#slot()} and {@link SlotItem#item()} are compared;
     * click handlers and view requirements are intentionally ignored since they
     * are derived from the same template and do not change between ticks.
     *
     * @param a first slot list
     * @param b second slot list
     * @return {@code true} if both lists contain the same items in the same slots
     */
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
        }
        return true;
    }

    // ---------------------------------------------------------------
    // Per-player state holder
    // ---------------------------------------------------------------

    /**
     * Mutable holder for per-player engine state.
     *
     * <p>Stored in a {@link ConcurrentHashMap} keyed by player UUID. The
     * {@code cancelled} flag is checked by the scheduled runnable before
     * performing any work. The {@code session} is updated after each tick
     * to reflect the latest slot state and revision.
     */
    private static final class PlayerState {

        final AtomicBoolean cancelled = new AtomicBoolean(false);
        final AtomicLong currentTick = new AtomicLong(0);
        volatile MenuSession session;
        final MenuTemplate template;

        PlayerState(final MenuSession session, final MenuTemplate template) {
            this.session = session;
            this.template = template;
        }
    }
}
