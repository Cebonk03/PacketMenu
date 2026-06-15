package unit.domain;

import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.cebonk03.packetmenu.core.domain.MenuType;
import com.cebonk03.packetmenu.core.domain.SlotItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MenuSession}.
 */
class MenuSessionTest {

    private static final NamespacedKey STONE_KEY = NamespacedKey.minecraft("stone");
    private static final NamespacedKey DIAMOND_KEY = NamespacedKey.minecraft("diamond");

    private static final ItemStackSnapshot BASE_ITEM = new ItemStackSnapshot(
        STONE_KEY, 1, Component.text("stone"),
        List.of(), Map.of(), Set.of(), null, 0, 0, null
    );

    private static final ItemStackSnapshot REPLACEMENT_ITEM = new ItemStackSnapshot(
        DIAMOND_KEY, 1, Component.text("diamond"),
        List.of(), Map.of(), Set.of(), null, 0, 0, null
    );

    private static final SlotItem SLOT_0 = new SlotItem(0, BASE_ITEM, null, null);

    private final MenuSession session = new MenuSession(
        101, "test_menu", MenuType.GENERIC_9x3, Component.text("Test Title"),
        List.of(SLOT_0), 0, true, null
    );

    @Test
    void constructorDefensivelyCopiesSlots() {
        var mutableSlots = new ArrayList<SlotItem>();
        mutableSlots.add(SLOT_0);

        var s = new MenuSession(200, "m", MenuType.GENERIC_9x1,
            Component.text("T"), mutableSlots, 0, false, null);

        mutableSlots.add(new SlotItem(1, BASE_ITEM, null, null));

        assertEquals(1, s.slots().size());
    }

    @Test
    void sessionSlotsIsUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> session.slots().add(SLOT_0));
    }

    @Test
    void withRevisionIncrementsRevisionIdOnly() {
        var updated = session.withRevision();

        assertAll("withRevision should only change revisionId",
            () -> assertEquals(session.revisionId() + 1, updated.revisionId()),
            () -> assertEquals(session.containerId(), updated.containerId()),
            () -> assertEquals(session.menuId(), updated.menuId()),
            () -> assertEquals(session.type(), updated.type()),
            () -> assertSame(session.title(), updated.title()),
            () -> assertEquals(session.slots(), updated.slots()),
            () -> assertEquals(session.notifyOnClose(), updated.notifyOnClose()),
            () -> assertEquals(session.parentMenuId(), updated.parentMenuId())
        );
    }

    @Test
    void withRevisionDoesNotMutateOriginal() {
        session.withRevision();
        assertEquals(0, session.revisionId());
    }

    @Test
    void withItemReplacesExistingSlotItem() {
        var updated = session.withItem(0, REPLACEMENT_ITEM);

        assertAll("slot 0 item should be replaced",
            () -> assertSame(REPLACEMENT_ITEM, updated.slots().get(0).item()),
            () -> assertEquals(1, updated.slots().size()),
            () -> assertNotSame(session, updated)
        );
    }

    @Test
    void withItemPreservesClickHandlerAndViewRequirement() {
        var updated = session.withItem(0, REPLACEMENT_ITEM);

        assertAll("clickHandler and viewRequirement should be preserved",
            () -> assertNull(updated.slots().get(0).clickHandler()),
            () -> assertNull(updated.slots().get(0).viewRequirement())
        );
    }

    @Test
    void withItemAppendsNewSlotWhenIndexNotFound() {
        var updated = session.withItem(99, REPLACEMENT_ITEM);

        assertAll("new slot should be appended",
            () -> assertEquals(2, updated.slots().size()),
            () -> assertEquals(0, updated.slots().get(0).slot()),
            () -> assertEquals(99, updated.slots().get(1).slot()),
            () -> assertSame(REPLACEMENT_ITEM, updated.slots().get(1).item()),
            () -> assertNull(updated.slots().get(1).clickHandler()),
            () -> assertNull(updated.slots().get(1).viewRequirement())
        );
    }

    @Test
    void withItemDoesNotMutateOriginalSession() {
        session.withItem(0, REPLACEMENT_ITEM);
        assertSame(BASE_ITEM, session.slots().get(0).item());
    }

    @Test
    void withItemMultipleCallsAllIndependent() {
        var secondItem = new ItemStackSnapshot(
            NamespacedKey.minecraft("iron_ingot"), 1, Component.text("iron"),
            List.of(), Map.of(), Set.of(), null, 0, 0, null
        );

        var updated1 = session.withItem(0, REPLACEMENT_ITEM);
        var updated2 = updated1.withItem(5, secondItem);

        assertAll("each withItem call returns independent session",
            () -> assertEquals(1, updated1.slots().size()),
            () -> assertEquals(2, updated2.slots().size()),
            () -> assertSame(REPLACEMENT_ITEM, updated2.slots().get(0).item()),
            () -> assertEquals(5, updated2.slots().get(1).slot())
        );
    }

    @Test
    void constructorAcceptsNullParentMenuId() {
        var s = new MenuSession(102, "sub", MenuType.GENERIC_9x1,
            Component.text("Sub"), List.of(), 0, false, null);
        assertNull(s.parentMenuId());
    }

    @Test
    void constructorAcceptsZeroRevisionId() {
        var s = new MenuSession(103, "zero", MenuType.GENERIC_9x6,
            Component.text("Zero"), List.of(), 0, false, null);
        assertEquals(0, s.revisionId());
    }

    @Test
    void constructorAcceptsEmptySlots() {
        var s = new MenuSession(104, "empty", MenuType.GENERIC_9x3,
            Component.text("Empty"), List.of(), 5, true, null);
        assertTrue(s.slots().isEmpty());
    }

    @Test
    void withRevisionCanBeCalledMultipleTimes() {
        var v1 = session.withRevision();
        var v2 = v1.withRevision();
        var v3 = v2.withRevision();

        assertAll("revision should increment sequentially",
            () -> assertEquals(1, v1.revisionId()),
            () -> assertEquals(2, v2.revisionId()),
            () -> assertEquals(3, v3.revisionId())
        );
    }
}
