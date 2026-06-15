package unit.service;

import com.cebonk03.packetmenu.core.service.ContainerIdAllocator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ContainerIdAllocator}.
 */
class ContainerIdAllocatorTest {

    private final ContainerIdAllocator allocator = new ContainerIdAllocator();
    private final UUID playerA = UUID.randomUUID();
    private final UUID playerB = UUID.randomUUID();

    @Test
    void firstAllocationStartsAt101() {
        int id = allocator.allocate(playerA);
        assertEquals(101, id);
    }

    @Test
    void subsequentAllocationsIncrement() {
        int first = allocator.allocate(playerA);
        int second = allocator.allocate(playerA);
        int third = allocator.allocate(playerA);

        assertAll("sequential IDs",
            () -> assertEquals(101, first),
            () -> assertEquals(102, second),
            () -> assertEquals(103, third)
        );
    }

    @Test
    void differentPlayersGetIndependentSequences() {
        int a1 = allocator.allocate(playerA);
        int b1 = allocator.allocate(playerB);
        int a2 = allocator.allocate(playerA);
        int b2 = allocator.allocate(playerB);

        assertAll("independent per player",
            () -> assertEquals(101, a1),
            () -> assertEquals(101, b1),
            () -> assertEquals(102, a2),
            () -> assertEquals(102, b2)
        );
    }

    @Test
    void reclaimReturnsIdLifoOrder() {
        allocator.allocate(playerA); // 101
        allocator.allocate(playerA); // 102
        allocator.reclaim(playerA, 102);
        allocator.reclaim(playerA, 101);

        // LIFO: most recently reclaimed (101) comes first
        assertEquals(101, allocator.allocate(playerA));
        assertEquals(102, allocator.allocate(playerA));
    }

    @Test
    void reclaimThenAllocateUsesReclaimedBeforeNew() {
        allocator.allocate(playerA); // 101
        allocator.allocate(playerA); // 102
        allocator.reclaim(playerA, 101);

        assertEquals(101, allocator.allocate(playerA)); // reclaimed
        assertEquals(103, allocator.allocate(playerA)); // new (102 is still "in use")
    }

    @Test
    void reclaimMultipleForSamePlayer() {
        allocator.allocate(playerA); // 101
        allocator.allocate(playerA); // 102
        allocator.allocate(playerA); // 103
        allocator.reclaim(playerA, 103);
        allocator.reclaim(playerA, 102);

        assertEquals(102, allocator.allocate(playerA)); // LIFO
        assertEquals(103, allocator.allocate(playerA));
        assertEquals(104, allocator.allocate(playerA)); // new
    }

    @Test
    void reclaimForPlayerUnaffectedByOtherPlayer() {
        allocator.allocate(playerA); // 101
        allocator.allocate(playerB); // 101

        allocator.reclaim(playerA, 101);

        assertEquals(101, allocator.allocate(playerA)); // reclaimed
        assertEquals(102, allocator.allocate(playerB)); // fresh, unaffected
    }

    @Test
    void threadSafetyUnderConcurrentAllocation() throws InterruptedException {
        int threadCount = 10;
        int allocationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ConcurrentHashMap<Integer, Boolean> seen = new ConcurrentHashMap<>();
        AtomicInteger errors = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final UUID player = UUID.randomUUID();
            executor.submit(() -> {
                try {
                    for (int i = 0; i < allocationsPerThread; i++) {
                        int id = allocator.allocate(player);
                        if (seen.putIfAbsent(id, Boolean.TRUE) != null) {
                            errors.incrementAndGet(); // duplicate
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(0, errors.get(), "No duplicate IDs under concurrent allocation");
    }

    @Test
    void threadSafetyReclaimAndAllocate() throws InterruptedException {
        int threadCount = 5;
        int opsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        UUID player = UUID.randomUUID();
        List<Integer> allIds = new ArrayList<>();

        // Pre-allocate IDs then reclaim them
        for (int i = 0; i < 500; i++) {
            allIds.add(allocator.allocate(player));
        }
        for (int id : allIds) {
            allocator.reclaim(player, id);
        }

        AtomicInteger errors = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < opsPerThread; i++) {
                        int allocated = allocator.allocate(player);
                        if (allocated < 101) {
                            errors.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(0, errors.get(), "No invalid IDs under concurrent reclaim+allocate");
    }

    @Test
    void largeNumberOfAllocationsNoOverlap() {
        UUID player = UUID.randomUUID();
        int count = 10_000;

        for (int i = 0; i < count; i++) {
            allocator.allocate(player);
        }

        // After 10k allocs, the counter should have advanced by 10k
        // If we reclaim one and re-allocate, we should get the reclaimed one
        allocator.reclaim(player, 101);
        assertEquals(101, allocator.allocate(player));

        // Next without reclaim should be fresh
        assertEquals(101 + count + 1, allocator.allocate(player));
    }
}
