package com.cebonk03.packetmenu.core.service;

import com.cebonk03.packetmenu.core.domain.Requirement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;

/**
 * Thread-safe registry for requirement factories indexed by type name.
 *
 * <p>Each factory is a {@code Function<List<String>, Requirement>} that accepts the
 * parsed argument list (strings split from the configuration definition) and returns a
 * {@link Requirement} predicate.
 *
 * <p>Registration is explicit: requirement implementations call {@link #register(String,
 * Function)} during plugin initialisation.  The requirement-parsing infrastructure uses
 * this registry to dispatch requirement definitions to the correct factory.
 */
@NullMarked
public final class RequirementRegistry {

    private final ConcurrentHashMap<String, Function<List<String>, Requirement>> factories;

    /**
     * Creates an empty requirement registry.
     */
    public RequirementRegistry() {
        this.factories = new ConcurrentHashMap<>();
    }

    /**
     * Registers a factory for the given requirement type.
     *
     * <p>If a factory for the same type already exists it is silently replaced.
     *
     * @param type    the requirement type identifier (e.g. {@code "has permission"},
     *                {@code "has money"}, {@code "string equals"})
     * @param factory the factory that produces a {@link Requirement} from an argument list
     */
    public void register(
            final String type,
            final Function<List<String>, Requirement> factory
    ) {
        factories.put(type, factory);
    }

    /**
     * Returns the factory registered for the given type, or {@code null} if
     * no factory has been registered.
     *
     * @param type the requirement type identifier
     * @return the registered factory, or {@code null}
     */
    public Function<List<String>, Requirement> get(final String type) {
        return factories.get(type);
    }

    /**
     * Returns an unmodifiable snapshot of all currently registered factories.
     *
     * @return a map of type identifiers to factories
     */
    public Map<String, Function<List<String>, Requirement>> getAll() {
        return Map.copyOf(factories);
    }
}
