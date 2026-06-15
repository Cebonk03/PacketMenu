package com.cebonk03.packetmenu.bootstrap;

import com.cebonk03.packetmenu.adapter.config.ConfigurateMenuLoader;
import com.cebonk03.packetmenu.adapter.packetevents.PacketEventBus;
import com.cebonk03.packetmenu.adapter.paper.PaperPlayerHandle;
import com.cebonk03.packetmenu.adapter.paper.PaperSchedulerAdapter;
import com.cebonk03.packetmenu.adapter.paper.PaperSessionManager;
import com.cebonk03.packetmenu.adapter.placeholder.NoOpPlaceholderAdapter;
import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.cebonk03.packetmenu.core.port.PlaceholderPort;
import com.cebonk03.packetmenu.core.port.PlayerHandle;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import com.cebonk03.packetmenu.core.port.SessionManager;
import com.cebonk03.packetmenu.core.service.ClickInterpreter;
import com.cebonk03.packetmenu.core.service.ContainerIdAllocator;
import com.cebonk03.packetmenu.core.service.MenuFactory;
import com.cebonk03.packetmenu.core.service.MenuRegistry;
import com.cebonk03.packetmenu.core.service.PlayerCache;
import com.github.retrooper.packetevents.PacketEvents;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main plugin bootstrap class for PacketMenu.
 *
 * <p>PacketMenu is a packet-based GUI library for Minecraft Paper/Folia servers.
 * It uses PacketEvents 2.7.0 for packet interception and does not rely on the
 * Bukkit Inventory API.
 *
 * <p>Lifecycle:
 * <ul>
 *   <li>{@link #onLoad()} — create config directory, initialize config file</li>
 *   <li>{@link #onEnable()} — verify PacketEvents, initialize services, register
 *       commands and packet listeners</li>
 *   <li>{@link #onDisable()} — close sessions, cancel tasks, clear registry</li>
 * </ul>
 */
@NullMarked
public final class PacketMenuPlugin extends JavaPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(PacketMenuPlugin.class);

    private ServiceRegistry registry;
    private SchedulerPort scheduler;
    private SessionManager sessionManager;
    private MenuRegistry menuRegistry;
    private MenuFactory menuFactory;

    /**
     * Tracks the active menu session per player UUID.
     *
     * <p>Populated by the menu service when a session is opened and cleared
     * when it is closed. Used by {@link PacketEventBus} to resolve which
     * session a click targets.
     */
    private final Map<UUID, MenuSession> activeSessions = new ConcurrentHashMap<>();

    // ── Lifecycle ────────────────────────────────────────────────────────────────

    @Override
    public void onLoad() {
        LOGGER.info("PacketMenu is loading...");
        createConfigDirectory();
        initializeConfig();
    }

    @Override
    public void onEnable() {
        LOGGER.info("PacketMenu is enabling...");

        if (!isPacketEventsPresent()) {
            LOGGER.warn("PacketEvents not found! PacketMenu requires PacketEvents 2.7.0.");
            LOGGER.warn("Disabling PacketMenu due to missing hard dependency.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        initializeServices();
        registerCommands();
        registerPacketListeners();

        LOGGER.info("PacketMenu has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        LOGGER.info("PacketMenu is disabling...");

        closeSessions();
        cancelTasks();
        clearRegistry();

        LOGGER.info("PacketMenu has been disabled successfully.");
    }

    // ── Configuration ────────────────────────────────────────────────────────────

    private void createConfigDirectory() {
        try {
            final var configDir = getDataFolder().toPath();
            java.nio.file.Files.createDirectories(configDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory", e);
        }
    }

    private void initializeConfig() {
        saveDefaultConfig();
        reloadConfig();
    }

    // ── Dependency check ─────────────────────────────────────────────────────────

    private static boolean isPacketEventsPresent() {
        return Bukkit.getPluginManager().getPlugin("PacketEvents") != null;
    }

    // ── Service wiring ───────────────────────────────────────────────────────────

    private void initializeServices() {
        registry = new ServiceRegistry();

        scheduler = new PaperSchedulerAdapter(this);
        registry.register(SchedulerPort.class, scheduler);

        sessionManager = new PaperSessionManager();
        registry.register(SessionManager.class, sessionManager);

        final ContainerIdAllocator containerIdAllocator = new ContainerIdAllocator();
        registry.register(ContainerIdAllocator.class, containerIdAllocator);

        final PlayerCache playerCache = new PlayerCache();
        registry.register(PlayerCache.class, playerCache);

        final PlaceholderPort placeholderPort = new NoOpPlaceholderAdapter();
        registry.register(PlaceholderPort.class, placeholderPort);

        final ConfigurateMenuLoader configurateLoader = new ConfigurateMenuLoader(scheduler, null);
        registry.register(ConfigurateMenuLoader.class, configurateLoader);

        menuRegistry = new MenuRegistry(
                configurateLoader, getDataFolder().toPath().resolve("menus"));
        registry.register(MenuRegistry.class, menuRegistry);

        final BiConsumer<PlayerHandle, MenuSession> updateHandler = (player, session) -> {
            // No-op: dynamic update packet dispatch will be wired in a future task
        };
        menuFactory = new MenuFactory(
                containerIdAllocator,
                placeholderPort,
                scheduler,
                updateHandler,
                playerCache
        );
        registry.register(MenuFactory.class, menuFactory);

        LOGGER.info("Running on: {} (Folia={})",
                scheduler.isFolia() ? "Folia" : "Paper",
                scheduler.isFolia());
    }

    // ── Command registration ─────────────────────────────────────────────────────

    private void registerCommands() {
        final PluginCommand command = getCommand("packetmenu");
        if (command != null) {
            final var executor = new PacketMenuCommand(this, menuRegistry, menuFactory);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            LOGGER.warn("Command 'packetmenu' not defined in plugin.yml — skipping registration.");
        }
    }

    // ── Packet listener registration ─────────────────────────────────────────────

    private void registerPacketListeners() {
        final var clickInterpreter = new ClickInterpreter();
        registry.register(ClickInterpreter.class, clickInterpreter);

        final var eventBus = new PacketEventBus(
                scheduler,
                activeSessions::get,
                clickInterpreter,
                PaperPlayerHandle::new
        );
        PacketEvents.getAPI().getEventManager().registerListener(eventBus);

        LOGGER.info("Registered PacketEventBus for CLICK_WINDOW handling.");
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────────

    private void closeSessions() {
        if (sessionManager != null) {
            sessionManager.closeAll();
            LOGGER.info("Closed all active menu sessions.");
        }
    }

    private void cancelTasks() {
        if (scheduler != null) {
            scheduler.cancelAllTasks();
            LOGGER.info("Cancelled all scheduled tasks.");
        }
    }

    private void clearRegistry() {
        if (registry != null) {
            registry.clear();
        }
    }
}
