package com.cebonk03.packetmenu.core.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.jspecify.annotations.Nullable;

/**
 * An immutable snapshot of an item's visual and functional data, free from
 * any Bukkit {@code ItemStack} reference.
 *
 * @param materialKey     the {@link NamespacedKey} identifying the material
 * @param amount          the stack size
 * @param displayName     the custom display name ({@link Component})
 * @param lore            the lore lines
 * @param enchantments    the enchantment map (enchantment → level)
 * @param itemFlags       the set of active {@link ItemFlag}s
 * @param nbt             optional raw NBT string (e.g. from a model or data component)
 * @param customModelData the custom model data integer
 * @param durability      the durability / damage value
 * @param skullTexture    optional player skull texture value (base64)
 */
public record ItemStackSnapshot(
    NamespacedKey materialKey,
    int amount,
    Component displayName,
    List<Component> lore,
    Map<Enchantment, Integer> enchantments,
    Set<ItemFlag> itemFlags,
    @Nullable String nbt,
    int customModelData,
    int durability,
    @Nullable String skullTexture
) {

    /**
     * Compact constructor that defensively copies all mutable collection parameters.
     */
    public ItemStackSnapshot {
        lore = List.copyOf(lore);
        enchantments = Map.copyOf(enchantments);
        itemFlags = Set.copyOf(itemFlags);
    }
}
