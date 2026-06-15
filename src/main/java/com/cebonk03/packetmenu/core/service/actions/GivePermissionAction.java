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
 * Grants a permission node to the viewer via Bukkit's
 * {@link org.bukkit.permissions.PermissionAttachment} API.
 *
 * <p>The permission is added on the player's scheduler thread to ensure
 * thread safety. The change persists for the player's current session
 * and is automatically cleaned up when the player disconnects.
 */
@NullMarked
public final class GivePermissionAction implements MenuAction {

    private final String permissionNode;
    private final SchedulerPort scheduler;

    /**
     * Creates a new give-permission action.
     *
     * @param permissionNode the permission node to grant
     * @param scheduler      the scheduler for executing the permission change
     */
    public GivePermissionAction(final String permissionNode, final SchedulerPort scheduler) {
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
                player.addAttachment(plugin, permissionNode, true);
            }
        });
        return new ActionResult.Success();
    }
}
