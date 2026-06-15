package com.cebonk03.packetmenu.core.service;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ClickType;
import com.cebonk03.packetmenu.core.domain.LogicMode;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import com.cebonk03.packetmenu.core.domain.Requirement;
import com.cebonk03.packetmenu.core.domain.RequirementContext;
import com.cebonk03.packetmenu.core.domain.RequirementSet;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * Evaluates {@link RequirementSet} instances against a given
 * {@link RequirementContext}.
 *
 * <p>Supports {@link LogicMode#AND} (short-circuits on first {@code false})
 * and {@link LogicMode#OR} (short-circuits on first {@code true}).  When the
 * combined evaluation fails the set's {@code denyActions} are executed.
 *
 * <p>This class is stateless and thread-safe.
 */
@NullMarked
public final class RequirementEvaluator {

    private RequirementEvaluator() {
        // static utility class
    }

    /**
     * Evaluates a requirement set with a default click type of
     * {@link ClickType#LEFT} for deny action execution.
     *
     * @param set     the requirement set to evaluate
     * @param context the context for evaluation
     * @return {@code true} if the combined requirement passed
     */
    public static boolean evaluate(
            final RequirementSet set,
            final RequirementContext context
    ) {
        return evaluate(set, context, ClickType.LEFT);
    }

    /**
     * Evaluates a requirement set.
     *
     * @param set       the requirement set to evaluate
     * @param context   the context for evaluation
     * @param clickType the click type used when executing deny actions
     * @return {@code true} if the combined requirement passed
     */
    public static boolean evaluate(
            final RequirementSet set,
            final RequirementContext context,
            final ClickType clickType
    ) {
        final boolean passed = evaluateInternal(set.mode(), set.requirements(), context);

        if (!passed) {
            executeDenyActions(set.denyActions(), context, clickType);
        }

        return passed;
    }

    // ---------------------------------------------------------------
    // Internal evaluation
    // ---------------------------------------------------------------

    private static boolean evaluateInternal(
            final LogicMode mode,
            final Map<String, Requirement> requirements,
            final RequirementContext context
    ) {
        if (requirements.isEmpty()) {
            // AND on empty → vacuously true; OR on empty → vacuously false
            return mode == LogicMode.AND;
        }

        return switch (mode) {
            case AND -> evaluateAnd(requirements, context);
            case OR -> evaluateOr(requirements, context);
        };
    }

    private static boolean evaluateAnd(
            final Map<String, Requirement> requirements,
            final RequirementContext context
    ) {
        for (final Map.Entry<String, Requirement> entry : requirements.entrySet()) {
            if (!entry.getValue().test(context)) {
                return false;
            }
        }
        return true;
    }

    private static boolean evaluateOr(
            final Map<String, Requirement> requirements,
            final RequirementContext context
    ) {
        for (final Map.Entry<String, Requirement> entry : requirements.entrySet()) {
            if (entry.getValue().test(context)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------
    // Deny action execution
    // ---------------------------------------------------------------

    private static void executeDenyActions(
            final List<MenuAction> denyActions,
            final RequirementContext context,
            final ClickType clickType
    ) {
        if (denyActions == null || denyActions.isEmpty()) {
            return;
        }

        final ActionContext actionContext = new ActionContext(
                context.viewer(),
                context.session(),
                context.slot(),
                clickType,
                null
        );

        for (final MenuAction action : denyActions) {
            action.execute(actionContext);
        }
    }
}
