package com.cebonk03.packetmenu.bootstrap;

import com.cebonk03.packetmenu.adapter.paper.PaperPlayerHandle;
import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.cebonk03.packetmenu.core.domain.MenuTemplate;
import com.cebonk03.packetmenu.core.port.PlayerHandle;
import com.cebonk03.packetmenu.core.service.MenuFactory;
import com.cebonk03.packetmenu.core.service.MenuRegistry;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
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
 *   <li>{@code open} — open a menu for a player</li>
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
    private final MenuRegistry menuRegistry;
    private final MenuFactory menuFactory;

    /**
     * Creates a new command handler.
     *
     * @param plugin       the owning plugin instance
     * @param menuRegistry the registry for looking up menu templates
     * @param menuFactory  the factory for creating menu sessions
     */
    public PacketMenuCommand(
            JavaPlugin plugin,
            MenuRegistry menuRegistry,
            MenuFactory menuFactory
    ) {
        this.plugin = plugin;
        this.menuRegistry = menuRegistry;
        this.menuFactory = menuFactory;
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
        if (args.length == 2) {
            if ("open".equalsIgnoreCase(args[0])) {
                // Suggest registered menu IDs
                final var partial = args[1].toLowerCase();
                return menuRegistry.getAll().keySet().stream()
                        .filter(id -> id.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            }
            if ("close".equalsIgnoreCase(args[0])) {
                return null; // delegates to Bukkit's default player name completion
            }
        }
        if (args.length == 3 && "open".equalsIgnoreCase(args[0])) {
            return null; // delegates to Bukkit's default player name completion
        }
        return Collections.emptyList();
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage("§6PacketMenu §7v" + plugin.getPluginMeta().getVersion());
        sender.sendMessage("§7Usage: /" + label + " <" + String.join("|", SUBCOMMANDS) + ">");
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /packetmenu open <menuId> [player] [args...]");
            return;
        }

        final String menuId = args[1];
        final MenuTemplate template = menuRegistry.get(menuId).orElse(null);
        if (template == null) {
            sender.sendMessage("§cMenu not found: " + menuId);
            return;
        }

        final Player targetPlayer;
        final List<String> menuArgs;

        if (sender instanceof ConsoleCommandSender) {
            // Console requires a player name explicitly
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /packetmenu open <menuId> <player> [args...]");
                return;
            }
            final String playerName = args[2];
            targetPlayer = Bukkit.getPlayerExact(playerName);
            if (targetPlayer == null) {
                sender.sendMessage("§cPlayer not found: " + playerName);
                return;
            }
            menuArgs = buildArgList(args, 3);
        } else if (sender instanceof Player player) {
            if (args.length >= 3) {
                // Check if the second argument matches an online player name
                final String potentialName = args[2];
                final Player matched = Bukkit.getPlayerExact(potentialName);
                if (matched != null) {
                    targetPlayer = matched;
                    menuArgs = buildArgList(args, 3);
                } else {
                    targetPlayer = player;
                    menuArgs = buildArgList(args, 2);
                }
            } else {
                targetPlayer = player;
                menuArgs = List.of();
            }
        } else {
            sender.sendMessage("§cUnsupported sender type.");
            return;
        }

        final PlayerHandle playerHandle = new PaperPlayerHandle(targetPlayer);
        final MenuSession session = menuFactory.create(playerHandle, template, menuArgs);
        if (session == null) {
            sender.sendMessage("§cAccess denied: you do not meet the requirements to open this menu.");
            return;
        }

        sender.sendMessage("§6[PacketMenu] §7Menu opened: " + menuId);
    }

    private static List<String> buildArgList(String[] args, int fromIndex) {
        if (fromIndex >= args.length) {
            return List.of();
        }
        return List.of(args).subList(fromIndex, args.length);
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
