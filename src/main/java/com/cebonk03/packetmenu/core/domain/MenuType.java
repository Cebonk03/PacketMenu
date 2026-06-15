package com.cebonk03.packetmenu.core.domain;

/**
 * Describes the visual layout of a menu and its corresponding protocol window type.
 *
 * @param size            the number of slots in this menu
 * @param protocolTypeId  the Minecraft protocol integer for the window type
 */
public enum MenuType {
    GENERIC_9x1(9, 0),
    GENERIC_9x2(18, 1),
    GENERIC_9x3(27, 2),
    GENERIC_9x4(36, 3),
    GENERIC_9x5(45, 4),
    GENERIC_9x6(54, 5),
    GENERIC_3x3(9, 6);

    private final int size;
    private final int protocolTypeId;

    MenuType(final int size, final int protocolTypeId) {
        this.size = size;
        this.protocolTypeId = protocolTypeId;
    }

    /**
     * Returns the number of slots in this menu type.
     *
     * @return slot count
     */
    public int size() {
        return size;
    }

    /**
     * Returns the protocol integer used to identify this window type on the wire.
     *
     * @return protocol type id
     */
    public int protocolTypeId() {
        return protocolTypeId;
    }
}
