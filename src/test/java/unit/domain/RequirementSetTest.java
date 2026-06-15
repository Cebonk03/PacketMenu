package unit.domain;

import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.LogicMode;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.domain.Requirement;
import com.cebonk03.packetmenu.core.domain.RequirementSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for {@link RequirementSet}.
 */
class RequirementSetTest {

    private static final Requirement ALWAYS_TRUE = ctx -> true;
    private static final Requirement ALWAYS_FALSE = ctx -> false;
    private static final MenuAction NOOP_ACTION = ctx -> new ActionResult.Success();

    @Test
    void constructorWithAndMode() {
        var set = new RequirementSet(
            LogicMode.AND,
            Map.of("perm1", ALWAYS_TRUE, "perm2", ALWAYS_TRUE),
            List.of(NOOP_ACTION)
        );
        assertEquals(LogicMode.AND, set.mode());
    }

    @Test
    void constructorWithOrMode() {
        var set = new RequirementSet(
            LogicMode.OR,
            Map.of("perm1", ALWAYS_TRUE),
            List.of(NOOP_ACTION)
        );
        assertEquals(LogicMode.OR, set.mode());
    }

    @Test
    void constructorDefensivelyCopiesRequirements() {
        var mutableMap = new HashMap<String, Requirement>();
        mutableMap.put("a", ALWAYS_TRUE);

        var set = new RequirementSet(LogicMode.AND, mutableMap, List.of(NOOP_ACTION));
        mutableMap.put("b", ALWAYS_FALSE);

        assertEquals(1, set.requirements().size());
    }

    @Test
    void constructorDefensivelyCopiesDenyActions() {
        var mutableActions = new ArrayList<MenuAction>();
        mutableActions.add(NOOP_ACTION);

        var set = new RequirementSet(LogicMode.AND, Map.of("a", ALWAYS_TRUE), mutableActions);
        mutableActions.add(ctx -> new ActionResult.Success());

        assertEquals(1, set.denyActions().size());
    }

    @Test
    void constructorAllowsNullDenyActions() {
        var set = new RequirementSet(LogicMode.OR, Map.of("a", ALWAYS_TRUE), null);
        assertNull(set.denyActions());
    }

    @Test
    void constructorAllowsEmptyRequirements() {
        var set = new RequirementSet(LogicMode.AND, Map.of(), List.of());
        assertTrue(set.requirements().isEmpty());
    }

    @Test
    void constructorAllowsEmptyDenyActions() {
        var set = new RequirementSet(LogicMode.AND, Map.of("a", ALWAYS_TRUE), List.of());
        assertTrue(set.denyActions().isEmpty());
    }

    @Test
    void requirementsIsUnmodifiable() {
        var set = new RequirementSet(LogicMode.AND, Map.of("a", ALWAYS_TRUE), null);
        assertThrows(UnsupportedOperationException.class,
            () -> set.requirements().put("b", ALWAYS_FALSE));
    }

    @Test
    void denyActionsIsUnmodifiableWhenNotNull() {
        var set = new RequirementSet(LogicMode.AND, Map.of("a", ALWAYS_TRUE), List.of(NOOP_ACTION));
        assertThrows(UnsupportedOperationException.class,
            () -> set.denyActions().add(NOOP_ACTION));
    }

    @Test
    void multipleRequirementsInMapArePreserved() {
        var reqs = Map.of(
            "a", ALWAYS_TRUE,
            "b", ALWAYS_FALSE,
            "c", ALWAYS_TRUE
        );
        var set = new RequirementSet(LogicMode.AND, reqs, null);

        assertAll("all three requirements present",
            () -> assertEquals(3, set.requirements().size()),
            () -> assertTrue(set.requirements().containsKey("a")),
            () -> assertTrue(set.requirements().containsKey("b")),
            () -> assertTrue(set.requirements().containsKey("c"))
        );
    }

    @Test
    void nullRequirementsMapPassedToConstructorThrows() {
        assertThrows(NullPointerException.class,
            () -> new RequirementSet(LogicMode.AND, null, null));
    }

    @Test
    void equalityBasedOnAllComponents() {
        var a = new RequirementSet(LogicMode.AND, Map.of("x", ALWAYS_TRUE), List.of(NOOP_ACTION));
        var b = new RequirementSet(LogicMode.AND, Map.of("x", ALWAYS_TRUE), List.of(NOOP_ACTION));
        assertEquals(a, b);
    }

    @Test
    void differentLogicModeNotEqual() {
        var andSet = new RequirementSet(LogicMode.AND, Map.of("x", ALWAYS_TRUE), null);
        var orSet = new RequirementSet(LogicMode.OR, Map.of("x", ALWAYS_TRUE), null);
        assertNotEquals(andSet, orSet);
    }
}
