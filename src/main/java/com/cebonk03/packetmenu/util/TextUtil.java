package com.cebonk03.packetmenu.util;

import java.util.List;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NullMarked;

/**
 * Utility methods for text manipulation and parsing.
 *
 * @author Utility Classes
 * @since 1.0.0
 */
@NullMarked
public final class TextUtil {

    private TextUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Parses a MiniMessage string into an Adventure Component.
     * Falls back to legacy color codes (&#167; or &amp;#167;) if parsing fails.
     *
     * @param input the MiniMessage string to parse
     * @return the parsed Component
     * @throws IllegalArgumentException if input is null or empty
     */
    public static Component parseMiniMessage(final String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input cannot be null or blank");
        }

        try {
            return MiniMessage.miniMessage().deserialize(input);
        } catch (Exception e) {
            // Fallback to legacy color codes
            final String legacyInput = parseLegacy(input);
            return MiniMessage.miniMessage().deserialize(legacyInput);
        }
    }

    /**
     * Converts legacy color codes (& or &#167;) to MiniMessage format.
     *
     * @param input the input string with legacy color codes
     * @return the string with MiniMessage format color codes
     */
    public static String parseLegacy(final String input) {
        if (input == null) {
            return null;
        }

        // Convert & to &#167; (MiniMessage format)
        String result = input.replace('&', '§');
        
        // Convert &#167; to &#167; (already in MiniMessage format)
        // Note: In MiniMessage, &#167; is the legacy color code marker
        
        return result;
    }

    /**
     * Checks if a string contains placeholder patterns.
     * Patterns are in the format %placeholder%.
     *
     * @param input the input string to check
     * @return true if the string contains placeholder patterns, false otherwise
     */
    public static boolean hasPlaceholders(final String input) {
        if (input == null) {
            return false;
        }

        // Pattern to match %placeholder% (where placeholder is any non-% character)
        final Pattern placeholderPattern = Pattern.compile("%[^%]+%");
        return placeholderPattern.matcher(input).find();
    }

    /**
     * Replaces placeholders in a string with provided arguments.
     * Replaces %arg_1% through %arg_10% with the corresponding arguments.
     *
     * @param input the input string containing placeholders
     * @param args  the list of arguments to replace placeholders with
     * @return the string with placeholders replaced
     * @throws IllegalArgumentException if input is null or args is null
     */
    public static String replaceArgs(final String input, final List<String> args) {
        if (input == null) {
            return null;
        }
        if (args == null) {
            throw new IllegalArgumentException("Args list cannot be null");
        }

        String result = input;
        
        // Replace %arg_1% through %arg_10%
        for (int i = 0; i < Math.min(args.size(), 10); i++) {
            final String placeholder = "%arg_" + (i + 1) + "%";
            final String replacement = args.get(i);
            result = result.replace(placeholder, replacement != null ? replacement : "");
        }
        
        return result;
    }
}