package com.cebonk03.packetmenu.adapter.packetevents;

import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.cebonk03.packetmenu.core.domain.MenuType;
import com.cebonk03.packetmenu.core.domain.SlotItem;
import com.cebonk03.packetmenu.core.port.PacketComposer;
import com.cebonk03.packetmenu.core.port.PlayerHandle;
import com.cebonk03.packetmenu.core.service.ItemStackSnapshotPool;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemCustomModelData;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemEnchantments;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemLore;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentType;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link PacketComposer} implementation that uses PacketEvents 2.7.0 packet
 * wrappers to render menu windows on the client.
 *
 * <p>All packet sends are routed through the player's {@link
 * org.bukkit.entity.EntityScheduler} to guarantee thread safety on both Paper
 * and Folia. Item data is converted from the domain {@link ItemStackSnapshot}
 * into PacketEvents {@link ItemStack} using the component-based item builder.
 *
 * <p>This implementation requires the PacketEvents plugin to be installed on
 * the server and loaded before PacketMenu.
 */
@NullMarked
public final class PacketEventsComposer implements PacketComposer {

    private final Plugin plugin;
    private final Cache<String, List<ItemStack>> packetCache;
    private final Set<UUID> activeViewers;
    private final AtomicLong packetsSent;
    private ScheduledExecutorService metricsExecutor;
    private @Nullable ItemStackSnapshotPool itemPool;

    /**
     * Creates a new composer instance.
     *
     * @param plugin the owning plugin instance, used for scheduling
     */
    public PacketEventsComposer(final Plugin plugin) {
        this.plugin = plugin;
        this.packetCache = Caffeine.newBuilder()
                .maximumSize(200)
                .build();
        this.activeViewers = Collections.synchronizedSet(new HashSet<>());
        this.packetsSent = new AtomicLong();
    }

    // ── PacketComposer implementation ──────────────────────────────────────────

    @Override
    public void openWindow(final PlayerHandle player, final MenuSession session) {
        ensureMetricsStarted();
        final Player bukkitPlayer = (Player) player.nativePlayer();
        activeViewers.add(player.getUniqueId());
        final WrapperPlayServerOpenWindow packet = new WrapperPlayServerOpenWindow(
            session.containerId(),
            session.type().protocolTypeId(),
            session.title()
        );
        sendPacket(bukkitPlayer, packet);
    }

    @Override
    public void sendItems(final PlayerHandle player, final MenuSession session) {
        final Player bukkitPlayer = (Player) player.nativePlayer();
        activeViewers.add(player.getUniqueId());

        final String cacheKey = buildCacheKey(session);
        final List<ItemStack> cachedItems = packetCache.getIfPresent(cacheKey);
        if (cachedItems != null) {
            final WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(
                session.containerId(),
                session.revisionId(),
                cachedItems,
                null
            );
            sendPacket(bukkitPlayer, packet);
            return;
        }

        final List<ItemStack> items = buildItemList(session.type(), session.slots());
        packetCache.put(cacheKey, items);
        final WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(
            session.containerId(),
            session.revisionId(),
            items,
            null
        );
        sendPacket(bukkitPlayer, packet);
    }

    @Override
    public void setSlot(
            final PlayerHandle player,
            final int containerId,
            final int slot,
            final ItemStackSnapshot item
    ) {
        final Player bukkitPlayer = (Player) player.nativePlayer();
        final WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(
            containerId,
            0,
            slot,
            toPacketItem(item)
        );
        sendPacket(bukkitPlayer, packet);
    }

    @Override
    public void closeWindow(final PlayerHandle player, final int containerId) {
        activeViewers.remove(player.getUniqueId());
        final Player bukkitPlayer = (Player) player.nativePlayer();
        final WrapperPlayServerCloseWindow packet = new WrapperPlayServerCloseWindow(containerId);
        sendPacket(bukkitPlayer, packet);
    }

    // ── Packet sending ─────────────────────────────────────────────────────────

    /**
     * Sends a packet to the player via the EntityScheduler, ensuring the send
     * executes on the correct region thread (Paper) or tick thread (Folia).
     *
     * @param player the target player
     * @param packet the PacketEvents packet wrapper to send
     */
    private void sendPacket(final Player player, final Object packet) {
        player.getScheduler().run(plugin, scheduledTask -> {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        }, null);
        packetsSent.incrementAndGet();
    }

    // ── Item list construction ─────────────────────────────────────────────────

    /**
     * Builds a full-size list of PacketEvents {@link ItemStack} instances from
     * a menu session's slot data, filling unoccupied slots with air.
     */
    private static List<ItemStack> buildItemList(
            final MenuType type,
            final List<SlotItem> slots
    ) {
        final int size = type.size();
        final List<ItemStack> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(ItemStack.EMPTY);
        }
        for (final SlotItem slotItem : slots) {
            final int slot = slotItem.slot();
            if (slot >= 0 && slot < size) {
                items.set(slot, toPacketItem(slotItem.item()));
            }
        }
        return items;
    }

    /**
     * Builds a cache key for the given session's items.
     *
     * <p>The key is derived from the container ID, revision ID, and a content
     * hash of the slot items. When items change the revision ID changes,
     * resulting in a cache miss and automatic invalidation.
     */
    private static String buildCacheKey(final MenuSession session) {
        int hash = 1;
        for (final SlotItem slotItem : session.slots()) {
            hash = 31 * hash + slotItem.slot();
            hash = 31 * hash + slotItem.item().hashCode();
        }
        return session.containerId() + ":" + session.revisionId() + ":" + Integer.toHexString(hash);
    }

    // ── Domain → PacketEvents item conversion ──────────────────────────────────

    /**
     * Converts a domain {@link ItemStackSnapshot} into a PacketEvents {@link
     * ItemStack} using the component-based item builder.
     *
     * <p>Properties mapped:
     * <ul>
     *   <li>material + amount → builder type/amount</li>
     *   <li>display name → {@link ComponentTypes#CUSTOM_NAME}</li>
     *   <li>lore → {@link ComponentTypes#LORE}</li>
     *   <li>enchantments → {@link ComponentTypes#ENCHANTMENTS}</li>
     *   <li>custom model data → {@link ComponentTypes#CUSTOM_MODEL_DATA_LISTS}</li>
     *   <li>item flags → mapped to component visibility flags</li>
     * </ul>
     *
     * @param snapshot the domain snapshot, may be {@code null}
     * @return the PacketEvents item stack ({@link ItemStack#EMPTY} for null/air)
     */
    private static ItemStack toPacketItem(final @Nullable ItemStackSnapshot snapshot) {
        if (snapshot == null) {
            return ItemStack.EMPTY;
        }

        final ItemType itemType = ItemTypes.getByName(snapshot.materialKey().toString());
        if (itemType == null || itemType == ItemTypes.AIR) {
            return ItemStack.EMPTY;
        }

        final ItemStack.Builder builder = ItemStack.builder()
            .type(itemType)
            .amount(snapshot.amount());

        // ── Display name ───────────────────────────────────────────────────
        if (snapshot.displayName() != null) {
            builder.component(ComponentTypes.CUSTOM_NAME, snapshot.displayName());
        }

        // ── Lore ────────────────────────────────────────────────────────────
        if (!snapshot.lore().isEmpty()) {
            builder.component(ComponentTypes.LORE, new ItemLore(snapshot.lore()));
        }

        // ── Enchantments ────────────────────────────────────────────────────
        if (!snapshot.enchantments().isEmpty()) {
            final Map<EnchantmentType, Integer> enchMap = new HashMap<>();
            for (final Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry
                    : snapshot.enchantments().entrySet()) {
                final EnchantmentType type =
                    EnchantmentTypes.getByName(entry.getKey().getKey().toString());
                if (type != null) {
                    enchMap.put(type, entry.getValue());
                }
            }
            if (!enchMap.isEmpty()) {
                builder.component(
                    ComponentTypes.ENCHANTMENTS,
                    new ItemEnchantments(enchMap, !snapshot.itemFlags().contains(
                        org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS))
                );
            }
        }

        // ── Custom model data (1.21.4+ multi-element format) ─────────────
        if (snapshot.customModelData() != 0) {
            builder.component(
                ComponentTypes.CUSTOM_MODEL_DATA_LISTS,
                new ItemCustomModelData(snapshot.customModelData())
            );
        }

        return builder.build();
    }

    // ── Metrics ───────────────────────────────────────────────────────────────

    /**
     * Registers an {@link ItemStackSnapshotPool} whose pooled-item count will
     * be included in periodic metrics logging.
     *
     * @param itemPool the item pool to monitor
     */
    public void setItemPool(final ItemStackSnapshotPool itemPool) {
        this.itemPool = itemPool;
    }

    /**
     * Returns the total number of packets sent since this composer was created.
     */
    public long getPacketsSent() {
        return packetsSent.get();
    }

    /**
     * Returns the approximate number of players currently viewing a menu window.
     */
    public int getActiveViewerCount() {
        return activeViewers.size();
    }

    /**
     * Starts periodic metrics logging at DEBUG level every 5 minutes.
     *
     * <p>Logs active viewer count, total packets sent, packet cache size, and
     * items pooled count. Metrics are written via the given SLF4J logger.
     *
     * @param logger the SLF4J logger to write metrics to
     */
    public void startMetricsLogging(final Logger logger) {
        stopMetricsLogging();
        metricsExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "PacketMenu-Metrics");
            t.setDaemon(true);
            return t;
        });
        metricsExecutor.scheduleAtFixedRate(() -> {
            final long pooled = itemPool != null ? itemPool.itemsPooledCount() : 0;
            logger.debug(
                "Metrics [PacketMenu]: activeViewers={} packetsSent={} packetCacheSize={} itemsPooled={}",
                activeViewers.size(), packetsSent.get(), packetCache.estimatedSize(), pooled);
        }, 5, 5, TimeUnit.MINUTES);
    }

    private void ensureMetricsStarted() {
        if (metricsExecutor == null || metricsExecutor.isShutdown()) {
            startMetricsLogging(LoggerFactory.getLogger(PacketEventsComposer.class));
        }
    }

    /**
     * Shuts down the metrics logging executor if it is running.
     */
    public void stopMetricsLogging() {
        if (metricsExecutor != null && !metricsExecutor.isShutdown()) {
            metricsExecutor.shutdown();
        }
    }
}
