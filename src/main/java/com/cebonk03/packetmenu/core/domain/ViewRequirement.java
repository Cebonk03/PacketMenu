package com.cebonk03.packetmenu.core.domain;

/**
 * Determines whether a slot or element should be visible to a viewer.
 */
@FunctionalInterface
public interface ViewRequirement {

    /**
     * Evaluate whether the element should be shown.
     *
     * @param context the requirement context
     * @return {@code true} if the element is visible
     */
    boolean test(RequirementContext context);
}
