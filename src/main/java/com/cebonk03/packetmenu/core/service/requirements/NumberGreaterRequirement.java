package com.cebonk03.packetmenu.core.service.requirements;

import com.cebonk03.packetmenu.core.domain.Requirement;
import com.cebonk03.packetmenu.core.domain.RequirementContext;
import com.cebonk03.packetmenu.core.port.PlaceholderPort;

/**
 * A requirement that passes when a placeholder-resolved numeric value is
 * <em>greater than</em> the expected threshold.
 *
 * <p>Both the value and the threshold are resolved as strings through
 * {@link PlaceholderPort} and parsed as {@code double} before comparison.
 */
public final class NumberGreaterRequirement implements Requirement {

    private final PlaceholderPort placeholderPort;
    private final String valueInput;
    private final String thresholdInput;

    /**
     * Creates a new {@code NumberGreaterRequirement}.
     *
     * @param placeholderPort the placeholder resolver
     * @param valueInput      the value string (placeholder template)
     * @param thresholdInput  the threshold string (placeholder template)
     */
    public NumberGreaterRequirement(
            final PlaceholderPort placeholderPort,
            final String valueInput,
            final String thresholdInput
    ) {
        this.placeholderPort = placeholderPort;
        this.valueInput = valueInput;
        this.thresholdInput = thresholdInput;
    }

    @Override
    public boolean test(final RequirementContext context) {
        final String resolvedValue = placeholderPort.resolveString(valueInput, context.viewer());
        final String resolvedThreshold = placeholderPort.resolveString(thresholdInput, context.viewer());
        try {
            final double value = Double.parseDouble(resolvedValue);
            final double threshold = Double.parseDouble(resolvedThreshold);
            return value > threshold;
        } catch (final NumberFormatException e) {
            return false;
        }
    }
}
