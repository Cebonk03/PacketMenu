package com.cebonk03.packetmenu.adapter.paper;

import com.cebonk03.packetmenu.core.port.PlayerHandle;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

/**
 * Paper/Folia-aware implementation of {@link SchedulerPort}.
 *
 * <p>Delegates all scheduling to the Paper/Folia region schedulers
 * ({@code EntityScheduler}, {@code GlobalRegionScheduler}, {@code AsyncScheduler})
 * which are available on Paper 1.20+ and Folia. All returned
 * {@link ScheduledTask} instances are tracked so that they can be
 * cancelled during plugin shutdown via {@link #cancelAllTasks()}.
 */
@NullMarked
public final class PaperSchedulerAdapter implements SchedulerPort {

    private final JavaPlugin plugin;
    private final boolean folia;
    private final List<ScheduledTask> foliaTasks;
    private final ReentrantLock lock;

    /**
     * Creates a new adapter for the given plugin.
     *
     * @param plugin the owning plugin instance
     */
    public PaperSchedulerAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folia = Bukkit.getGlobalRegionScheduler() != null;
        this.foliaTasks = new ArrayList<>();
        this.lock = new ReentrantLock();
    }

    @Override
    public void runOnPlayer(PlayerHandle player, Runnable task) {
        final var bukkitPlayer = (Player) player.nativePlayer();
        final var scheduled = bukkitPlayer.getScheduler()
                .run(plugin, ignored -> task.run(), null);
        if (scheduled != null) {
            trackTask(scheduled);
        }
    }

    @Override
    public void runOnGlobal(Runnable task) {
        final var scheduled = Bukkit.getGlobalRegionScheduler()
                .run(plugin, ignored -> task.run());
        trackTask(scheduled);
    }

    @Override
    public void runAsync(Runnable task) {
        final var scheduled = Bukkit.getAsyncScheduler()
                .runNow(plugin, ignored -> task.run());
        trackTask(scheduled);
    }

    @Override
    public void runDelayedOnPlayer(PlayerHandle player, long ticks, Runnable task) {
        final var bukkitPlayer = (Player) player.nativePlayer();
        final var scheduled = bukkitPlayer.getScheduler()
                .runDelayed(plugin, ignored -> task.run(), () -> { }, ticks);
        if (scheduled != null) {
            trackTask(scheduled);
        }
    }

    @Override
    public void cancelAllTasks() {
        lock.lock();
        try {
            final var tasks = new ArrayList<>(foliaTasks);
            foliaTasks.clear();
            for (final var scheduledTask : tasks) {
                scheduledTask.cancel();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isFolia() {
        return folia;
    }

    private void trackTask(ScheduledTask task) {
        lock.lock();
        try {
            foliaTasks.add(task);
        } finally {
            lock.unlock();
        }
    }
}
