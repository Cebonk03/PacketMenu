package com.cebonk03.packetmenu.core.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodic metrics logger for PacketMenu runtime statistics.
 *
 * <p>Tracks three counters:
 * <ul>
 *   <li>{@code activeViewers} — number of players currently viewing a menu</li>
 *   <li>{@code menusLoaded} — total number of menu sessions created</li>
 *   <li>{@code packetsSent} — total number of packets dispatched</li>
 * </ul>
 *
 * <p>Every 5 minutes the current values are logged at INFO level. The counters
 * themselves are backed by {@link AtomicInteger} / {@link AtomicLong} so they
 * can be safely incremented from any thread.
 *
 * <p>Lifecycle is managed via {@link #start()} and {@link #stop()} / {@link #close()}.
 */
@NullMarked
public final class MetricsLogger implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsLogger.class);
    private static final long LOG_INTERVAL_SECONDS = 300;

    private final AtomicInteger activeViewers = new AtomicInteger(0);
    private final AtomicLong menusLoaded = new AtomicLong(0);
    private final AtomicLong packetsSent = new AtomicLong(0);

    private final ScheduledExecutorService executor;

    /** Creates a new metrics logger. The logging task is not started until {@link #start()} is called. */
    public MetricsLogger() {
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "PacketMenu-Metrics");
            t.setDaemon(true);
            return t;
        });
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────────

    /**
     * Starts periodic metrics logging.
     *
     * <p>Safe to call multiple times; only the first call schedules the task.
     */
    public void start() {
        executor.scheduleAtFixedRate(
                this::logMetrics,
                LOG_INTERVAL_SECONDS,
                LOG_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        LOGGER.info("Metrics logging started (interval={}s)", LOG_INTERVAL_SECONDS);
    }

    /**
     * Stops periodic metrics logging and shuts down the internal executor.
     *
     * <p>After this call no further metrics will be logged. Already-enqueued
     * log tasks are cancelled.
     */
    public void stop() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                LOGGER.warn("Metrics executor did not terminate within 2 seconds");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        stop();
    }

    // ── Counter accessors ────────────────────────────────────────────────────────

    /** Increments and returns the active-viewer count. */
    public int incrementActiveViewers() {
        return activeViewers.incrementAndGet();
    }

    /** Decrements and returns the active-viewer count. */
    public int decrementActiveViewers() {
        return activeViewers.decrementAndGet();
    }

    /** Increments and returns the total menus-loaded counter. */
    public long incrementMenusLoaded() {
        return menusLoaded.incrementAndGet();
    }

    /** Increments and returns the total packets-sent counter. */
    public long incrementPacketsSent() {
        return packetsSent.incrementAndGet();
    }

    /** Returns the current active-viewer count (read-only snapshot). */
    public int getActiveViewers() {
        return activeViewers.get();
    }

    /** Returns the total menus loaded so far. */
    public long getMenusLoaded() {
        return menusLoaded.get();
    }

    /** Returns the total packets sent so far. */
    public long getPacketsSent() {
        return packetsSent.get();
    }

    // ── Internal logging ─────────────────────────────────────────────────────────

    private void logMetrics() {
        LOGGER.info("[Metrics] activeViewers={} menusLoaded={} packetsSent={}",
                activeViewers.get(), menusLoaded.get(), packetsSent.get());
    }
}
