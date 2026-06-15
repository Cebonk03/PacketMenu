package com.cebonk03.packetmenu.core.domain;

/**
 * A predicate evaluated against a {@link RequirementContext} to decide
 * whether an action or view should be allowed.
 */
@FunctionalInterface
public interface Requirement {

    /**
     * Evaluate this requirement.
     *
     * @param context the context carrying viewer, session, and optional slot info
     * @return {@code true} if the requirement is satisfied
     */
    boolean test(RequirementContext context);
}
