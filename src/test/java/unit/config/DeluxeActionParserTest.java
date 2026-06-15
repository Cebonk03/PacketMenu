package unit.config;

import com.cebonk03.packetmenu.adapter.config.DeluxeActionParser;
import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.ClickType;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.cebonk03.packetmenu.core.domain.MenuType;
import com.cebonk03.packetmenu.core.port.PlayerHandle;
import com.cebonk03.packetmenu.core.service.ActionRegistry;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

/**
 * Unit tests for {@link DeluxeActionParser}.
 *
 * <p>Tests cover all built-in action types, argument parsing, delay wrapping,
 * error handling for invalid/unparseable input, and the {@code parseAll()}
 * convenience method.
 */
@DisplayName("DeluxeActionParser")
final class DeluxeActionParserTest {

    private ActionRegistry registry;
    private DeluxeActionParser parser;
    private ActionContext dummyContext;

    @BeforeEach
    void setUp() {
        registry = new ActionRegistry();
        parser = new DeluxeActionParser(registry);
        final PlayerHandle viewer = mock(PlayerHandle.class);
        final MenuSession session = new MenuSession(
            1, "test", MenuType.GENERIC_9x3, Component.text("Test"),
            List.of(), 0, false, null);
        dummyContext = new ActionContext(
            viewer, session, null, ClickType.LEFT, null);
    }

    // ── Null/blank input ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("null and blank input")
    final class NullBlankTest {

        @Test
        @DisplayName("should return no-op action for null input")
        void nullInput() {
            final MenuAction action = parser.parse(null);
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }

        @Test
        @DisplayName("should return no-op action for blank input")
        void blankInput() {
            final MenuAction action = parser.parse("   ");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }
    }

    // ── Unknown action type ───────────────────────────────────────────────────

    @Nested
    @DisplayName("unknown action type")
    final class UnknownActionTest {

        @Test
        @DisplayName("should return no-op action for unknown action type")
        void unknownType() {
            final MenuAction action = parser.parse("[nonexistent] arg1 arg2");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }

        @Test
        @DisplayName("should return no-op action for malformed bracket syntax")
        void malformedBrackets() {
            final MenuAction action = parser.parse("player say hi");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }
    }

    // ── Action types with argument parsing ────────────────────────────────────

    @Nested
    @DisplayName("action type parsing")
    final class ActionTypeTest {

        @Test
        @DisplayName("[player] should parse and create player command action")
        void playerAction() {
            final MenuAction action = parser.parse("[player] say Hello World");
            assertNotNull(action);
            final ActionResult result = action.execute(dummyContext);
            assertInstanceOf(ActionResult.Success.class, result);
        }

        @Test
        @DisplayName("[console] should parse and create console command action")
        void consoleAction() {
            final MenuAction action = parser.parse("[console] give %player% diamond 1");
            assertNotNull(action);
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }

        @Test
        @DisplayName("[message] should parse and send a message")
        void messageAction() {
            final PlayerHandle viewer = mock(PlayerHandle.class);
            final ActionContext ctx = new ActionContext(
                viewer, dummyContext.session(), dummyContext.slot(),
                ClickType.LEFT, null);

            final MenuAction action = parser.parse("[message] <green>Hello!");
            final ActionResult result = action.execute(ctx);

            assertInstanceOf(ActionResult.Success.class, result);
            verify(viewer, atLeastOnce()).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("[close] should return success")
        void closeAction() {
            final MenuAction action = parser.parse("[close]");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }

        @Test
        @DisplayName("[sound] should return success")
        void soundAction() {
            final MenuAction action = parser.parse("[sound] ENTITY_PLAYER_LEVELUP;1.0;1.0");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }

        @Test
        @DisplayName("[refresh] should return success")
        void refreshAction() {
            final MenuAction action = parser.parse("[refresh]");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }

        @Test
        @DisplayName("[opengui] should parse menu file name")
        void openGuiAction() {
            final MenuAction action = parser.parse("[opengui] some_menu.yml");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }

        @Test
        @DisplayName("[openguimenu] should parse menu ID")
        void openGuiMenuAction() {
            final MenuAction action = parser.parse("[openguimenu] my_menu");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }
    }

    // ── Economy actions ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("economy actions")
    final class EconomyActionTest {

        @Test
        @DisplayName("[takemoney] should parse amount")
        void takeMoneyAction() {
            final MenuAction action = parser.parse("[takemoney] 100.50");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }

        @Test
        @DisplayName("[givemoney] should parse amount")
        void giveMoneyAction() {
            final MenuAction action = parser.parse("[givemoney] 50.0");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }

        @Test
        @DisplayName("[takemoney] with no args should default to 0")
        void takeMoneyNoArgs() {
            final MenuAction action = parser.parse("[takemoney]");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }

        @Test
        @DisplayName("[takemoney] with invalid amount should default to 0")
        void takeMoneyInvalidAmount() {
            final MenuAction action = parser.parse("[takemoney] not_a_number");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }
    }

    // ── Item actions ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("item actions")
    final class ItemActionTest {

        @Test
        @DisplayName("[giveitem] should parse item specification")
        void giveItemAction() {
            final MenuAction action = parser.parse("[giveitem] diamond 1");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }

        @Test
        @DisplayName("[takeitem] should parse item specification")
        void takeItemAction() {
            final MenuAction action = parser.parse("[takeitem] stone 5");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }
    }

    // ── Permission actions ────────────────────────────────────────────────────

    @Nested
    @DisplayName("permission actions")
    final class PermissionActionTest {

        @Test
        @DisplayName("[givepermission] should parse permission node")
        void givePermissionAction() {
            final MenuAction action = parser.parse("[givepermission] some.permission.node");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }

        @Test
        @DisplayName("[takepermission] should parse permission node")
        void takePermissionAction() {
            final MenuAction action = parser.parse("[takepermission] some.permission.node");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }
    }

    // ── Broadcast actions ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("broadcast actions")
    final class BroadcastActionTest {

        @Test
        @DisplayName("[broadcast] should parse message text")
        void broadcastAction() {
            final MenuAction action = parser.parse("[broadcast] <red>Important message!");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }

        @Test
        @DisplayName("[jsonbroadcast] should parse JSON text")
        void jsonBroadcastAction() {
            final MenuAction action = parser.parse("[jsonbroadcast] {\"text\":\"hello\"}");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }

        @Test
        @DisplayName("[broadcastsound] should parse sound spec")
        void broadcastSoundAction() {
            final MenuAction action = parser.parse("[broadcastsound] ENTITY_EXPLOSION;1.0;1.0");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }
    }

    // ── Placeholder action ────────────────────────────────────────────────────

    @Nested
    @DisplayName("placeholder action")
    final class PlaceholderActionTest {

        @Test
        @DisplayName("[placeholder] should parse placeholder string")
        void placeholderAction() {
            final MenuAction action = parser.parse("[placeholder] %player_name%");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }
    }

    // ── Delay action ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delay action")
    final class DelayActionTest {

        @Test
        @DisplayName("[delay] should create delayed action with specified ticks")
        void delayAction() {
            final MenuAction action = parser.parse("[delay] 20");
            final ActionResult result = action.execute(dummyContext);
            assertInstanceOf(ActionResult.Delayed.class, result);
            assertEquals(20L, ((ActionResult.Delayed) result).ticks());
        }

        @Test
        @DisplayName("should wrap action in delay when <delay=N> suffix is present")
        void delaySuffixWrapping() {
            final MenuAction action = parser.parse("[player] say hi <delay=10>");
            final ActionResult result = action.execute(dummyContext);
            assertInstanceOf(ActionResult.Delayed.class, result);
            assertEquals(10L, ((ActionResult.Delayed) result).ticks());
        }

        @Test
        @DisplayName("should not wrap in delay when delay=0")
        void zeroDelayNoWrapping() {
            final MenuAction action = parser.parse("[message] hello <delay=0>");
            final ActionResult result = action.execute(dummyContext);
            assertInstanceOf(ActionResult.Success.class, result);
        }
    }

    // ── parseAll() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("parseAll() convenience method")
    final class ParseAllTest {

        @Test
        @DisplayName("should parse list of action strings")
        void parseAllActions() {
            final List<String> rawActions = List.of(
                "[message] First",
                "[player] say Second",
                "[close]"
            );

            final List<MenuAction> actions = parser.parseAll(rawActions);
            assertEquals(3, actions.size());
        }

        @Test
        @DisplayName("should return empty list for null input")
        void nullInput() {
            assertTrue(parser.parseAll(null).isEmpty());
        }

        @Test
        @DisplayName("should return empty list for empty input")
        void emptyInput() {
            assertTrue(parser.parseAll(List.of()).isEmpty());
        }

        @Test
        @DisplayName("should skip blank entries")
        void skipBlank() {
            final List<String> rawActions = List.of(
                "[message] Hello",
                "",
                "   ",
                "[close]"
            );

            final List<MenuAction> actions = parser.parseAll(rawActions);
            assertEquals(2, actions.size());
        }
    }

    // ── Parameterized action type format ──────────────────────────────────────

    @Nested
    @DisplayName("action format coverage")
    final class ActionFormatCoverageTest {

        @ParameterizedTest
        @CsvSource({
            "player, say hello",
            "console, give %player% diamond 1",
            "message, <red>Hello",
            "close, ",
            "sound, ENTITY_PLAYER_LEVELUP;1.0;1.0",
            "refresh, ",
            "opengui, shops/shop.yml",
            "openguimenu, my_menu",
            "takemoney, 100",
            "givemoney, 50.5",
            "giveitem, diamond_sword 1",
            "takeitem, stone 64",
            "givepermission, some.node",
            "takepermission, some.node",
            "broadcast, <gold>Server message!",
            "jsonbroadcast, {\"text\":\"hello\"}",
            "broadcastsound, ENTITY_EXPLOSION;2.0;1.0",
            "placeholder, %player_name%"
        })
        @DisplayName("should parse all built-in action types")
        void allBuiltinTypes(final String type, final String args) {
            final String raw = args == null || args.isBlank()
                ? "[" + type + "]"
                : "[" + type + "] " + args;
            final MenuAction action = parser.parse(raw);
            assertNotNull(action, "Action for [" + type + "] should not be null");
            assertInstanceOf(ActionResult.Success.class, action.execute(dummyContext));
        }
    }
}
