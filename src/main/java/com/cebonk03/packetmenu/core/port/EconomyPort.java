package com.cebonk03.packetmenu.core.port;

/**
 * Port for economy operations.
 *
 * <p>Implementations wrap an economy provider (e.g. Vault) and expose balance
 * checks and fund transfers without leaking the underlying API into the core.
 */
public interface EconomyPort {

    /**
     * Checks whether the player has at least the given amount.
     *
     * @param player the target player
     * @param amount the minimum amount required
     * @return {@code true} if the player has sufficient funds
     */
    boolean has(PlayerHandle player, double amount);

    /**
     * Withdraws the given amount from the player's balance.
     *
     * @param player the target player
     * @param amount the amount to withdraw
     * @return {@code true} if the withdrawal succeeded
     */
    boolean withdraw(PlayerHandle player, double amount);

    /**
     * Deposits the given amount into the player's balance.
     *
     * @param player the target player
     * @param amount the amount to deposit
     * @return {@code true} if the deposit succeeded
     */
    boolean deposit(PlayerHandle player, double amount);
}
