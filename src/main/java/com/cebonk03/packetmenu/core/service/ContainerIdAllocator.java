package com.cebonk03.packetmenu.core.service;

import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.NullMarked;

/**
 * Thread-safe container ID allocator for virtual inventory windows.
 *
 * <p>Container IDs 0–100 are reserved for vanilla Minecraft windows (crafting table,
 * player inventory, chests, etc.). This allocator starts at 101 and supports reclaiming
 * IDs from closed windows for LIFO reuse, preventing ID exhaustion over long play sessions.
 *
 * <p>All public methods are thread-safe. The allocator uses an {@link AtomicInteger} for
 * the global ID counter and a {@link ConcurrentHashMap} of {@link ConcurrentLinkedDeque}
 * instances for per-player reclaim tracking.
 */
@NullMarked
public final class ContainerIdAllocator {

    private final AtomicInteger nextId;
    private final ConcurrentHashMap<UUID, Deque<Integer>> reclaims;

    /**
     * Creates a new allocator with the initial container ID set to 101.
     */
    public ContainerIdAllocator() {
        this.nextId = new AtomicInteger(101);
        this.reclaims = new ConcurrentHashMap<>();
    }

    /**
     * Allocates the next available container ID for the given player.
     *
     * <p>If the player has reclaimed IDs from previously closed windows, the most recently
     * reclaimed ID (LIFO) is returned first. Otherwise, a fresh ID is generated from the
     * global counter.
     *
     * @param playerId the unique identifier of the player
     * @return a container ID in the range [101, {@link Integer#MAX_VALUE})
     */
    public int allocate(UUID playerId) {
        final Deque<Integer> deque = reclaims.computeIfAbsent(
                playerId, k -> new ConcurrentLinkedDeque<>());
        final Integer reclaimed = deque.pollFirst();
        if (reclaimed != null) {
            return reclaimed;
        }
        return nextId.getAndIncrement();
    }

    /**
     * Reclaims a container ID for future reuse by the given player.
     *
     * <p>The reclaimed ID is pushed to the front of the player's reclaim deque so that the
     * next call to {@link #allocate(UUID)} for the same player will return it (LIFO order).
     *
     * @param playerId    the unique identifier of the player
     * @param containerId the container ID to reclaim
     */
    public void reclaim(UUID playerId, int containerId) {
        final Deque<Integer> deque = reclaims.computeIfAbsent(
                playerId, k -> new ConcurrentLinkedDeque<>());
        deque.addFirst(containerId);
    }
}
