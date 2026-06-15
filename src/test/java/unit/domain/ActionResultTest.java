package unit.domain;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ActionResult} sealed interface and its implementations.
 */
class ActionResultTest {

    @Test
    void successIsActionResult() {
        ActionResult result = new ActionResult.Success();
        assertInstanceOf(ActionResult.class, result);
    }

    @Test
    void successIsRecord() {
        var success = new ActionResult.Success();
        // Verify it's not null and serializes cleanly via toString
        assertNotNull(success.toString());
    }

    @Test
    void successEquality() {
        assertEquals(new ActionResult.Success(), new ActionResult.Success());
    }

    @Test
    void successHashCode() {
        assertEquals(new ActionResult.Success().hashCode(), new ActionResult.Success().hashCode());
    }

    @Test
    void failureIsActionResult() {
        ActionResult result = new ActionResult.Failure("reason");
        assertInstanceOf(ActionResult.class, result);
    }

    @Test
    void failureHasReason() {
        var reason = "Not enough money";
        var failure = new ActionResult.Failure(reason);
        assertEquals(reason, failure.reason());
    }

    @Test
    void failureSupportsEmptyReason() {
        var failure = new ActionResult.Failure("");
        assertEquals("", failure.reason());
    }

    @Test
    void failureSupportsLongReason() {
        var longReason = "a".repeat(1000);
        var failure = new ActionResult.Failure(longReason);
        assertEquals(longReason, failure.reason());
    }

    @Test
    void failureEqualitySameReason() {
        assertEquals(
            new ActionResult.Failure("denied"),
            new ActionResult.Failure("denied")
        );
    }

    @Test
    void failureInequalityDifferentReason() {
        assertNotEquals(
            new ActionResult.Failure("denied"),
            new ActionResult.Failure("blocked")
        );
    }

    @Test
    void delayedIsActionResult() {
        ActionResult result = new ActionResult.Delayed(5, ctx -> new ActionResult.Success());
        assertInstanceOf(ActionResult.class, result);
    }

    @Test
    void delayedHasTicks() {
        var delayed = new ActionResult.Delayed(20, ctx -> new ActionResult.Success());
        assertEquals(20, delayed.ticks());
    }

    @Test
    void delayedHasNextAction() {
        MenuAction action = ctx -> new ActionResult.Success();
        var delayed = new ActionResult.Delayed(10, action);
        assertSame(action, delayed.next());
    }

    @Test
    void delayedAcceptsZeroTicks() {
        var delayed = new ActionResult.Delayed(0, ctx -> new ActionResult.Success());
        assertEquals(0, delayed.ticks());
    }

    @Test
    void delayedEquality() {
        MenuAction action = ctx -> new ActionResult.Success();
        assertEquals(
            new ActionResult.Delayed(5, action),
            new ActionResult.Delayed(5, action)
        );
    }

    @Test
    void sealedInterfaceExhaustivePatternMatching() {
        ActionResult success = new ActionResult.Success();
        ActionResult failure = new ActionResult.Failure("err");
        ActionResult delayed = new ActionResult.Delayed(1, ctx -> new ActionResult.Success());

        assertAll("pattern matching on all sealed subtypes",
            () -> assertTrue(describeActionResult(success).contains("Success")),
            () -> assertTrue(describeActionResult(failure).contains("Failure")),
            () -> assertTrue(describeActionResult(delayed).contains("Delayed"))
        );
    }

    @Test
    void successAndFailureNotEqual() {
        assertNotEquals(new ActionResult.Success(), new ActionResult.Failure("ok"));
    }

    @Test
    void failureReasonMayContainSpecialCharacters() {
        var failure = new ActionResult.Failure("§cAccess denied: %reason%");
        assertEquals("§cAccess denied: %reason%", failure.reason());
    }

    /**
     * Demonstrates Java 21 sealed interface pattern matching exhaustiveness.
     */
    private static String describeActionResult(final ActionResult result) {
        return switch (result) {
            case ActionResult.Success s -> "Success(" + s + ")";
            case ActionResult.Failure f -> "Failure(" + f.reason() + ")";
            case ActionResult.Delayed d -> "Delayed(" + d.ticks() + "t)";
        };
    }
}
