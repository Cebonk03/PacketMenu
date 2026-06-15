package com.cebonk03.packetmenu.core.service;

import com.cebonk03.packetmenu.core.domain.MenuAction;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;

/**
 * Thread-safe registry for action factories indexed by type name.
 *
 * <p>Each factory is a {@code Function<List<String>, MenuAction>} that accepts the
 * parsed argument list (strings split from the action definition) and returns a
 * ready-to-execute {@link MenuAction}.
 *
 * <p>Registration is explicit: action implementations call {@link #register(String,
 * Function)} during plugin initialisation, typically from their own static
 * initialisers or from a dedicated bootstrap pass.  The {@link
 * com.cebonk03.packetmenu.adapter.config.DeluxeActionParser} uses this registry to
 * dispatch parsed action strings to the correct factory.
 */
@NullMarked
public final class ActionRegistry {

    private final ConcurrentHashMap<String, Function<List<String>, MenuAction>> factories;

    /**
     * Creates an empty action registry.
     */
    public ActionRegistry() {
        this.factories = new ConcurrentHashMap<>();
    }

    /**
     * Registers a factory for the given action type.
     *
     * <p>If a factory for the same type already exists it is silently replaced.
     *
     * @param type    the action type identifier (e.g. {@code "player"}, {@code "console"})
     * @param factory the factory that produces a {@link MenuAction} from an argument list
     */
    public void register(
            final String type,
            final Function<List<String>, MenuAction> factory
    ) {
        factories.put(type, factory);
    }

    /**
     * Returns the factory registered for the given type, or {@code null} if
     * no factory has been registered.
     *
     * @param type the action type identifier
     * @return the registered factory, or {@code null}
     */
    public Function<List<String>, MenuAction> get(final String type) {
        return factories.get(type);
    }

    /**
     * Returns an unmodifiable snapshot of all currently registered factories.
     *
     * @return a map of type identifiers to factories
     */
    public Map<String, Function<List<String>, MenuAction>> getAll() {
        return Map.copyOf(factories);
    }
}
