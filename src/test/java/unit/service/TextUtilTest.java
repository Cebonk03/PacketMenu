package unit.service;

import com.cebonk03.packetmenu.util.TextUtil;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for {@link TextUtil}.
 */
class TextUtilTest {

    // ── parseMiniMessage ─────────────────────────────────────────────────────

    @Test
    void parseMiniMessageParsesBasicText() {
        Component result = TextUtil.parseMiniMessage("Hello");
        assertNotNull(result);
    }

    @Test
    void parseMiniMessageParsesColorTag() {
        Component result = TextUtil.parseMiniMessage("<red>Hello</red>");
        assertNotNull(result);
    }

    @Test
    void parseMiniMessageParsesBoldTag() {
        Component result = TextUtil.parseMiniMessage("<bold>Hello</bold>");
        assertNotNull(result);
    }

    @Test
    void parseMiniMessageParsesGradient() {
        Component result = TextUtil.parseMiniMessage("<gradient:red:blue>Hello</gradient>");
        assertNotNull(result);
    }

    @Test
    void parseMiniMessageFallsBackToLegacyCodes() {
        // "&cHello" is not valid MiniMessage; falls back to legacy -> "§cHello"
        Component result = TextUtil.parseMiniMessage("&cHello");
        assertNotNull(result);
    }

    @Test
    void parseMiniMessageFallsBackToLegacyAmpersand() {
        Component result = TextUtil.parseMiniMessage("&aGreen &bAqua");
        assertNotNull(result);
    }

    @Test
    void parseMiniMessageRejectsNull() {
        assertThrows(IllegalArgumentException.class,
            () -> TextUtil.parseMiniMessage(null));
    }

    @Test
    void parseMiniMessageRejectsEmptyString() {
        assertThrows(IllegalArgumentException.class,
            () -> TextUtil.parseMiniMessage(""));
    }

    @Test
    void parseMiniMessageRejectsBlankString() {
        assertThrows(IllegalArgumentException.class,
            () -> TextUtil.parseMiniMessage("   "));
    }

    // ── parseLegacy ──────────────────────────────────────────────────────────

    @Test
    void parseLegacyConvertsAmpersandToSection() {
        assertEquals("§cHello", TextUtil.parseLegacy("&cHello"));
    }

    @Test
    void parseLegacyConvertsMultipleCodes() {
        assertEquals("§a§lGreen Bold", TextUtil.parseLegacy("&a&lGreen Bold"));
    }

    @Test
    void parseLegacyReturnsNullForNullInput() {
        assertNull(TextUtil.parseLegacy(null));
    }

    @Test
    void parseLegacyReturnsEmptyForEmptyInput() {
        assertEquals("", TextUtil.parseLegacy(""));
    }

    @Test
    void parseLegacyPreservesNoCodes() {
        assertEquals("plain text", TextUtil.parseLegacy("plain text"));
    }

    // ── hasPlaceholders ──────────────────────────────────────────────────────

    @Test
    void hasPlaceholdersDetectsPercentPattern() {
        assertTrue(TextUtil.hasPlaceholders("%player_name%"));
    }

    @Test
    void hasPlaceholdersDetectsMultiplePlaceholders() {
        assertTrue(TextUtil.hasPlaceholders("%player_name% has %balance% coins"));
    }

    @Test
    void hasPlaceholdersReturnsFalseForPlainText() {
        assertFalse(TextUtil.hasPlaceholders("Hello world"));
    }

    @Test
    void hasPlaceholdersReturnsFalseForSinglePercent() {
        assertFalse(TextUtil.hasPlaceholders("50% off"));
    }

    @Test
    void hasPlaceholdersReturnsFalseForNull() {
        assertFalse(TextUtil.hasPlaceholders(null));
    }

    // ── replaceArgs ──────────────────────────────────────────────────────────

    @Test
    void replaceArgsReplacesSinglePlaceholder() {
        assertEquals("Hello World",
            TextUtil.replaceArgs("Hello %arg_1%", List.of("World")));
    }

    @Test
    void replaceArgsReplacesMultiplePlaceholders() {
        assertEquals("Hello World from Java",
            TextUtil.replaceArgs("Hello %arg_1% from %arg_2%",
                List.of("World", "Java")));
    }

    @Test
    void replaceArgsLeavesUnmatchedPlaceholders() {
        assertEquals("Hello %arg_1% World",
            TextUtil.replaceArgs("Hello %arg_1% %arg_2%", List.of("World")));
    }

    @Test
    void replaceArgsReturnsNullForNullInput() {
        assertNull(TextUtil.replaceArgs(null, List.of()));
    }

    @Test
    void replaceArgsThrowsForNullArgs() {
        assertThrows(IllegalArgumentException.class,
            () -> TextUtil.replaceArgs("test", null));
    }

    @Test
    void replaceArgsHandlesEmptyArgsList() {
        assertEquals("Hello %arg_1%",
            TextUtil.replaceArgs("Hello %arg_1%", List.of()));
    }

    @Test
    void replaceArgsHandlesReplacementWithEmptyString() {
        assertEquals("Hello ",
            TextUtil.replaceArgs("Hello %arg_1%", new java.util.ArrayList<>(List.of((String) null))));
    }

    @Test
    void replaceArgsRespectsArg10Limit() {
        var args = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k");
        String result = TextUtil.replaceArgs(
            "%arg_1% %arg_10% %arg_11%", args);
        // arg_1 = a, arg_10 = j, arg_11 is beyond 10 limit -> left as is
        assertEquals("a j %arg_11%", result);
    }

    @Test
    void replaceArgsDoesNotModifyEmptyInput() {
        assertEquals("", TextUtil.replaceArgs("", List.of("a")));
    }
}
