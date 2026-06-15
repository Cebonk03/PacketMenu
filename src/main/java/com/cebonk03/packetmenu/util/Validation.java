package com.cebonk03.packetmenu.util;

import com.cebonk03.packetmenu.core.domain.MenuType;
import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;

/**
 * Utility methods for validating input parameters.
 *
 * @author Utility Classes
 * @since 1.0.0
 */
@NullMarked
public final class Validation {

    private Validation() {
        // Private constructor to prevent instantiation
    }

    /**
     * Validates that a slot index is within the bounds of a menu type.
     *
     * @param slot  the slot index to validate (0-based)
     * @param type  the menu type to validate against
     * @throws IllegalArgumentException if slot is out of bounds
     */
    public static void checkSlotIndex(final int slot, final MenuType type) {
        if (slot < 0 || slot >= type.size()) {
            throw new IllegalArgumentException(
                "Slot index %d is out of bounds for menu type %s (size: %d, valid range: 0-%d)"
                    .formatted(slot, type, type.size(), type.size() - 1)
            );
        }
    }

    /**
     * Validates that an object is not null.
     *
     * @param obj   the object to validate
     * @param name  the name of the parameter for error message
     * @throws NullPointerException if obj is null
     */
    public static void checkNotNull(final Object obj, final String name) {
        if (obj == null) {
            throw new NullPointerException("Parameter '%s' cannot be null".formatted(name));
        }
    }

    /**
     * Validates that a value is positive (greater than 0).
     *
     * @param value the value to validate
     * @param name  the name of the parameter for error message
     * @throws IllegalArgumentException if value is <= 0
     */
    public static void checkPositive(final int value, final String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                "Parameter '%s' must be positive, got: %d".formatted(name, value)
            );
        }
    }

    /**
     * Validates that a material name exists in the current server version.
     *
     * @param materialName the material name to validate
     * @throws IllegalArgumentException if material does not exist
     */
    public static void checkMaterial(final String materialName) {
        if (materialName == null || materialName.isBlank()) {
            throw new IllegalArgumentException("Material name cannot be null or blank");
        }

        if (Material.matchMaterial(materialName) == null) {
            throw new IllegalArgumentException(
                "Material '%s' does not exist in this server version".formatted(materialName)
            );
        }
    }
}