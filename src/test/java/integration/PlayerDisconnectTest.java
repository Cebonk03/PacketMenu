package integration;

import com.cebonk03.packetmenu.adapter.paper.PaperPlayerHandle;
import com.cebonk03.packetmenu.adapter.placeholder.NoOpPlaceholderAdapter;
import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.cebonk03.packetmenu.core.domain.MenuTemplate;
import com.cebonk03.packetmenu.core.domain.MenuType;
import com.cebonk03.packetmenu.core.port.PlaceholderPort;
import com.cebonk03.packetmenu.core.port.PlayerHandle;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import com.cebonk03.packetmenu.core.service.ContainerIdAllocator;
import com.cebonk03.packetmenu.core.service.MenuFactory;
import com.cebonk03.packetmenu.core.service.PlayerCache;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import java.util.List;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration test verifying session cleanup when a player disconnects.
 *
 * <p>PacketMenu tracks active menu sessions per player in a {@link PlayerCache}.
 * When a player quits, the cache must be invalidated to prevent stale sessions
 * from being reused. This test validates that cleanup pathway.
 */
class PlayerDisconnectTest {

    private static ServerMock server;
    private PlayerMock playerMock;
    private PlayerHandle playerHandle;
    private PlayerCache playerCache;
    private ContainerIdAllocator containerIdAllocator;
    private PlaceholderPort placeholderPort;
    private SchedulerPort scheduler;
    private MenuFactory menuFactory;

    @BeforeAll
    static void setUp() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void tearDown() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUpEach() {
        playerMock = server.addPlayer("TestPlayer");
        playerHandle = new PaperPlayerHandle(playerMock);

        containerIdAllocator = new ContainerIdAllocator();
        placeholderPort = new NoOpPlaceholderAdapter();
        scheduler = mock(SchedulerPort.class);
        playerCache = new PlayerCache();

        @SuppressWarnings("unchecked")
        final BiConsumer<PlayerHandle, MenuSession> updateHandler =
                (BiConsumer<PlayerHandle, MenuSession>) mock(BiConsumer.class);

        menuFactory = new MenuFactory(
                containerIdAllocator,
                placeholderPort,
                scheduler,
                updateHandler,
                playerCache
        );
    }

    @Test
    void activeSessionIsRemovedOnPlayerQuit() {
        // Arrange — open a menu for the player
        MenuTemplate template = new MenuTemplate(
                "disconnect_test",
                Component.text("Disconnect Test"),
                MenuType.GENERIC_9x1,
                List.of(),
                null,
                List.of(),
                null,
                0,
                true,
                null
        );

        MenuSession session = menuFactory.create(playerHandle, template);
        assertNotNull(session, "Session must be created before disconnect");

        // Verify session is active in cache
        MenuSession activeSession = playerCache.getActiveSession(playerMock.getUniqueId());
        assertNotNull(activeSession, "Active session must exist in cache after creation");
        assertEquals("disconnect_test", activeSession.menuId(),
                "Cached session must reference the correct menu");

        // Act — simulate player quit by invalidating the player's cache
        playerCache.invalidatePlayer(playerMock.getUniqueId());

        // Assert — session must be removed from cache
        MenuSession afterQuit = playerCache.getActiveSession(playerMock.getUniqueId());
        assertNull(afterQuit, "Session must be removed from cache after player quit");
    }

    @Test
    void placeholderAndRequirementCacheAreClearedOnQuit() {
        // Populate player's placeholder cache
        playerCache.cachePlaceholder(
                playerMock.getUniqueId(), "some_placeholder",
                Component.text("cached_value")
        );
        assertNotNull(
                playerCache.getCachedPlaceholder(
                        playerMock.getUniqueId(), "some_placeholder"),
                "Placeholder cache must be populated before quit"
        );

        // Populate player's requirement cache
        playerCache.cacheRequirement(
                playerMock.getUniqueId(), "some_requirement", true);
        assertNotNull(
                playerCache.getCachedRequirement(
                        playerMock.getUniqueId(), "some_requirement"),
                "Requirement cache must be populated before quit"
        );

        // Act — simulate player quit
        playerCache.invalidatePlayer(playerMock.getUniqueId());

        // Assert — all caches are cleared for that player
        assertNull(
                playerCache.getCachedPlaceholder(
                        playerMock.getUniqueId(), "some_placeholder"),
                "Placeholder cache must be cleared on quit"
        );
        assertNull(
                playerCache.getCachedRequirement(
                        playerMock.getUniqueId(), "some_requirement"),
                "Requirement cache must be cleared on quit"
        );
    }

    @Test
    void otherPlayerCacheIsNotAffectedByAnotherPlayersQuit() {
        // Create a second player
        PlayerMock otherMock = server.addPlayer("OtherPlayer");
        PlayerHandle otherHandle = new PaperPlayerHandle(otherMock);

        // Create sessions for both players
        MenuTemplate template = new MenuTemplate(
                "shared_test",
                Component.text("Shared"),
                MenuType.GENERIC_9x1,
                List.of(),
                null,
                List.of(),
                null,
                0,
                true,
                null
        );

        MenuSession session1 = menuFactory.create(playerHandle, template);
        MenuSession session2 = menuFactory.create(otherHandle, template);
        assertNotNull(session1);
        assertNotNull(session2);

        // Verify both sessions are cached
        assertNotNull(playerCache.getActiveSession(playerMock.getUniqueId()),
                "First player must have active session");
        assertNotNull(playerCache.getActiveSession(otherMock.getUniqueId()),
                "Second player must have active session");

        // Act — only first player quits
        playerCache.invalidatePlayer(playerMock.getUniqueId());

        // Assert — first player's session is cleared, second player's is preserved
        assertNull(playerCache.getActiveSession(playerMock.getUniqueId()),
                "Quit player's session must be removed");
        assertNotNull(playerCache.getActiveSession(otherMock.getUniqueId()),
                "Other player's session must be preserved");
    }

    @Test
    void sessionCleanupIsIdempotent() {
        // Trying to invalidate a player who has no session must not throw
        assertDoesNotThrow(() -> {
            playerCache.invalidatePlayer(playerMock.getUniqueId());
        }, "Invalidating a player without a session must not throw");

        // Double invalidation must also be safe
        playerCache.invalidatePlayer(playerMock.getUniqueId());
        assertDoesNotThrow(() -> {
            playerCache.invalidatePlayer(playerMock.getUniqueId());
        }, "Double invalidation must be idempotent and not throw");
    }
}
