package integration;

import com.cebonk03.packetmenu.bootstrap.PacketMenuPlugin;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying that PacketMenu handles the absence of PacketEvents
 * gracefully without crashing the server.
 *
 * <p>MockBukkit does not provide PacketEvents, so the plugin will detect the
 * missing dependency in {@code onEnable()}, log a warning, and disable itself.
 * This test validates that the entire startup path completes without exceptions
 * and that the plugin enters a known safe state.
 */
class PluginLoadTest {

    private static ServerMock server;

    @BeforeAll
    static void setUp() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void pluginLoadsWithoutCrashing() {
        assertDoesNotThrow(() -> {
            PacketMenuPlugin plugin = MockBukkit.load(PacketMenuPlugin.class);
            assertNotNull(plugin, "Plugin instance must not be null");
            // The plugin will be disabled because PacketEvents is not present in MockBukkit.
            // This is expected behaviour — the plugin detects the missing hard dependency
            // and disables itself rather than operating in a broken state.
        });
    }

    @Test
    void pluginDetectsMissingPacketEvents() {
        PacketMenuPlugin plugin = MockBukkit.load(PacketMenuPlugin.class);
        assertNotNull(plugin, "Plugin instance must not be null");
        // Without PacketEvents the plugin cannot function and self-disables
        assertFalse(plugin.isEnabled(),
                "Plugin should be disabled when PacketEvents is absent");
    }

    @Test
    void pluginDoesNotThrowDuringLifecycle() {
        assertDoesNotThrow(() -> {
            PacketMenuPlugin plugin = MockBukkit.load(PacketMenuPlugin.class);
            // onLoad and onEnable must complete without propagating exceptions
            assertNotNull(plugin);
        });
    }
}
