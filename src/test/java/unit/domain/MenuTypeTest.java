package unit.domain;

import com.cebonk03.packetmenu.core.domain.MenuType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for {@link MenuType}.
 */
class MenuTypeTest {

    @Test
    void generic9x1HasCorrectSizeAndProtocolId() {
        assertAll("GENERIC_9x1",
            () -> assertEquals(9, MenuType.GENERIC_9x1.size()),
            () -> assertEquals(0, MenuType.GENERIC_9x1.protocolTypeId())
        );
    }

    @Test
    void generic9x2HasCorrectSizeAndProtocolId() {
        assertAll("GENERIC_9x2",
            () -> assertEquals(18, MenuType.GENERIC_9x2.size()),
            () -> assertEquals(1, MenuType.GENERIC_9x2.protocolTypeId())
        );
    }

    @Test
    void generic9x3HasCorrectSizeAndProtocolId() {
        assertAll("GENERIC_9x3",
            () -> assertEquals(27, MenuType.GENERIC_9x3.size()),
            () -> assertEquals(2, MenuType.GENERIC_9x3.protocolTypeId())
        );
    }

    @Test
    void generic9x4HasCorrectSizeAndProtocolId() {
        assertAll("GENERIC_9x4",
            () -> assertEquals(36, MenuType.GENERIC_9x4.size()),
            () -> assertEquals(3, MenuType.GENERIC_9x4.protocolTypeId())
        );
    }

    @Test
    void generic9x5HasCorrectSizeAndProtocolId() {
        assertAll("GENERIC_9x5",
            () -> assertEquals(45, MenuType.GENERIC_9x5.size()),
            () -> assertEquals(4, MenuType.GENERIC_9x5.protocolTypeId())
        );
    }

    @Test
    void generic9x6HasCorrectSizeAndProtocolId() {
        assertAll("GENERIC_9x6",
            () -> assertEquals(54, MenuType.GENERIC_9x6.size()),
            () -> assertEquals(5, MenuType.GENERIC_9x6.protocolTypeId())
        );
    }

    @Test
    void generic3x3HasCorrectSizeAndProtocolId() {
        assertAll("GENERIC_3x3",
            () -> assertEquals(9, MenuType.GENERIC_3x3.size()),
            () -> assertEquals(6, MenuType.GENERIC_3x3.protocolTypeId())
        );
    }

    @Test
    void generic9x1And3x3ShareSizeButDifferentProtocolId() {
        assertAll("same size, different protocol type",
            () -> assertEquals(MenuType.GENERIC_9x1.size(), MenuType.GENERIC_3x3.size()),
            () -> assertNotEquals(MenuType.GENERIC_9x1.protocolTypeId(), MenuType.GENERIC_3x3.protocolTypeId())
        );
    }

    @Test
    void sevenEnumConstantsExist() {
        assertEquals(7, MenuType.values().length);
    }

    @Test
    void slotBoundsScaleWithSize() {
        // Valid: 0 to size-1
        assertDoesNotThrow(() -> checkSlotValid(0, MenuType.GENERIC_9x1));
        assertDoesNotThrow(() -> checkSlotValid(8, MenuType.GENERIC_9x1));
        assertAll("GENERIC_9x6 full range",
            () -> assertDoesNotThrow(() -> checkSlotValid(0, MenuType.GENERIC_9x6)),
            () -> assertDoesNotThrow(() -> checkSlotValid(53, MenuType.GENERIC_9x6))
        );
    }

    /**
     * Helper that does nothing on valid indices — used with assertDoesNotThrow above.
     */
    private static void checkSlotValid(final int slot, final MenuType type) {
        if (slot < 0 || slot >= type.size()) {
            throw new IllegalArgumentException("Out of bounds");
        }
    }
}
