package com.cebonk03.packetmenu.adapter.config;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.port.ActionParser;
import com.cebonk03.packetmenu.core.service.ActionRegistry;
import com.cebonk03.packetmenu.util.TextUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * DeluxeMenus-compatible action parser that converts raw action strings into
 * {@link MenuAction} instances.
 *
 * <p>Parsed format:</p>
 * <pre>{@code
 * [actionType] arg1 arg2 ... <delay=ticks>
 * }</pre>
 *
 * <p>The parser uses a {@link ActionRegistry} to look up factory functions
 * keyed by action type.  All known DeluxeMenus action types are pre-registered
 * with default implementations during construction; external action
 * implementations may override them by registering into the same registry
 * with a higher-priority registration pass.
 */
@NullMarked
public final class DeluxeActionParser implements ActionParser {

    /**
     * Regex matching the DeluxeMenus action format.
     *
     * <ul>
     *   <li>Group 1 — action type (e.g. {@code player}, {@code console})</li>
     *   <li>Group 2 — arguments string (optional)</li>
     *   <li>Group 3 — delay in ticks (optional, {@code <delay=N>})</li>
     * </ul>
     */
    private static final Pattern ACTION_PATTERN = Pattern.compile(
            "\\[(\\w+)\\](?:\\s+(.*?))?(?:\\s*<delay=(\\d+)>)?$"
    );

    private final ActionRegistry actionRegistry;

    /**
     * Creates a new {@code DeluxeActionParser} backed by the given registry.
     *
     * <p>All built-in action types are pre-registered during construction.
     *
     * @param actionRegistry the registry to use for factory lookups
     */
    public DeluxeActionParser(final ActionRegistry actionRegistry) {
        this.actionRegistry = actionRegistry;
        registerDefaults();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Unparseable input and unknown action types produce a no-op action
     * that returns {@link ActionResult.Success}.
     */
    @Override
    public MenuAction parse(final String raw) {
        if (raw == null || raw.isBlank()) {
            return ctx -> new ActionResult.Success();
        }

        final Matcher matcher = ACTION_PATTERN.matcher(raw.trim());
        if (!matcher.matches()) {
            return ctx -> new ActionResult.Success();
        }

        final String type = matcher.group(1);
        final @Nullable String argsString = matcher.group(2);
        final @Nullable String delayString = matcher.group(3);

        final List<String> args = argsString != null && !argsString.isBlank()
                ? List.of(argsString.split("\\s+"))
                : List.of();

        final Function<List<String>, MenuAction> factory = actionRegistry.get(type);
        if (factory == null) {
            return ctx -> new ActionResult.Success();
        }

        final MenuAction action = factory.apply(args);
        if (delayString != null) {
            final long delay = Long.parseLong(delayString);
            if (delay > 0) {
                return new DelayAction(delay, action);
            }
        }
        return action;
    }

    /**
     * Parses a list of raw action strings into an immutable list of
     * {@link MenuAction} instances.
     *
     * <p>Convenience method for parsing {@code deny_commands} and similar
     * multi-action configuration sections.  Blank or {@code null} entries
     * are silently skipped.
     *
     * @param rawActions the raw action strings to parse
     * @return an immutable list of parsed menu actions
     */
    public List<MenuAction> parseAll(final List<String> rawActions) {
        if (rawActions == null || rawActions.isEmpty()) {
            return List.of();
        }
        final List<MenuAction> result = new ArrayList<>(rawActions.size());
        for (final String raw : rawActions) {
            if (raw != null && !raw.isBlank()) {
                result.add(parse(raw));
            }
        }
        return List.copyOf(result);
    }

    /**
     * Pre-registers all built-in DeluxeMenus action types into the
     * {@link ActionRegistry}.
     *
     * <p>Actions that require platform access (command dispatch, sound
     * playback, economy, permissions) are registered as stubs that return
     * {@link ActionResult.Success}.  Real implementations from Tasks 17/18
     * should register into the same registry to override these defaults.
     */
    private void registerDefaults() {
        // player — execute a command as the player who clicked
        actionRegistry.register("player", args -> {
            final String command = String.join(" ", args);
            return ctx -> {
                // Platform-specific: Bukkit.dispatchCommand(player, command)
                return new ActionResult.Success();
            };
        });

        // console — execute a command from the server console
        actionRegistry.register("console", args -> {
            final String command = String.join(" ", args);
            return ctx -> {
                // Platform-specific: Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                return new ActionResult.Success();
            };
        });

        // message — send a formatted MiniMessage to the player
        actionRegistry.register("message", args -> {
            final String text = String.join(" ", args);
            final Component message = TextUtil.parseMiniMessage(text);
            return ctx -> {
                ctx.viewer().sendMessage(message);
                return new ActionResult.Success();
            };
        });

        // close — close the currently open menu
        actionRegistry.register("close", args -> ctx -> {
            // Platform-specific: close the player's open inventory
            return new ActionResult.Success();
        });

        // sound — play a sound effect for the player
        actionRegistry.register("sound", args -> ctx -> {
            // Platform-specific: playSound on native player
            return new ActionResult.Success();
        });

        // refresh — re-send the current menu contents
        actionRegistry.register("refresh", args -> ctx -> {
            // Platform-specific: re-render and send menu items
            return new ActionResult.Success();
        });

        // opengui — open a menu by its file name
        actionRegistry.register("opengui", args -> {
            final String menuFile = String.join(" ", args);
            return ctx -> {
                // Platform-specific: load and open menu by file name
                return new ActionResult.Success();
            };
        });

        // openguimenu — open a menu by its registered identifier
        actionRegistry.register("openguimenu", args -> {
            final String menuId = String.join(" ", args);
            return ctx -> {
                // Platform-specific: open menu by registered id
                return new ActionResult.Success();
            };
        });

        // takemoney — withdraw virtual currency from the player
        actionRegistry.register("takemoney", args -> {
            final double amount = parseAmount(args);
            return ctx -> {
                // Platform-specific: economy.withdrawPlayer(...)
                return new ActionResult.Success();
            };
        });

        // givemoney — deposit virtual currency to the player
        actionRegistry.register("givemoney", args -> {
            final double amount = parseAmount(args);
            return ctx -> {
                // Platform-specific: economy.depositPlayer(...)
                return new ActionResult.Success();
            };
        });

        // giveitem — add an item to the player's inventory
        actionRegistry.register("giveitem", args -> {
            final String itemSpec = String.join(" ", args);
            return ctx -> {
                // Platform-specific: give item to player
                return new ActionResult.Success();
            };
        });

        // takeitem — remove an item from the player's inventory
        actionRegistry.register("takeitem", args -> {
            final String itemSpec = String.join(" ", args);
            return ctx -> {
                // Platform-specific: take item from player
                return new ActionResult.Success();
            };
        });

        // givepermission — grant a permission node
        actionRegistry.register("givepermission", args -> {
            final String permission = String.join(" ", args);
            return ctx -> {
                // Platform-specific: permission system grant
                return new ActionResult.Success();
            };
        });

        // takepermission — revoke a permission node
        actionRegistry.register("takepermission", args -> {
            final String permission = String.join(" ", args);
            return ctx -> {
                // Platform-specific: permission system revoke
                return new ActionResult.Success();
            };
        });

        // broadcast — send a formatted message to all online players
        actionRegistry.register("broadcast", args -> {
            final String text = String.join(" ", args);
            final Component message = TextUtil.parseMiniMessage(text);
            return ctx -> {
                // Platform-specific: Bukkit.broadcast(message)
                return new ActionResult.Success();
            };
        });

        // jsonbroadcast — send a JSON/Adventure broadcast to all players
        actionRegistry.register("jsonbroadcast", args -> {
            final String text = String.join(" ", args);
            final Component message = TextUtil.parseMiniMessage(text);
            return ctx -> {
                // Platform-specific: broadcast to all players
                return new ActionResult.Success();
            };
        });

        // broadcastsound — play a sound for all online players
        actionRegistry.register("broadcastsound", args -> ctx -> {
            // Platform-specific: play sound for all players
            return new ActionResult.Success();
        });

        // placeholder — resolve a PlaceholderAPI placeholder string
        actionRegistry.register("placeholder", args -> {
            final String placeholder = String.join(" ", args);
            return ctx -> {
                // Platform-specific: PlaceholderAPI.setPlaceholders(...)
                return new ActionResult.Success();
            };
        });

        // delay — pause action execution for the given number of ticks
        actionRegistry.register("delay", args -> {
            final long ticks = Long.parseLong(args.get(0));
            return ctx -> new ActionResult.Delayed(
                    ticks, ignored -> new ActionResult.Success()
            );
        });
    }

    /**
     * Parses the first element of the argument list as a double-precision
     * monetary amount.
     *
     * @param args the argument list
     * @return the parsed amount, or {@code 0.0} if the list is empty or the
     *         value is not a valid number
     */
    private static double parseAmount(final List<String> args) {
        if (args.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(args.get(0));
        } catch (final NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * A {@link MenuAction} wrapper that returns a {@link ActionResult.Delayed}
     * result, causing the action execution engine to re-attempt the wrapped
     * action after the specified number of server ticks.
     *
     * @param delay  the delay in ticks
     * @param action the action to execute after the delay
     */
    private record DelayAction(long delay, MenuAction action) implements MenuAction {

        @Override
        public ActionResult execute(final ActionContext context) {
            return new ActionResult.Delayed(delay, action);
        }
    }
}
