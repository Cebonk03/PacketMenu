package com.cebonk03.packetmenu.core.port;

import com.cebonk03.packetmenu.core.domain.MenuSession;
import java.util.Collection;

/**
 * Manages active menu sessions.
 *
 * <p>Tracks which players currently have an open PacketMenu session and
 * provides lifecycle operations such as closing all sessions at once
 * (used during plugin shutdown).
 */
public interface SessionManager {

    /**
     * Closes all active menu sessions gracefully.
     *
     * <p>Each session is sent a close packet and removed from the internal
     * tracking map. Safe to call during plugin {@code onDisable()}.
     */
    void closeAll();

    /**
     * Returns a snapshot of all currently active sessions.
     *
     * @return unmodifiable collection of active sessions
     */
    Collection<MenuSession> getActiveSessions();
}
