package unit.domain;

import com.cebonk03.packetmenu.core.domain.ItemStackSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
 * Unit tests for {@link ItemStackSnapshot}.
 */
class ItemStackSnapshotTest {

    private static final NamespacedKey STONE_KEY = NamespacedKey.minecraft("stone");
    private static final NamespacedKey DIAMOND_KEY = NamespacedKey.minecraft("diamond");

    private static final Component NAME = Component.text("Test Item");
    private static final Component LORE_LINE = Component.text("line1");

    @Test
    void twoIdenticalSnapshotsAreEqual() {
        var a = new ItemStackSnapshot(
            STONE_KEY, 1, NAME, List.of(LORE_LINE), Map.of(), Set.of(),
            null, 0, 0, null
        );
        var b = new ItemStackSnapshot(
            STONE_KEY, 1, NAME, List.of(LORE_LINE), Map.of(), Set.of(),
            null, 0, 0, null
        );

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void snapshotsWithDifferentMaterialKeyAreNotEqual() {
        var stone = new ItemStackSnapshot(
            STONE_KEY, 1, NAME, List.of(), Map.of(), Set.of(), null, 0, 0, null
        );
        var diamond = new ItemStackSnapshot(
            DIAMOND_KEY, 1, NAME, List.of(), Map.of(), Set.of(), null, 0, 0, null
        );

        assertNotEquals(stone, diamond);
    }

    @Test
    void snapshotsWithDifferentAmountAreNotEqual() {
        var one = new ItemStackSnapshot(
            STONE_KEY, 1, NAME, List.of(), Map.of(), Set.of(), null, 0, 0, null
        );
        var two = new ItemStackSnapshot(
            STONE_KEY, 2, NAME, List.of(), Map.of(), Set.of(), null, 0, 0, null
        );

        assertNotEquals(one, two);
    }

    @Test
    void snapshotsWithDifferentDisplayNameAreNotEqual() {
        var nameA = new ItemStackSnapshot(
            STONE_KEY, 1, Component.text("A"), List.of(), Map.of(), Set.of(),
            null, 0, 0, null
        );
        var nameB = new ItemStackSnapshot(
            STONE_KEY, 1, Component.text("B"), List.of(), Map.of(), Set.of(),
            null, 0, 0, null
        );

        assertNotEquals(nameA, nameB);
    }

    @Test
    void constructorDefensivelyCopiesLore() {
        var mutableLore = new ArrayList<Component>();
        mutableLore.add(Component.text("original"));
        var snap = new ItemStackSnapshot(
            STONE_KEY, 1, NAME, mutableLore, Map.of(), Set.of(), null, 0, 0, null
        );
        mutableLore.add(Component.text("appended"));

        assertEquals(1, snap.lore().size());
    }

    @Test
    void constructorDefensivelyCopiesEnchantments() {
        var mutableEnch = new HashMap<Enchantment, Integer>();
        var snap = new ItemStackSnapshot(
            STONE_KEY, 1, NAME, List.of(), mutableEnch, Set.of(), null, 0, 0, null
        );
        mutableEnch.put(null, 1); // should not affect snapshot

        assertEquals(0, snap.enchantments().size());
    }

    @Test
    void constructorDefensivelyCopiesItemFlags() {
        var mutableFlags = new HashSet<ItemFlag>();
        var snap = new ItemStackSnapshot(
            STONE_KEY, 1, NAME, List.of(), Map.of(), mutableFlags, null, 0, 0, null
        );
        mutableFlags.add(ItemFlag.HIDE_ENCHANTS);

        assertEquals(0, snap.itemFlags().size());
    }

    @Test
    void loreIsUnmodifiable() {
        var snap = new ItemStackSnapshot(
            STONE_KEY, 1, NAME, List.of(), Map.of(), Set.of(), null, 0, 0, null
        );
        assertThrows(UnsupportedOperationException.class, () -> snap.lore().add(Component.text("x")));
    }

    @Test
    void enchantmentsIsUnmodifiable() {
        var snap = new ItemStackSnapshot(
            STONE_KEY, 1, NAME, List.of(), Map.of(), Set.of(), null, 0, 0, null
        );
        assertThrows(UnsupportedOperationException.class,
            () -> snap.enchantments().put(null, 1));
    }

    @Test
    void itemFlagsIsUnmodifiable() {
        var snap = new ItemStackSnapshot(
            STONE_KEY, 1, NAME, List.of(), Map.of(), Set.of(), null, 0, 0, null
        );
        assertThrows(UnsupportedOperationException.class, () -> snap.itemFlags().add(ItemFlag.HIDE_ENCHANTS));
    }

    @Test
    void acceptsNullNbt() {
        var snap = new ItemStackSnapshot(
            STONE_KEY, 1, NAME, List.of(), Map.of(), Set.of(), null, 0, 0, null
        );
        assertNull(snap.nbt());
    }

    @Test
    void acceptsNullSkullTexture() {
        var snap = new ItemStackSnapshot(
            STONE_KEY, 1, NAME, List.of(), Map.of(), Set.of(), null, 0, 0, null
        );
        assertNull(snap.skullTexture());
    }

    @Test
    void acceptsAllPrimitiveDefaults() {
        var snap = new ItemStackSnapshot(
            STONE_KEY, 0, Component.empty(), List.of(), Map.of(), Set.of(),
            null, 0, 0, null
        );

        assertAll("default values",
            () -> assertEquals(0, snap.amount()),
            () -> assertEquals(0, snap.customModelData()),
            () -> assertEquals(0, snap.durability()),
            () -> assertTrue(snap.lore().isEmpty()),
            () -> assertTrue(snap.enchantments().isEmpty()),
            () -> assertTrue(snap.itemFlags().isEmpty())
        );
    }
}
