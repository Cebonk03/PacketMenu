package com.cebonk03.packetmenu.adapter.packetevents;

import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.cebonk03.packetmenu.core.domain.MenuType;
import com.cebonk03.packetmenu.core.domain.SlotItem;
import com.cebonk03.packetmenu.core.port.PacketComposer;
import com.cebonk03.packetmenu.core.port.PlayerHandle;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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

    /**
     * Creates a new composer instance.
     *
     * @param plugin the owning plugin instance, used for scheduling
     */
    public PacketEventsComposer(final Plugin plugin) {
        this.plugin = plugin;
    }

    // ── PacketComposer implementation ──────────────────────────────────────────

    @Override
    public void openWindow(final PlayerHandle player, final MenuSession session) {
        final Player bukkitPlayer = (Player) player.nativePlayer();
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
        final List<ItemStack> items = buildItemList(session.type(), session.slots());
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
}
