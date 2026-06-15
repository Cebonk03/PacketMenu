package com.cebonk03.packetmenu.adapter.paper;

import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.cebonk03.packetmenu.core.port.SessionManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * Stub implementation of {@link SessionManager}.
 *
 * <p>This minimal implementation provides the {@link #closeAll()} lifecycle
 * hook required during plugin shutdown. Full session tracking and packet-based
 * teardown logic will be added in a later task.
 */
@NullMarked
public final class PaperSessionManager implements SessionManager {

    private final List<MenuSession> sessions = new ArrayList<>();

    /**
     * Creates a new empty session manager.
     */
    public PaperSessionManager() {
    }

    @Override
    public void closeAll() {
        // Stub: will be fleshed out with packet-based close logic
        sessions.clear();
    }

    @Override
    public Collection<MenuSession> getActiveSessions() {
        return List.copyOf(sessions);
    }

    /**
     * Registers a new session (stub — real impl to follow).
     *
     * @param session the session to track
     */
    public void addSession(MenuSession session) {
        sessions.add(session);
    }

    /**
     * Removes a session from tracking (stub — real impl to follow).
     *
     * @param session the session to remove
     */
    public void removeSession(MenuSession session) {
        sessions.remove(session);
    }
}
