package com.cebonk03.packetmenu.adapter.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.jspecify.annotations.NullMarked;

/**
 * Provides version detection capabilities using the PacketEvents {@link ServerVersion} API.
 *
 * <p>All version checks are based on protocol version comparison rather than hard-coded
 * version strings, ensuring forward compatibility with future Minecraft releases.
 */
@NullMarked
public final class VersionCapabilities {

    private final ServerVersion serverVersion;

    /**
     * Creates a new {@code VersionCapabilities} instance that reads the server version
     * from the PacketEvents API at construction time.
     */
    public VersionCapabilities() {
        this.serverVersion = PacketEvents.getAPI().getServerManager().getVersion();
    }

    /**
     * Creates a new {@code VersionCapabilities} instance with the given server version.
     *
     * <p>This constructor is useful for testing with a mock or fixed version.
     *
     * @param serverVersion the server version to use
     */
    VersionCapabilities(ServerVersion serverVersion) {
        this.serverVersion = serverVersion;
    }

    /**
     * Returns whether the server supports the modern component serialization format
     * introduced in Minecraft 1.20.5.
     *
     * <p>In 1.20.5+ Minecraft changed how {@code Component} objects are serialized in
     * packets. This method helps determine which serialization path to use when encoding
     * and decoding chat and inventory components.
     *
     * @return {@code true} if the server is running Minecraft 1.20.5 or later
     */
    public boolean supportsModernComponentFormat() {
        return serverVersion.isNewerThanOrEquals(ServerVersion.V_1_20_5);
    }

    /**
     * Returns whether the server supports the item component system introduced in
     * Minecraft 1.20.5.
     *
     * <p>The item component system replaced the legacy NBT-based item data system. Servers
     * running 1.20.5 or later use data components ({@code DataComponentPatch}) for item
     * metadata instead of raw NBT.
     *
     * @return {@code true} if the server is running Minecraft 1.20.5 or later
     */
    public boolean supportsItemComponents() {
        return serverVersion.isNewerThanOrEquals(ServerVersion.V_1_20_5);
    }

    /**
     * Returns the protocol version of the server.
     *
     * @return the protocol version number (e.g., {@code 766} for Minecraft 1.20.5/1.20.6)
     */
    public int getProtocolVersion() {
        return serverVersion.getProtocolVersion();
    }

    /**
     * Returns the major version number parsed from the server's release name.
     *
     * <p>For Minecraft {@code 1.20.5} this returns {@code 1}. For Minecraft {@code 26.1.2}
     * this returns {@code 26}.
     *
     * @return the major version component
     */
    public int getMajorVersion() {
        final String releaseName = serverVersion.getReleaseName();
        final String[] parts = releaseName.split("\\.");
        return Integer.parseInt(parts[0]);
    }

    /**
     * Returns the minor version number parsed from the server's release name.
     *
     * <p>For Minecraft {@code 1.20.5} this returns {@code 20}. For Minecraft {@code 26.1.2}
     * this returns {@code 1}.
     *
     * @return the minor version component
     */
    public int getMinorVersion() {
        final String releaseName = serverVersion.getReleaseName();
        final String[] parts = releaseName.split("\\.");
        return Integer.parseInt(parts[1]);
    }
}
