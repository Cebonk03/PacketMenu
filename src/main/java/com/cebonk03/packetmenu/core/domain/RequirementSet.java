package com.cebonk03.packetmenu.core.domain;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A named set of requirements evaluated together under a single {@link LogicMode}.
 *
 * <p>When the combined evaluation fails, the {@code denyActions} are executed
 * in order via an {@link ActionContext}.
 *
 * @param mode         how the individual requirements are combined (AND or OR)
 * @param requirements the named requirements to evaluate
 * @param denyActions  actions to run when the combined check fails
 */
public record RequirementSet(
    LogicMode mode,
    Map<String, Requirement> requirements,
    @Nullable List<MenuAction> denyActions
) {

    /**
     * Compact constructor that defensively copies mutable collections.
     */
    public RequirementSet {
        requirements = Map.copyOf(requirements);
        if (denyActions != null) {
            denyActions = List.copyOf(denyActions);
        }
    }
}
