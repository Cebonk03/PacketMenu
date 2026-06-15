package unit.service;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.ClickType;
import com.cebonk03.packetmenu.core.domain.LogicMode;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.cebonk03.packetmenu.core.domain.MenuType;
import com.cebonk03.packetmenu.core.domain.Requirement;
import com.cebonk03.packetmenu.core.domain.RequirementContext;
import com.cebonk03.packetmenu.core.domain.RequirementSet;
import com.cebonk03.packetmenu.core.port.PlayerHandle;
import com.cebonk03.packetmenu.core.service.RequirementEvaluator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RequirementEvaluator}.
 *
 * <p>Tests cover AND/OR logic modes, short-circuit behaviour, empty requirement
 * maps, deny action execution, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RequirementEvaluator")
final class RequirementEvaluatorTest {

    @Mock
    private PlayerHandle viewer;

    @Mock
    private MenuAction denyAction;

    private final Requirement passingReq = ctx -> true;
    private final Requirement failingReq = ctx -> false;

    private final MenuSession session = new MenuSession(
        1, "test", MenuType.GENERIC_9x3, Component.text("Test"),
        List.of(), 0, false, null);

    private RequirementContext context() {
        return new RequirementContext(viewer, session, null);
    }

    // ── AND logic ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AND logic mode")
    final class AndLogicTest {

        @Test
        @DisplayName("should return true when all requirements pass")
        void allPass() {
            final var set = new RequirementSet(
                LogicMode.AND,
                Map.of("req1", passingReq, "req2", passingReq),
                null);

            assertTrue(RequirementEvaluator.evaluate(set, context()));
        }

        @Test
        @DisplayName("should short-circuit on first failure")
        void shortCircuitOnFailure() {
            final var reqMap = new LinkedHashMap<String, Requirement>();
            reqMap.put("pass", passingReq);
            reqMap.put("fail", failingReq);
            reqMap.put("never", passingReq);
            final var set = new RequirementSet(LogicMode.AND, reqMap, null);

            assertFalse(RequirementEvaluator.evaluate(set, context()));
        }

        @Test
        @DisplayName("should return true for empty requirement map (vacuous truth)")
        void emptyRequirements() {
            final var set = new RequirementSet(LogicMode.AND, Map.of(), null);
            assertTrue(RequirementEvaluator.evaluate(set, context()));
        }
    }

    // ── OR logic ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("OR logic mode")
    final class OrLogicTest {

        @Test
        @DisplayName("should return true when at least one requirement passes")
        void onePasses() {
            final var reqMap = new LinkedHashMap<String, Requirement>();
            reqMap.put("fail", failingReq);
            reqMap.put("pass", passingReq);
            final var set = new RequirementSet(LogicMode.OR, reqMap, null);

            assertTrue(RequirementEvaluator.evaluate(set, context()));
        }

        @Test
        @DisplayName("should short-circuit on first success")
        void shortCircuitOnSuccess() {
            final var reqMap = new LinkedHashMap<String, Requirement>();
            reqMap.put("pass", passingReq);
            reqMap.put("never", failingReq);
            final var set = new RequirementSet(LogicMode.OR, reqMap, null);

            assertTrue(RequirementEvaluator.evaluate(set, context()));
        }

        @Test
        @DisplayName("should return false when no requirement passes")
        void nonePass() {
            final var set = new RequirementSet(
                LogicMode.OR,
                Map.of("fail1", failingReq, "fail2", failingReq),
                null);

            assertFalse(RequirementEvaluator.evaluate(set, context()));
        }

        @Test
        @DisplayName("should return false for empty requirement map (vacuous false)")
        void emptyRequirements() {
            final var set = new RequirementSet(LogicMode.OR, Map.of(), null);
            assertFalse(RequirementEvaluator.evaluate(set, context()));
        }
    }

    // ── Deny action execution ─────────────────────────────────────────────────

    @Nested
    @DisplayName("deny action execution")
    final class DenyActionTest {

        @Test
        @DisplayName("should execute deny actions when requirements fail")
        void executeDenyActions() {
            when(denyAction.execute(any())).thenReturn(new ActionResult.Success());

            final var set = new RequirementSet(
                LogicMode.AND,
                Map.of("fail", failingReq),
                List.of(denyAction));

            assertFalse(RequirementEvaluator.evaluate(set, context()));

            verify(denyAction, times(1)).execute(any(ActionContext.class));
        }

        @Test
        @DisplayName("should not execute deny actions when requirements pass")
        void noDenyOnPass() {
            final var set = new RequirementSet(
                LogicMode.AND,
                Map.of("pass", passingReq),
                List.of(denyAction));

            assertTrue(RequirementEvaluator.evaluate(set, context()));
            verifyNoInteractions(denyAction);
        }

        @Test
        @DisplayName("should not fail when denyActions is null")
        void nullDenyActions() {
            final var set = new RequirementSet(
                LogicMode.AND,
                Map.of("fail", failingReq),
                null);

            assertFalse(RequirementEvaluator.evaluate(set, context()));
        }

        @Test
        @DisplayName("should execute all deny actions in order")
        void executeMultipleDenyActions() {

            final MenuAction deny1 = mock(MenuAction.class);
            final MenuAction deny2 = mock(MenuAction.class);
            when(deny1.execute(any())).thenReturn(new ActionResult.Success());
            when(deny2.execute(any())).thenReturn(new ActionResult.Success());

            final var set = new RequirementSet(
                LogicMode.AND,
                Map.of("fail", failingReq),
                List.of(deny1, deny2));

            assertFalse(RequirementEvaluator.evaluate(set, context()));
            verify(deny1, times(1)).execute(any());
            verify(deny2, times(1)).execute(any());
        }
    }

    // ── Click type propagation ────────────────────────────────────────────────

    @Nested
    @DisplayName("click type propagation")
    final class ClickTypeTest {

        @Test
        @DisplayName("should use provided click type for deny action context")
        void usesProvidedClickType() {
            when(denyAction.execute(any())).thenReturn(new ActionResult.Success());

            final var set = new RequirementSet(
                LogicMode.AND,
                Map.of("fail", failingReq),
                List.of(denyAction));

            final RequirementContext ctx = new RequirementContext(viewer, session, null);
            RequirementEvaluator.evaluate(set, ctx, ClickType.SHIFT_RIGHT);

            verify(denyAction, times(1)).execute(argThat(
                ac -> ac.clickType() == ClickType.SHIFT_RIGHT));
        }

        @Test
        @DisplayName("should default to LEFT click type")
        void defaultsToLeft() {
            when(denyAction.execute(any())).thenReturn(new ActionResult.Success());

            final var set = new RequirementSet(
                LogicMode.AND,
                Map.of("fail", failingReq),
                List.of(denyAction));

            RequirementEvaluator.evaluate(set, context());

            verify(denyAction, times(1)).execute(argThat(
                ac -> ac.clickType() == ClickType.LEFT));
        }
    }
}
