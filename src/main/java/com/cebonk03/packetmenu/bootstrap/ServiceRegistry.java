package com.cebonk03.packetmenu.bootstrap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Simple thread-safe dependency injection registry.
 *
 * <p>Services are registered by interface type and retrieved on demand.
 * The registry is populated during plugin {@code onEnable()} and cleared
 * during {@code onDisable()}.
 *
 * <p>This is intentionally <b>not</b> a full DI framework — no lifecycle
 * hooks, no qualifiers, no proxy generation. Just a type-safe map.
 */
@NullMarked
public final class ServiceRegistry {

    private final ConcurrentMap<Class<?>, Object> services = new ConcurrentHashMap<>();

    /**
     * Registers a service instance by its interface type.
     *
     * @param <T>      the service type
     * @param type     the interface class
     * @param instance the service implementation
     * @throws IllegalArgumentException if a service of this type is already registered
     */
    public <T> void register(Class<T> type, T instance) {
        final var previous = services.putIfAbsent(type, instance);
        if (previous != null) {
            throw new IllegalArgumentException(
                    "Service already registered for type: " + type.getName());
        }
    }

    /**
     * Retrieves a registered service instance.
     *
     * @param <T>  the service type
     * @param type the interface class
     * @return the service instance, or {@code null} if not registered
     */
    public <T> @Nullable T get(Class<T> type) {
        final var instance = services.get(type);
        return type.cast(instance);
    }

    /**
     * Checks whether a service type is registered.
     *
     * @param type the interface class
     * @return {@code true} if a service of this type is registered
     */
    public boolean has(Class<?> type) {
        return services.containsKey(type);
    }

    /**
     * Removes all registered services.
     *
     * <p>Called during plugin {@code onDisable()} to release references
     * and prevent memory leaks.
     */
    public void clear() {
        services.clear();
    }
}
