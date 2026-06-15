package com.cebonk03.packetmenu.core.service;

import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.NullMarked;

/**
 * Canonical instance pool for {@link ItemStackSnapshot} deduplication.
 *
 * <p>Identical snapshot instances are interned so that subsequent equality
 * checks can use reference identity and the memory footprint of repeated
 * identical items is reduced. This is analogous to {@link String#intern()}
 * but scoped to item snapshots.
 *
 * <p>The pool is backed by a {@link ConcurrentHashMap} and is thread-safe.
 * A monotonic counter tracks how many unique items have been pooled for
 * metrics purposes.
 */
@NullMarked
public final class ItemStackSnapshotPool {

    private final ConcurrentHashMap<ItemStackSnapshot, ItemStackSnapshot> pool =
        new ConcurrentHashMap<>();

    private final AtomicLong itemsPooled = new AtomicLong();

    /**
     * Returns the canonical (pooled) instance for the given snapshot,
     * inserting it into the pool if it is not already present.
     *
     * @param snapshot the snapshot to intern; must not be {@code null}
     * @return the canonical instance
     */
    public ItemStackSnapshot canonical(final ItemStackSnapshot snapshot) {
        final ItemStackSnapshot existing = pool.putIfAbsent(snapshot, snapshot);
        if (existing == null) {
            itemsPooled.incrementAndGet();
            return snapshot;
        }
        return existing;
    }

    /**
     * Returns the number of unique snapshot instances currently held in
     * the pool.
     *
     * @return current pool size
     */
    public int size() {
        return pool.size();
    }

    /**
     * Returns the total number of unique snapshots that have been pooled
     * since this instance was created (a monotonic count that never
     * decreases).
     *
     * @return total items pooled since creation
     */
    public long itemsPooledCount() {
        return itemsPooled.get();
    }
}
