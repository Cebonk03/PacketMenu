package com.cebonk03.packetmenu.adapter.packetevents;

import com.cebonk03.packetmenu.core.domain.ClickType;
import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import com.cebonk03.packetmenu.core.service.ClickInterpreter;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType;
import java.util.UUID;
import java.util.function.Function;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for inbound {@code CLICK_WINDOW} packets via PacketEvents and routes
 * them through the domain click-interpretation pipeline.
 *
 * <p>All click processing is offloaded from the netty IO thread to the
 * player's entity scheduler (Paper/Folia region-aware thread).
 */
@NullMarked
public final class PacketEventBus extends SimplePacketListenerAbstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(PacketEventBus.class);

    /**
     * Protocol slot value used by the client when clicking outside the window.
     */
    private static final int SLOT_OUTSIDE_WINDOW = -999;

    private final SchedulerPort scheduler;
    private final Function<UUID, MenuSession> sessionLookup;
    private final ClickInterpreter clickInterpreter;
    private final Function<? super Player, ? extends com.cebonk03.packetmenu.core.port.PlayerHandle> playerHandleFactory;

    /**
     * Creates a new packet event bus.
     *
     * @param scheduler            scheduler for offloading work from the IO thread
     * @param sessionLookup        resolves a player UUID to their active session, or {@code null}
     * @param clickInterpreter     interprets and dispatches click data to slot handlers
     * @param playerHandleFactory  creates a port-level {@code PlayerHandle} from a Bukkit {@link Player}
     */
    public PacketEventBus(
            final SchedulerPort scheduler,
            final Function<UUID, MenuSession> sessionLookup,
            final ClickInterpreter clickInterpreter,
            final Function<? super Player, ? extends com.cebonk03.packetmenu.core.port.PlayerHandle> playerHandleFactory
    ) {
        super(PacketListenerPriority.LOW);
        this.scheduler = scheduler;
        this.sessionLookup = sessionLookup;
        this.clickInterpreter = clickInterpreter;
        this.playerHandleFactory = playerHandleFactory;
    }

    @Override
    public void onPacketPlayReceive(final PacketPlayReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) {
            return;
        }

        final WrapperPlayClientClickWindow clickPacket = new WrapperPlayClientClickWindow(event);
        final int slot = clickPacket.getSlot();
        final int button = clickPacket.getButton();
        final WindowClickType windowClickType = clickPacket.getWindowClickType();

        // Handle click outside the menu window — no slot processing required
        if (slot == SLOT_OUTSIDE_WINDOW) {
            return;
        }

        // Resolve the Bukkit player from the packet event
        final Object rawPlayer = event.getPlayer();
        if (!(rawPlayer instanceof final Player player)) {
            return;
        }

        // Look up the player's active menu session
        final MenuSession session = sessionLookup.apply(player.getUniqueId());
        if (session == null) {
            return;
        }

        // Guard against invalid slot indices
        if (slot < 0 || slot >= session.type().size()) {
            return;
        }

        // Map the raw click data to the domain ClickType
        final ClickType clickType = mapClickType(windowClickType, button);

        // Obtain a port-level player handle
        final com.cebonk03.packetmenu.core.port.PlayerHandle viewer =
                playerHandleFactory.apply(player);

        // Offload click processing to the player's own scheduler thread.
        // This is critical — we must NOT perform any inventory/entity logic
        // on the netty IO thread.
        scheduler.runOnPlayer(viewer, () -> {
            try {
                clickInterpreter.interpret(viewer, session, slot, clickType);
            } catch (final Exception e) {
                LOGGER.error("Failed to process click for player {} (slot={}, type={})",
                        viewer.getName(), slot, clickType, e);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Click-type mapping
    // -------------------------------------------------------------------------

    /**
     * Translates a raw PacketEvents {@link WindowClickType} and button value
     * into the domain {@link ClickType} enum.
     *
     * <p>Mapping reference (slot, button):
     * <ul>
     *   <li>{@code LEFT} — PICKUP, button 0</li>
     *   <li>{@code RIGHT} — PICKUP, button 1</li>
     *   <li>{@code SHIFT_LEFT} — QUICK_MOVE, button 0</li>
     *   <li>{@code SHIFT_RIGHT} — QUICK_MOVE, button 1</li>
     *   <li>{@code MIDDLE} — CLONE or SWAP with button 2</li>
     *   <li>{@code DROP} — THROW, button 0</li>
     *   <li>{@code CONTROL_DROP} — THROW, button 1</li>
     *   <li>{@code DOUBLE_CLICK} — PICKUP_ALL</li>
     * </ul>
     *
     * @param windowClickType the PacketEvents window-click type
     * @param button          the click button number
     * @return the mapped domain click type
     */
    private static ClickType mapClickType(final WindowClickType windowClickType, final int button) {
        return switch (windowClickType) {
            case PICKUP -> button == 0 ? ClickType.LEFT : ClickType.RIGHT;
            case QUICK_MOVE -> button == 0 ? ClickType.SHIFT_LEFT : ClickType.SHIFT_RIGHT;
            case SWAP -> ClickType.MIDDLE;
            case THROW -> button == 0 ? ClickType.DROP : ClickType.CONTROL_DROP;
            case CLONE -> ClickType.MIDDLE;
            case PICKUP_ALL -> ClickType.DOUBLE_CLICK;
            // QUICK_CRAFT (drag) — treat as plain left/right click per button
            case QUICK_CRAFT -> button == 0 ? ClickType.LEFT : ClickType.RIGHT;
            default -> ClickType.LEFT;
        };
    }
}
