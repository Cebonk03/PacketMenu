package integration;

import com.cebonk03.packetmenu.adapter.paper.PaperPlayerHandle;
import com.cebonk03.packetmenu.adapter.placeholder.NoOpPlaceholderAdapter;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.ClickType;
import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.cebonk03.packetmenu.core.domain.MenuTemplate;
import com.cebonk03.packetmenu.core.domain.MenuType;
import com.cebonk03.packetmenu.core.domain.SlotItem;
import com.cebonk03.packetmenu.core.domain.SlotTemplate;
import com.cebonk03.packetmenu.core.port.PlaceholderPort;
import com.cebonk03.packetmenu.core.port.PlayerHandle;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import com.cebonk03.packetmenu.core.service.ClickInterpreter;
import com.cebonk03.packetmenu.core.service.ContainerIdAllocator;
import com.cebonk03.packetmenu.core.service.MenuFactory;
import com.cebonk03.packetmenu.core.service.MenuRegistry;
import com.cebonk03.packetmenu.core.service.PlayerCache;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for the full menu lifecycle: template registration, session
 * creation, field verification, and click-handler execution.
 *
 * <p>These tests construct the menu service layer manually rather than loading
 * the full plugin, because PacketEvents is not available in MockBukkit and the
 * plugin self-disables when it detects the missing dependency.
 */
class MenuLifecycleTest {

    private static ServerMock server;
    private static PlayerMock playerMock;
    private static PlayerHandle playerHandle;
    private static PlayerCache playerCache;
    private static ContainerIdAllocator containerIdAllocator;
    private static PlaceholderPort placeholderPort;
    private static SchedulerPort scheduler;
    private static MenuFactory menuFactory;
    private static MenuRegistry menuRegistry;

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

        menuRegistry = new MenuRegistry(
                mock(com.cebonk03.packetmenu.core.port.MenuLoader.class),
                Path.of("/tmp/menus")
        );
    }

    @Test
    void createSessionFromRegisteredTemplate() {
        // Arrange
        MenuTemplate template = new MenuTemplate(
                "test_menu",
                Component.text("Test Menu"),
                MenuType.GENERIC_9x3,
                List.of(),
                null,
                List.of(),
                null,
                0,
                true,
                null
        );
        menuRegistry.register(template);

        // Act
        MenuSession session = menuFactory.create(playerHandle, template);

        // Assert
        assertNotNull(session, "Session must be created for valid template");
        assertEquals("test_menu", session.menuId(), "Session must reference template ID");
        assertEquals(MenuType.GENERIC_9x3, session.type(), "Session must use menu type from template");
        assertEquals(27, session.type().size(), "GENERIC_9x3 must have 27 slots");
        assertEquals(Component.text("Test Menu"), session.title(), "Title must match template");
        assertEquals(1, session.revisionId(), "Initial revision must be 1");
        assertTrue(session.notifyOnClose(), "Default notifyOnClose must be true");
        assertNull(session.parentMenuId(), "No parent menu for top-level menu");
        assertTrue(session.slots().isEmpty(), "Template with no slots produces empty slot list");
    }

    @Test
    void sessionContainerIdIsAllocatedSequentially() {
        MenuTemplate template = new MenuTemplate(
                "seq_test",
                Component.text("Sequence Test"),
                MenuType.GENERIC_9x1,
                List.of(),
                null,
                List.of(),
                null,
                0,
                true,
                null
        );
        menuRegistry.register(template);

        // First session gets container ID 101
        MenuSession first = menuFactory.create(playerHandle, template);
        assertNotNull(first);
        assertEquals(101, first.containerId(), "First container ID must be 101");

        // Second session for same player gets 102
        MenuSession second = menuFactory.create(playerHandle, template);
        assertNotNull(second);
        assertEquals(102, second.containerId(), "Second container ID must increment");
    }

    @Test
    void sessionTracksActiveSessionInCache() {
        MenuTemplate template = new MenuTemplate(
                "cache_test",
                Component.text("Cache Test"),
                MenuType.GENERIC_9x1,
                List.of(),
                null,
                List.of(),
                null,
                0,
                true,
                null
        );
        menuRegistry.register(template);

        MenuSession session = menuFactory.create(playerHandle, template);
        assertNotNull(session);

        // Verify the session is tracked
        MenuSession cached = playerCache.getActiveSession(playerMock.getUniqueId());
        assertNotNull(cached, "Session must be stored in player cache");
        assertEquals(session.menuId(), cached.menuId(),
                "Cached session must match created session");
    }

    @Test
    void openRequirementDeniesAccess() {
        // A requirement that always fails
        MenuTemplate deniedTemplate = new MenuTemplate(
                "denied_menu",
                Component.text("Denied"),
                MenuType.GENERIC_9x1,
                List.of(),
                ctx -> false, // open requirement always fails
                List.of(),
                null,
                0,
                true,
                null
        );
        menuRegistry.register(deniedTemplate);

        MenuSession session = menuFactory.create(playerHandle, deniedTemplate);
        assertNull(session, "Session must be null when open requirement fails");
    }

    @Test
    void clickHandlerExecutesOnSlotClick() {
        // Arrange
        AtomicBoolean clicked = new AtomicBoolean(false);

        MenuAction clickAction = ctx -> {
            clicked.set(true);
            return new ActionResult.Success();
        };

        SlotTemplate clickSlot = new SlotTemplate(
                0,                              // slot index
                0,                              // priority
                new ItemStackSnapshot(
                        NamespacedKey.minecraft("stone"),
                        1,
                        Component.text("Click me"),
                        List.of(),
                        Map.of(),
                        Set.of(),
                        null,
                        0,
                        0,
                        null
                ),
                null,                           // no view requirement
                List.of(clickAction),
                List.of(),                      // no click requirements
                false,
                0
        );

        MenuTemplate template = new MenuTemplate(
                "click_test",
                Component.text("Click Test"),
                MenuType.GENERIC_9x3,
                List.of(),
                null,
                List.of(clickSlot),
                null,
                0,
                true,
                null
        );
        menuRegistry.register(template);

        MenuSession session = menuFactory.create(playerHandle, template);
        assertNotNull(session);
        assertFalse(session.slots().isEmpty(), "Slot list must not be empty");

        // Act — simulate click via ClickInterpreter
        ClickInterpreter interpreter = new ClickInterpreter();
        ActionResult result = interpreter.interpret(
                playerHandle, session, 0, ClickType.LEFT
        );

        // Assert
        assertInstanceOf(ActionResult.Success.class, result,
                "Click must produce a success result");
        assertTrue(clicked.get(), "Click handler must have been invoked");
    }

    @Test
    void clickOnEmptySlotIsSilentlyIgnored() {
        MenuTemplate template = new MenuTemplate(
                "empty_click_test",
                Component.text("Empty Click"),
                MenuType.GENERIC_9x3,
                List.of(),
                null,
                List.of(),
                null,
                0,
                true,
                null
        );
        menuRegistry.register(template);

        MenuSession session = menuFactory.create(playerHandle, template);
        assertNotNull(session);
        assertTrue(session.slots().isEmpty());

        ClickInterpreter interpreter = new ClickInterpreter();
        ActionResult result = interpreter.interpret(
                playerHandle, session, 5, ClickType.RIGHT
        );

        assertInstanceOf(ActionResult.Success.class, result,
                "Click on empty slot must return success without error");
    }

    @Test
    void clickRespectsViewRequirement() {
        // A slot that is invisible (view requirement returns false)
        SlotTemplate hiddenSlot = new SlotTemplate(
                0,
                0,
                new ItemStackSnapshot(
                        NamespacedKey.minecraft("diamond"),
                        1,
                        Component.text("Hidden"),
                        List.of(),
                        Map.of(),
                        Set.of(),
                        null,
                        0,
                        0,
                        null
                ),
                ctx -> false, // view requirement: always hidden
                List.of(ctx -> {
                    fail("Click handler must not be called on hidden slot");
                    return new ActionResult.Success();
                }),
                List.of(),
                false,
                0
        );

        MenuTemplate template = new MenuTemplate(
                "hidden_test",
                Component.text("Hidden Test"),
                MenuType.GENERIC_9x3,
                List.of(),
                null,
                List.of(hiddenSlot),
                null,
                0,
                true,
                null
        );
        menuRegistry.register(template);

        MenuSession session = menuFactory.create(playerHandle, template);
        assertNotNull(session);
        assertTrue(session.slots().isEmpty(),
                "Hidden slot must be filtered out of visible slots");

        ClickInterpreter interpreter = new ClickInterpreter();
        ActionResult result = interpreter.interpret(
                playerHandle, session, 0, ClickType.LEFT
        );

        assertInstanceOf(ActionResult.Success.class, result,
                "Click on hidden slot must succeed without invoking handler");
    }
}
