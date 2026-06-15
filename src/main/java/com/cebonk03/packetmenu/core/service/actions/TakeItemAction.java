package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * An action that removes matching items from the player's inventory.
 *
 * <p>Items are matched by material type. The operation is routed through the
 * player's scheduler for thread safety.
 */
public final class TakeItemAction implements MenuAction {

    private final ItemStackSnapshot item;
    private final SchedulerPort scheduler;

    /**
     * Creates a new take-item action.
     *
     * @param item      the snapshot describing the item to remove
     * @param scheduler the scheduler port for thread-safe execution
     */
    public TakeItemAction(final ItemStackSnapshot item, final SchedulerPort scheduler) {
        this.item = item;
        this.scheduler = scheduler;
    }

    @Override
    public ActionResult execute(final ActionContext context) {
        final var material = Material.getMaterial(item.materialKey().getKey().toUpperCase());
        if (material == null) {
            return new ActionResult.Failure("Unknown material: " + item.materialKey());
        }
        scheduler.runOnPlayer(context.viewer(), () -> {
            final var player = (Player) context.viewer().nativePlayer();
            final var inventory = player.getInventory();
            final int targetAmount = item.amount();
            int remaining = targetAmount;

            for (int i = 0; i < inventory.getSize() && remaining > 0; i++) {
                final var slot = inventory.getItem(i);
                if (slot != null && slot.getType() == material) {
                    final int toRemove = Math.min(remaining, slot.getAmount());
                    slot.setAmount(slot.getAmount() - toRemove);
                    remaining -= toRemove;
                    if (slot.getAmount() <= 0) {
                        inventory.setItem(i, null);
                    }
                }
            }
        });
        return new ActionResult.Success();
    }
}
