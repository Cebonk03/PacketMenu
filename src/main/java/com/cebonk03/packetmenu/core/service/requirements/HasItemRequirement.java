package com.cebonk03.packetmenu.core.service.requirements;

import com.cebonk03.packetmenu.core.domain.Requirement;
import com.cebonk03.packetmenu.core.domain.RequirementContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * A requirement that passes when the viewer's inventory contains at least the
 * specified amount of a given material.
 *
 * <p>Accesses the native Bukkit {@link Player} via
 * {@link com.cebonk03.packetmenu.core.port.PlayerHandle#nativePlayer()}.
 */
public final class HasItemRequirement implements Requirement {

    private final Material material;
    private final int amount;

    /**
     * Creates a new {@code HasItemRequirement}.
     *
     * @param material the required item material
     * @param amount   the minimum number of items required
     */
    public HasItemRequirement(final Material material, final int amount) {
        this.material = material;
        this.amount = amount;
    }

    @Override
    public boolean test(final RequirementContext context) {
        final Object raw = context.viewer().nativePlayer();
        if (!(raw instanceof Player player)) {
            return false;
        }
        final PlayerInventory inventory = player.getInventory();
        int count = 0;
        for (final ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
                if (count >= amount) {
                    return true;
                }
            }
        }
        return count >= amount;
    }
}
