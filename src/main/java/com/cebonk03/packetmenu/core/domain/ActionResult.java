package com.cebonk03.packetmenu.core.domain;

/**
 * The result of executing a {@link MenuAction}.
 *
 * <p>Sealed to the three standard outcomes:
 * <ul>
 *   <li>{@link Success} — the action completed normally</li>
 *   <li>{@link Failure} — the action failed with a reason</li>
 *   <li>{@link Delayed} — the action is deferred and re-executed after a tick delay</li>
 * </ul>
 */
public sealed interface ActionResult {

    /**
     * Indicates the action completed successfully.
     */
    record Success() implements ActionResult {
    }

    /**
     * Indicates the action failed.
     *
     * @param reason a human-readable explanation
     */
    record Failure(String reason) implements ActionResult {
    }

    /**
     * Indicates the action should be re-tried after a delay.
     *
     * @param ticks the number of server ticks to wait
     * @param next  the action to execute after the delay
     */
    record Delayed(long ticks, MenuAction next) implements ActionResult {

        /**
         * Compact constructor — {@code next} is deliberately non-null
         * for a delayed action to be meaningful.
         */
        public Delayed {
            // ticks is intentionally allowed to be 0 (immediate re-execution)
        }
    }
}
