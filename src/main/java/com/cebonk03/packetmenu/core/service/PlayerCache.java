package com.cebonk03.packetmenu.core.service;

import com.cebonk03.packetmenu.core.domain.MenuSession;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Three-tier player-specific cache backed by Caffeine and
 * {@link ConcurrentHashMap}.
 *
 * <p>Cache tiers:
 * <ol>
 *   <li><strong>Placeholder resolution</strong> &mdash; keyed by
 *       {@code playerId + "::" + templateString}, valued at {@link Component},
 *       30-second TTL, max 1000 entries.</li>
 *   <li><strong>Requirement result</strong> &mdash; keyed by
 *       {@code playerId + "::" + requirementId}, valued at {@link Boolean},
 *       5-second TTL, max 1000 entries.</li>
 *   <li><strong>Active menu session</strong> &mdash; a
 *       {@link ConcurrentHashMap} keyed by {@link UUID} with no eviction.</li>
 * </ol>
 *
 * <p>All public methods are thread-safe. Caffeine caches handle their own
 * synchronisation; the session map is backed by a {@link ConcurrentHashMap}.
 */
@NullMarked
public final class PlayerCache {

    private static final String SEP = "::";

    private final Cache<String, Component> placeholderCache;
    private final Cache<String, Boolean> requirementCache;
    private final Map<UUID, MenuSession> sessionCache;

    /** Creates a new cache with default settings for all three tiers. */
    public PlayerCache() {
        this.placeholderCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(1000)
                .build();

        this.requirementCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .maximumSize(1000)
                .build();

        this.sessionCache = new ConcurrentHashMap<>();
    }

    // ---------------------------------------------------------------
    // Placeholder cache
    // ---------------------------------------------------------------

    /**
     * Returns a previously cached resolved placeholder component, or
     * {@code null} if not present or expired.
     *
     * @param playerId the player's unique identifier
     * @param template the placeholder template string
     * @return the cached {@link Component}, or {@code null}
     */
    @Nullable
    public Component getCachedPlaceholder(final UUID playerId, final String template) {
        return placeholderCache.getIfPresent(key(playerId, template));
    }

    /**
     * Stores a resolved placeholder component in the cache.
     *
     * @param playerId the player's unique identifier
     * @param template the placeholder template string
     * @param resolved the resolved {@link Component}
     */
    public void cachePlaceholder(final UUID playerId, final String template, final Component resolved) {
        placeholderCache.put(key(playerId, template), resolved);
    }

    // ---------------------------------------------------------------
    // Requirement cache
    // ---------------------------------------------------------------

    /**
     * Returns a previously cached requirement evaluation result, or
     * {@code null} if not present or expired.
     *
     * @param playerId      the player's unique identifier
     * @param requirementId the requirement identifier
     * @return the cached {@link Boolean}, or {@code null}
     */
    @Nullable
    public Boolean getCachedRequirement(final UUID playerId, final String requirementId) {
        return requirementCache.getIfPresent(key(playerId, requirementId));
    }

    /**
     * Stores a requirement evaluation result in the cache.
     *
     * @param playerId      the player's unique identifier
     * @param requirementId the requirement identifier
     * @param result        the evaluation result
     */
    public void cacheRequirement(final UUID playerId, final String requirementId, final boolean result) {
        requirementCache.put(key(playerId, requirementId), result);
    }

    // ---------------------------------------------------------------
    // Session cache
    // ---------------------------------------------------------------

    /**
     * Returns the active {@link MenuSession} for the given player, or
     * {@code null} if the player does not have an active session.
     *
     * @param playerId the player's unique identifier
     * @return the active session, or {@code null}
     */
    @Nullable
    public MenuSession getActiveSession(final UUID playerId) {
        return sessionCache.get(playerId);
    }

    /**
     * Sets the active {@link MenuSession} for the given player.
     *
     * @param playerId the player's unique identifier
     * @param session  the session to associate
     */
    public void setActiveSession(final UUID playerId, final MenuSession session) {
        sessionCache.put(playerId, session);
    }

    /**
     * Removes the active {@link MenuSession} for the given player.
     *
     * @param playerId the player's unique identifier
     */
    public void removeActiveSession(final UUID playerId) {
        sessionCache.remove(playerId);
    }

    // ---------------------------------------------------------------
    // Bulk invalidation
    // ---------------------------------------------------------------

    /**
     * Invalidates ALL cached entries (placeholder, requirement, and
     * session) for the given player.
     *
     * <p>Intended to be called on player quit so stale data from a
     * previous session is never reused.
     *
     * @param playerId the player's unique identifier
     */
    public void invalidatePlayer(final UUID playerId) {
        final String prefix = playerId + SEP;
        placeholderCache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
        requirementCache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
        sessionCache.remove(playerId);
    }

    /**
     * Clears every cache tier entirely.
     *
     * <p>Intended to be called on plugin reload so that all players
     * start with a fresh cache state.
     */
    public void invalidateAll() {
        placeholderCache.invalidateAll();
        requirementCache.invalidateAll();
        sessionCache.clear();
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    private static String key(final UUID playerId, final String suffix) {
        return playerId + SEP + suffix;
    }
}
