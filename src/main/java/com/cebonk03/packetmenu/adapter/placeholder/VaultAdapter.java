package com.cebonk03.packetmenu.adapter.placeholder;

import com.cebonk03.packetmenu.core.port.EconomyPort;
import com.cebonk03.packetmenu.core.port.PlayerHandle;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * {@link EconomyPort} implementation backed by Vault.
 *
 * <p>Detects the presence of Vault at runtime via
 * {@link Bukkit#getPluginManager()} and retrieves the registered
 * {@link Economy} provider from the Bukkit services manager. If Vault
 * is absent or no economy provider is registered, all methods return
 * {@code false} (graceful degradation).
 *
 * <p>Economy operations are executed directly on the calling thread.
 * If the caller requires off-thread execution, it should use
 * {@link SchedulerPort#runAsync(Runnable)} to invoke the adapter
 * methods.
 */
@NullMarked
public final class VaultAdapter implements EconomyPort {

    private static final boolean VAULT_PRESENT;

    static {
        VAULT_PRESENT = Bukkit.getPluginManager().getPlugin("Vault") != null;
    }

    private final @Nullable Economy economy;
    private final SchedulerPort scheduler;

    /**
     * Creates a new Vault adapter.
     *
     * @param scheduler the scheduler port for potential async coordination
     */
    public VaultAdapter(final SchedulerPort scheduler) {
        this.scheduler = scheduler;
        if (VAULT_PRESENT) {
            final var rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            this.economy = rsp != null ? rsp.getProvider() : null;
        } else {
            this.economy = null;
        }
    }

    @Override
    public boolean has(final PlayerHandle player, final double amount) {
        if (economy == null) {
            return false;
        }
        try {
            return economy.has((Player) player.nativePlayer(), amount);
        } catch (final Exception e) {
            return false;
        }
    }

    @Override
    public boolean withdraw(final PlayerHandle player, final double amount) {
        if (economy == null) {
            return false;
        }
        try {
            final EconomyResponse response = economy.withdrawPlayer(
                    (Player) player.nativePlayer(), amount
            );
            return response.transactionSuccess();
        } catch (final Exception e) {
            return false;
        }
    }

    @Override
    public boolean deposit(final PlayerHandle player, final double amount) {
        if (economy == null) {
            return false;
        }
        try {
            final EconomyResponse response = economy.depositPlayer(
                    (Player) player.nativePlayer(), amount
            );
            return response.transactionSuccess();
        } catch (final Exception e) {
            return false;
        }
    }
}
