package com.cebonk03.packetmenu.core.service.requirements;

import com.cebonk03.packetmenu.core.domain.Requirement;
import com.cebonk03.packetmenu.core.domain.RequirementContext;
import com.cebonk03.packetmenu.core.port.EconomyPort;

/**
 * A requirement that passes when the viewer has at least the specified amount
 * of in-game currency.
 *
 * <p>Delegates to {@link EconomyPort#has(com.cebonk03.packetmenu.core.port.PlayerHandle, double)}.
 */
public final class HasMoneyRequirement implements Requirement {

    private final EconomyPort economy;
    private final double amount;

    /**
     * Creates a new {@code HasMoneyRequirement}.
     *
     * @param economy the economy port
     * @param amount  the minimum amount of currency required
     */
    public HasMoneyRequirement(final EconomyPort economy, final double amount) {
        this.economy = economy;
        this.amount = amount;
    }

    @Override
    public boolean test(final RequirementContext context) {
        return economy.has(context.viewer(), amount);
    }
}
