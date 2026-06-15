package com.cebonk03.packetmenu.core.service.requirements;

import com.cebonk03.packetmenu.core.domain.Requirement;
import com.cebonk03.packetmenu.core.domain.RequirementContext;
import com.cebonk03.packetmenu.core.port.PlaceholderPort;

/**
 * A requirement that passes when the placeholder-resolved input string equals
 * the expected value.
 *
 * <p>Both the input and the expected value are resolved through
 * {@link PlaceholderPort} before comparison.
 */
public final class StringEqualsRequirement implements Requirement {

    private final PlaceholderPort placeholderPort;
    private final String input;
    private final String expected;

    /**
     * Creates a new {@code StringEqualsRequirement}.
     *
     * @param placeholderPort the placeholder resolver
     * @param input           the input string (placeholder template)
     * @param expected        the expected string (placeholder template)
     */
    public StringEqualsRequirement(
            final PlaceholderPort placeholderPort,
            final String input,
            final String expected
    ) {
        this.placeholderPort = placeholderPort;
        this.input = input;
        this.expected = expected;
    }

    @Override
    public boolean test(final RequirementContext context) {
        final String resolvedInput = placeholderPort.resolveString(input, context.viewer());
        final String resolvedExpected = placeholderPort.resolveString(expected, context.viewer());
        return resolvedInput.equals(resolvedExpected);
    }
}
