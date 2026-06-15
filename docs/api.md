# Developer API

PacketMenu provides an extension API for registering custom actions and requirements. The API uses a registry pattern with functional interfaces.

## Architecture Overview

```
MenuAction (domain interface)
    ^
    |  implements
CustomAction (your class)
    |
    v
ActionRegistry (registers factory by type name)
    |
    v
DeluxeActionParser (parses [type] args <delay=N>)
    |
    v
Slot click handler (MenuFactory / Paginator)
```

```
Requirement (domain interface)
    ^
    |  implements
CustomRequirement (your class)
    |
    v
RequirementRegistry (registers factory by type name)
    |
    v
RequirementEvaluator (evaluates combined constraints)
```

## Custom Actions

### Step 1: Implement MenuAction

Create a class that implements the `MenuAction` interface from `com.cebonk03.packetmenu.core.domain`.

```java
package com.example.myplugin;

import com.cebonk03.packetmenu.core.domain.ActionContext;
import com.cebonk03.packetmenu.core.domain.ActionResult;
import com.cebonk03.packetmenu.core.domain.MenuAction;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class HealAction implements MenuAction {

    private final double health;

    public HealAction(final double health) {
        this.health = health;
    }

    @Override
    public ActionResult execute(final ActionContext context) {
        final var player = (org.bukkit.entity.Player) context.viewer().nativePlayer();
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + health));
        return new ActionResult.Success();
    }
}
```

### Step 2: Register the Action Factory

Register the action with the `ActionRegistry` using a type identifier string. The factory function receives the parsed argument list (strings split from the action definition).

```java
import com.cebonk03.packetmenu.core.service.ActionRegistry;

ActionRegistry registry = new ActionRegistry();
registry.register("heal", args -> {
    double amount = 10.0; // default
    if (!args.isEmpty()) {
        try {
            amount = Double.parseDouble(args.get(0));
        } catch (NumberFormatException e) {
            // use default
        }
    }
    return new HealAction(amount);
});
```

### Step 3: Wire into the Action Parser

Pass the `ActionRegistry` instance to the `DeluxeActionParser`. Custom registrations can happen before or after the parser is constructed. Registrations after construction override defaults.

```java
ActionRegistry registry = new ActionRegistry();
DeluxeActionParser parser = new DeluxeActionParser(registry);

// Register custom action
registry.register("heal", args -> new HealAction(parseAmount(args)));
```

Now `[heal] 5` in any action list will call `HealAction` with 5 health.

### Thread Safety

Actions that modify game state (give items, change health, teleport, etc.) MUST route execution through the `SchedulerPort` to maintain thread safety, especially on Folia.

```java
public final class SafeAction implements MenuAction {

    private final SchedulerPort scheduler;

    public SafeAction(final SchedulerPort scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public ActionResult execute(final ActionContext context) {
        scheduler.runOnPlayer(context.viewer(), () -> {
            // Game-state modifying code here
        });
        return new ActionResult.Success();
    }
}
```

### ActionResult Types

Actions can return three result types:

| Type | Meaning |
|---|---|
| `ActionResult.Success` | The action completed normally |
| `ActionResult.Failure(String reason)` | The action failed with a reason |
| `ActionResult.Delayed(long ticks, MenuAction next)` | Re-execute the next action after a delay |

### Dependency Injection

Actions receive their dependencies through the constructor. The factory function created during registration captures these dependencies.

```java
// Economy action that needs Vault
registry.register("custompay", args -> {
    double amount = Double.parseDouble(args.get(0));
    // EconomyPort is captured from the enclosing scope
    return new CustomPayAction(economyPort, amount, schedulerPort);
});
```

## Custom Requirements

### Step 1: Implement the Requirement Interface

Create a class that implements the `Requirement` functional interface from `com.cebonk03.packetmenu.core.domain`.

```java
package com.example.myplugin;

import com.cebonk03.packetmenu.core.domain.Requirement;
import com.cebonk03.packetmenu.core.domain.RequirementContext;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class BiomeRequirement implements Requirement {

    private final String biomeName;

    public BiomeRequirement(final String biomeName) {
        this.biomeName = biomeName;
    }

    @Override
    public boolean test(final RequirementContext context) {
        final var player = (org.bukkit.entity.Player) context.viewer().nativePlayer();
        return player.getLocation().getBlock().getBiome().getKey().value()
                .equalsIgnoreCase(biomeName);
    }
}
```

### Step 2: Register the Requirement Factory

Register the requirement with the `RequirementRegistry`.

```java
import com.cebonk03.packetmenu.core.service.RequirementRegistry;

RequirementRegistry registry = new RequirementRegistry();
registry.register("biome", args -> {
    String biome = args.isEmpty() ? "plains" : args.get(0);
    return new BiomeRequirement(biome);
});
```

### Step 3: Wire into the Requirement System

The `RequirementRegistry` is used by the config loader to parse requirement definitions. Custom registrations are picked up automatically.

```yaml
click_requirements:
  - requirement:
      type: biome
      biome: desert
    deny_commands:
      - "[message] &cYou must be in a desert!"
```

## Registry APIs

### ActionRegistry

```java
package com.cebonk03.packetmenu.core.service;

public final class ActionRegistry {
    public ActionRegistry();
    public void register(String type, Function<List<String>, MenuAction> factory);
    public Function<List<String>, MenuAction> get(String type);
    public Map<String, Function<List<String>, MenuAction>> getAll();
}
```

Thread-safe. Uses `ConcurrentHashMap` internally. If a factory for the same type already exists, it is silently replaced.

### RequirementRegistry

```java
package com.cebonk03.packetmenu.core.service;

public final class RequirementRegistry {
    public RequirementRegistry();
    public void register(String type, Function<List<String>, Requirement> factory);
    public Function<List<String>, Requirement> get(String type);
    public Map<String, Function<List<String>, Requirement>> getAll();
}
```

Same thread-safety and replacement semantics as ActionRegistry.

## Domain Interfaces

### MenuAction

```java
@FunctionalInterface
public interface MenuAction {
    ActionResult execute(ActionContext context);
}
```

### Requirement

```java
@FunctionalInterface
public interface Requirement {
    boolean test(RequirementContext context);
}
```

### ViewRequirement

```java
@FunctionalInterface
public interface ViewRequirement {
    boolean test(RequirementContext context);
}
```

### ClickHandler

```java
@FunctionalInterface
public interface ClickHandler {
    void handle(ClickContext context);
}
```

## Context Classes

### ActionContext

```java
public record ActionContext(
    PlayerHandle viewer,
    MenuSession session,
    @Nullable SlotItem slot,
    ClickType clickType,
    @Nullable MenuAction sourceAction
) {}
```

### RequirementContext

```java
public record RequirementContext(
    PlayerHandle viewer,
    MenuSession session,
    @Nullable SlotItem slot
) {}
```

### ClickContext

```java
public record ClickContext(
    MenuSession session,
    SlotItem slot,
    ClickType clickType,
    PlayerHandle viewer
) {}
```

## PlayerHandle API

The `PlayerHandle` interface abstracts the underlying Bukkit/Folia player object:

```java
public interface PlayerHandle {
    Object nativePlayer();               // Returns the Bukkit Player
    void sendMessage(Component message); // Sends a chat message
    boolean hasPermission(String perm);  // Checks a permission
    UUID getUniqueId();                  // Player UUID
    String getName();                    // Player name
}
```

Cast `nativePlayer()` to `org.bukkit.entity.Player` when you need Bukkit API access.

## Port Interfaces for Dependencies

```java
// Economy operations (Vault integration)
public interface EconomyPort {
    boolean has(PlayerHandle player, double amount);
    boolean withdraw(PlayerHandle player, double amount);
    boolean deposit(PlayerHandle player, double amount);
}

// Placeholder resolution (PlaceholderAPI integration)
public interface PlaceholderPort {
    Component resolve(Component component, PlayerHandle player);
    String resolveString(String raw, PlayerHandle player);
}

// Thread-safe scheduling (Paper/Folia)
public interface SchedulerPort {
    void runOnPlayer(PlayerHandle player, Runnable task);
    void runOnGlobal(Runnable task);
    void runAsync(Runnable task);
    void runDelayedOnPlayer(PlayerHandle player, long ticks, Runnable task);
    void cancelAllTasks();
    boolean isFolia();
}
```

## Example: Complete Integration

```java
package com.example.myplugin;

import com.cebonk03.packetmenu.bootstrap.PacketMenuPlugin;
import com.cebonk03.packetmenu.core.service.ActionRegistry;
import com.cebonk03.packetmenu.core.service.RequirementRegistry;
import com.cebonk03.packetmenu.core.port.SchedulerPort;
import org.bukkit.plugin.java.JavaPlugin;

public class MyExtension extends JavaPlugin {

    @Override
    public void onEnable() {
        // Get the PacketMenu instance
        PacketMenuPlugin packetMenu = (PacketMenuPlugin) getServer()
                .getPluginManager().getPlugin("PacketMenu");

        if (packetMenu == null) {
            getLogger().severe("PacketMenu not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register custom action
        ActionRegistry actionRegistry = new ActionRegistry();
        actionRegistry.register("heal", args -> {
            double amount = args.isEmpty() ? 10.0 : Double.parseDouble(args.get(0));
            return new HealAction(amount);
        });

        // Register custom requirement
        RequirementRegistry requirementRegistry = new RequirementRegistry();
        requirementRegistry.register("biome", args -> {
            String biome = args.isEmpty() ? "plains" : args.get(0);
            return new BiomeRequirement(biome);
        });

        getLogger().info("Custom actions and requirements registered!");
    }
}
```

## Best Practices

1. **Keep actions stateless where possible.** All state should be in the immutable fields captured at construction time.
2. **Use `@NullMarked` at the package level** and `@Nullable` on individual fields/methods that accept or return null.
3. **Route game-state mutations through SchedulerPort** for Folia compatibility. Actions that only send chat messages can run synchronously.
4. **Use `ActionResult.Failure` for graceful error handling** rather than throwing exceptions.
5. **Validate arguments in the factory function** and use sensible defaults where possible.
6. **Keep action execution fast.** Actions run on the server tick thread (or region thread). Long-running operations should use `runAsync`.
7. **Prefer constructor injection** over service locators.
