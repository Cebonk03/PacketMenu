package com.cebonk03.packetmenu.core.service.actions;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

/**
 * Removes (explicitly denies) a permission node from the viewer via Bukkit's
 * {@link org.bukkit.permissions.PermissionAttachment} API.
 *
 * <p>This adds a negative override for the permission, ensuring it evaluates
 * to {@code false} regardless of other grants. The change is applied on the
 * player's scheduler thread for thread safety and is automatically cleaned
 * up when the player disconnects.
 */
@NullMarked
public final class TakePermissionAction implements MenuAction {

    private final String permissionNode;
    private final SchedulerPort scheduler;

    /**
     * Creates a new take-permission action.
     *
     * @param permissionNode the permission node to remove (deny)
     * @param scheduler      the scheduler for executing the permission change
     */
    public TakePermissionAction(final String permissionNode, final SchedulerPort scheduler) {
        this.permissionNode = permissionNode;
        this.scheduler = scheduler;
    }

    @Override
    public ActionResult execute(final ActionContext context) {
        scheduler.runOnPlayer(context.viewer(), () -> {
            final Player player = (Player) context.viewer().nativePlayer();
            if (!player.isOnline()) {
                return;
            }
            final Plugin plugin = Bukkit.getPluginManager().getPlugin("PacketMenu");
            if (plugin != null && plugin.isEnabled()) {
                player.addAttachment(plugin, permissionNode, false);
            }
        });
        return new ActionResult.Success();
    }
}
