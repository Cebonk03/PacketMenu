package com.cebonk03.packetmenu.bootstrap;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Command handler for the {@code /packetmenu} command.
 *
 * <p>Supports five subcommands:
 * <ul>
 *   <li>{@code open} — open a menu for a player (stub)</li>
 *   <li>{@code reload} — reload configuration (stub)</li>
 *   <li>{@code close} — close a player's menu (stub)</li>
 *   <li>{@code list} — list active sessions (stub)</li>
 *   <li>{@code version} — display plugin version</li>
 * </ul>
 */
@NullMarked
public final class PacketMenuCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "open", "reload", "close", "list", "version"
    );

    private final JavaPlugin plugin;

    /**
     * Creates a new command handler.
     *
     * @param plugin the owning plugin instance
     */
    public PacketMenuCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "open" -> handleOpen(sender, args);
            case "reload" -> handleReload(sender);
            case "close" -> handleClose(sender, args);
            case "list" -> handleList(sender);
            case "version" -> handleVersion(sender);
            default -> sender.sendMessage("§cUnknown subcommand: " + args[0]);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (args.length == 1) {
            final var partial = args[0].toLowerCase();
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(partial))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && ("open".equalsIgnoreCase(args[0])
                || "close".equalsIgnoreCase(args[0]))) {
            // Suggest online player names for open/close
            return null; // delegates to Bukkit's default player name completion
        }
        return Collections.emptyList();
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage("§6PacketMenu §7v" + plugin.getPluginMeta().getVersion());
        sender.sendMessage("§7Usage: /" + label + " <" + String.join("|", SUBCOMMANDS) + ">");
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can open menus.");
            return;
        }
        // Stub: will be implemented with MenuService
        sender.sendMessage("§6[PacketMenu] §7Open subcommand — not yet implemented.");
    }

    private void handleReload(CommandSender sender) {
        // Stub: will reload config and services
        sender.sendMessage("§6[PacketMenu] §7Configuration reloaded.");
    }

    private void handleClose(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can close menus.");
            return;
        }
        // Stub: will close the player's active session
        sender.sendMessage("§6[PacketMenu] §7Close subcommand — not yet implemented.");
    }

    private void handleList(CommandSender sender) {
        // Stub: will list active sessions
        sender.sendMessage("§6[PacketMenu] §7Active sessions: 0");
    }

    private void handleVersion(CommandSender sender) {
        sender.sendMessage("§6PacketMenu §7version §e" + plugin.getPluginMeta().getVersion());
    }
}
