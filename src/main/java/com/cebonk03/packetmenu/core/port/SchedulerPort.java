package com.cebonk03.packetmenu.core.port;

/**
 * Port for scheduling tasks on the Minecraft server.
 *
 * <p>Implementations abstract over the server's scheduler (Paper / Folia),
 * providing both synchronous region-aware execution and asynchronous execution.
 */
public interface SchedulerPort {

    /**
     * Runs a task on the thread assigned to the given player (Folia region
     * scheduling; on Paper this falls back to the global server thread).
     *
     * @param player the target player whose region executes the task
     * @param task   the task to run
     */
    void runOnPlayer(PlayerHandle player, Runnable task);

    /**
     * Runs a task on the global server tick thread.
     *
     * @param task the task to run
     */
    void runOnGlobal(Runnable task);

    /**
     * Runs a task asynchronously, off the server tick thread.
     *
     * @param task the task to run
     */
    void runAsync(Runnable task);

    /**
     * Runs a task on the player's region thread after a delay.
     *
     * @param player the target player
     * @param ticks  delay in server ticks before execution
     * @param task   the task to run
     */
    void runDelayedOnPlayer(PlayerHandle player, long ticks, Runnable task);

    /**
     * Cancels all scheduled tasks managed by this port.
     *
     * <p>Called during plugin {@code onDisable()} to prevent stale task
     * execution after the plugin has been disabled.
     */
    void cancelAllTasks();

    /**
     * Returns whether the underlying server runtime is Folia.
     *
     * <p>Folia uses region-based threading; Paper uses a single server
     * thread. Behaviour of scheduler methods may differ between the two.
     *
     * @return {@code true} if running on Folia, {@code false} for Paper
     */
    boolean isFolia();
}
