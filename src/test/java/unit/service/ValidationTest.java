package unit.service;

import com.cebonk03.packetmenu.core.domain.MenuType;
import com.cebonk03.packetmenu.util.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Validation}.
 */
class ValidationTest {

    // ── checkSlotIndex ───────────────────────────────────────────────────────

    @Test
    void checkSlotIndexAcceptsValidSlot() {
        assertAll(
            () -> assertDoesNotThrow(() -> Validation.checkSlotIndex(0, MenuType.GENERIC_9x1)),
            () -> assertDoesNotThrow(() -> Validation.checkSlotIndex(8, MenuType.GENERIC_9x1)),
            () -> assertDoesNotThrow(() -> Validation.checkSlotIndex(0, MenuType.GENERIC_9x6)),
            () -> assertDoesNotThrow(() -> Validation.checkSlotIndex(53, MenuType.GENERIC_9x6)),
            () -> assertDoesNotThrow(() -> Validation.checkSlotIndex(0, MenuType.GENERIC_3x3)),
            () -> assertDoesNotThrow(() -> Validation.checkSlotIndex(8, MenuType.GENERIC_3x3))
        );
    }

    @Test
    void checkSlotIndexRejectsNegativeSlot() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> Validation.checkSlotIndex(-1, MenuType.GENERIC_9x3));
        assertTrue(ex.getMessage().contains("-1"));
    }

    @Test
    void checkSlotIndexRejectsSlotEqualToSize() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> Validation.checkSlotIndex(27, MenuType.GENERIC_9x3));
        assertTrue(ex.getMessage().contains("27"));
    }

    @Test
    void checkSlotIndexRejectsSlotBeyondSize() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> Validation.checkSlotIndex(100, MenuType.GENERIC_9x5));
        assertTrue(ex.getMessage().contains("100"));
    }

    @Test
    void checkSlotIndexErrorMessageContainsMenuType() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> Validation.checkSlotIndex(99, MenuType.GENERIC_9x6));
        assertAll("error message should reference type and bounds",
            () -> assertTrue(ex.getMessage().contains("GENERIC_9x6")),
            () -> assertTrue(ex.getMessage().contains("99")),
            () -> assertTrue(ex.getMessage().contains("0-53"))
        );
    }

    // ── checkNotNull ────────────────────────────────────────────────────────

    @Test
    void checkNotNullAcceptsNonNull() {
        assertDoesNotThrow(() -> Validation.checkNotNull("hello", "name"));
    }

    @Test
    void checkNotNullRejectsNull() {
        var ex = assertThrows(NullPointerException.class,
            () -> Validation.checkNotNull(null, "myParam"));
        assertTrue(ex.getMessage().contains("myParam"));
    }

    // ── checkPositive ────────────────────────────────────────────────────────

    @Test
    void checkPositiveAcceptsPositive() {
        assertDoesNotThrow(() -> Validation.checkPositive(1, "count"));
        assertDoesNotThrow(() -> Validation.checkPositive(Integer.MAX_VALUE, "max"));
    }

    @Test
    void checkPositiveRejectsZero() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> Validation.checkPositive(0, "count"));
        assertTrue(ex.getMessage().contains("count"));
    }

    @Test
    void checkPositiveRejectsNegative() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> Validation.checkPositive(-5, "level"));
        assertTrue(ex.getMessage().contains("level"));
        assertTrue(ex.getMessage().contains("-5"));
    }

    // ── checkMaterial ────────────────────────────────────────────────────────

    @Test
    void checkMaterialRejectsNull() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> Validation.checkMaterial(null));
        assertTrue(ex.getMessage().toLowerCase().contains("null"));
    }

    @Test
    void checkMaterialRejectsBlank() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> Validation.checkMaterial("  "));
        assertTrue(ex.getMessage().toLowerCase().contains("blank"));
    }

    @Test
    void checkMaterialRejectsEmptyString() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> Validation.checkMaterial(""));
        assertTrue(ex.getMessage().toLowerCase().contains("blank"));
    }

    @Test
    void checkMaterialThrowsForUnknownMaterial() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> Validation.checkMaterial("NON_EXISTENT_MATERIAL_XYZ"));
        assertTrue(ex.getMessage().contains("NON_EXISTENT_MATERIAL_XYZ"));
    }

    @Test
    void checkMaterialThrowsForGibberishString() {
        assertThrows(IllegalArgumentException.class,
            () -> Validation.checkMaterial("!!!not-a-material!!!"));
    }
}
