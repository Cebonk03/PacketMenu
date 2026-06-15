package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import java.util.Map.Entry;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * An action that gives an item to the player's inventory.
 *
 * <p>If the inventory is full, the excess item drops on the ground. The
 * operation is routed through the player's scheduler for thread safety.
 */
public final class GiveItemAction implements MenuAction {

    private final ItemStackSnapshot item;
    private final SchedulerPort scheduler;

    /**
     * Creates a new give-item action.
     *
     * @param item      the snapshot describing the item to give
     * @param scheduler the scheduler port for thread-safe execution
     */
    public GiveItemAction(final ItemStackSnapshot item, final SchedulerPort scheduler) {
        this.item = item;
        this.scheduler = scheduler;
    }

    @Override
    public ActionResult execute(final ActionContext context) {
        scheduler.runOnPlayer(context.viewer(), () -> {
            final var player = (Player) context.viewer().nativePlayer();
            final var bukkitStack = toBukkitItemStack(item);
            if (bukkitStack != null) {
                final var excess = player.getInventory().addItem(bukkitStack);
                for (final var remaining : excess.values()) {
                    player.getWorld().dropItemNaturally(
                        player.getLocation(), remaining
                    );
                }
            }
        });
        return new ActionResult.Success();
    }

    private static @org.jspecify.annotations.Nullable ItemStack toBukkitItemStack(
        final ItemStackSnapshot snapshot
    ) {
        final var material = Material.getMaterial(snapshot.materialKey().getKey().toUpperCase());
        if (material == null) {
            return null;
        }
        final var stack = new ItemStack(material, snapshot.amount());
        final var meta = stack.getItemMeta();
        if (meta != null) {
            applyMeta(meta, snapshot);
            final boolean applied = stack.setItemMeta(meta);
            if (!applied) {
                return null;
            }
        }
        return stack;
    }

    private static void applyMeta(final ItemMeta meta, final ItemStackSnapshot snapshot) {
        meta.displayName(snapshot.displayName());
        if (!snapshot.lore().isEmpty()) {
            meta.lore(snapshot.lore());
        }
        for (final Entry<Enchantment, Integer> entry : snapshot.enchantments().entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }
        meta.addItemFlags(snapshot.itemFlags().toArray(new ItemFlag[0]));
        if (snapshot.customModelData() != 0) {
            meta.setCustomModelData(snapshot.customModelData());
        }
        if (snapshot.durability() != 0 && meta instanceof final Damageable damageable) {
            damageable.setDamage(snapshot.durability());
        }
    }
}
