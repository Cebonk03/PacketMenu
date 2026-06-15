# PacketMenu - Learnings

## Project Info
- Greenfield project: empty repo with README.md only
- Minecraft Paper/Folia plugin (1.21.4 - 26.1.2)
- Packet-based GUI (no Bukkit Inventory API)
- PacketEvents 2.7.0 (external dep, not shaded)
- Hexagonal architecture (domain -> core -> adapter)
- Java 21 --release 21 compile target
- Gradle 9.5.1 wrapper
- Build/tests run on GitHub Actions only (no local compilation)
- Package: com.cebonk03.packetmenu
- CI/CD: Task 2 complete — 3 workflows (.github/workflows/)
  - build.yml: Java 21/25 × MC 1.21.4/1.21.7/1.21.11/26.1/26.1.2 matrix, excludes Java21+26.x
  - quality.yml: Checkstyle, SpotBugs, ArchUnit tests on Java 21
  - release.yml: Tag-based v* → GitHub Releases with CHANGELOG body
  - Uses setup-java@v4 (temurin), setup-gradle@v4, upload-artifact@v4
  - ORG_GRADLE_PROJECT_configurationCache=false + --no-configuration-cache flag
  - MC version passed as -PminecraftVersion property (handled by build system)
- Code quality (Task 3): Checkstyle 10.20.2 + SpotBugs 4.8.6 wired
  - config/checkstyle/checkstyle.xml: Minecraft-adapted Sun/Google hybrid
    - Max line length 120, Javadoc on public API only, no wildcard imports
    - Final on params/locals, require @Override, maxErrors=0, maxWarnings=0
    - 4-space indent, K&R brace style, exclude generated sources
  - config/spotbugs/exclude.xml: EI_EXPOSE_REP, EI_EXPOSE_REP2, SE_NO_SERIALVERSIONID, RCN_REDUNDANT_NULLCHECK
  - build.gradle.kts: checkstyle/spotbugs wired with toolVersion, SpotBugs effort=MAX, reportLevel=MEDIUM

## Build System (Task 1)
- Gradle 9.5.1 wrapper, Java 21 toolchain with --release 21
- settings.gradle.kts: rootProject.name = "PacketMenu"
- gradle.properties: -Xmx2G, config-cache=false, parallel=true, UTF-8
- Version catalog (gradle/libs.versions.toml) with ALL pins
- Plugins: Shadow 9.4.2 (com.gradleup.shadow), plugin-yml 0.6.0 (net.minecrell.plugin-yml.paper), SpotBugs 6.0.27
- Repos: mavenCentral, PaperMC, CodeMC (for PacketEvents)
- Paper API 1.21.4-R0.1-SNAPSHOT (compileOnly)
- PacketEvents 2.7.0 (compileOnly, external plugin dep, NOT shaded)
- Configurate-YAML 4.1.2 + Caffeine 3.1.8 (implementation, SHADED)
  - Relocated: org.spongepowered.configurate → com.cebonk03.packetmenu.libs.configurate
  - Relocated: com.github.benmanes.caffeine → com.cebonk03.packetmenu.libs.caffeine
- JSpecify 1.0.0 (compileOnly annotation)
- Test deps: JUnit 5.11.4 (BOM), Mockito 5.14.2, ArchUnit 1.3.0, MockBukkit 2.221.0
- Paper plugin generated via plugin-yml DSL: main=com.cebonk03.packetmenu.bootstrap.PacketMenuPlugin
- foliaSupported=true, server dep on PacketEvents (BEFORE, required)
- Shadow 8.3.6 had ASM mapValue() issue with Java 21; upgraded to 9.4.2
- PacketEvents repo is CodeMC (not unnamed/sonatype); artifact is packetevents-spigot

## Domain Model Records (Task 5)
- 16 files created under `core/domain/`:
  - **Enums**: `ClickType` (8 click variants), `MenuType` (7 layouts with size+protocolTypeId)
  - **Records**: `ItemStackSnapshot`, `SlotItem`, `SlotTemplate`, `MenuSession`, `MenuTemplate`
  - **Context carriers**: `ClickContext`, `RequirementContext`, `ActionContext`
  - **Functional interfaces**: `ClickHandler`, `ViewRequirement`, `MenuAction`, `Requirement`
  - **Sealed interface**: `ActionResult` (permits Success, Failure, Delayed)
  - **Stubs**: `PlayerHandle` (minimal domain-level handle)
- Immutable — `List.copyOf()`, `Map.copyOf()`, `Set.copyOf()` in compact constructors
- `@NullMarked` at package level (already set); explicit `@Nullable` on optional fields
- `MenuSession.withRevision()` → increments revisionId by 1
- `MenuSession.withItem(int, ItemStackSnapshot)` → replaces-or-appends slot, returns new instance
- No Bukkit-specific types beyond `Enchantment`, `ItemFlag`, `NamespacedKey`
- No PacketEvents imports anywhere in domain
- All Adventure Component imports from `net.kyori.adventure.text.Component`
- Pre-existing MenuSession.java (UUID stub) overwritten with full implementation
- Compilation verified: 0 errors from domain files (errors in PaperSchedulerAdapter/SchedulerPort are pre-existing)
- Blocks Tasks 6, 7, 8 (Wave 2) and downstream Wave 3/4

## PacketEventsComposer (Task 9)
- Created `adapter/packetevents/PacketEventsComposer.java` implementing `core.port.PacketComposer`
- Uses PacketEvents 2.7.0 wrapper classes for all 4 operations:
  - `openWindow`: `WrapperPlayServerOpenWindow(int, int, Component)` — containerId, protocolTypeId, Adventure title
  - `sendItems`: `WrapperPlayServerWindowItems(int, int, List<ItemStack>, @Nullable ItemStack)` — windowID, stateID/revisionId, items, null carried
  - `setSlot`: `WrapperPlayServerSetSlot(int, int, int, ItemStack)` — windowID, stateID=0, slot, item
  - `closeWindow`: `WrapperPlayServerCloseWindow(int)` — containerId
- **EntityScheduler routing**: All packet sends go through `player.getScheduler().run(plugin, task, null)` → `PacketEvents.getAPI().getPlayerManager().sendPacket()`
- **Item conversion**: Uses PacketEvents' component-based `ItemStack.builder()`:
  - `ItemTypes.getByName(key.toString())` for material
  - `ComponentTypes.CUSTOM_NAME` for display name (Adventure Component — natively supported)
  - `ComponentTypes.LORE` → `new ItemLore(List<Component>)`
  - `ComponentTypes.ENCHANTMENTS` → `new ItemEnchantments(Map<EnchantmentType, Integer>, showInTooltip)` with HIDE_ENCHANTS flag support
  - `ComponentTypes.CUSTOM_MODEL_DATA_LISTS` → `new ItemCustomModelData(int)` (non-deprecated 1.21.4+)
- **Component conversion**: `WrapperPlayServerOpenWindow` accepts `net.kyori.adventure.text.Component` natively — no Gson bridge needed
- **No Bukkit Inventory API calls**: Uses PacketEvents' item builder exclusively; no `Bukkit.createInventory()`, no `InventoryView`
- **No NMS reflection**: All conversions go through PacketEvents' own component system
- Constructor takes `Plugin` for EntityScheduler scheduling
- `@NullMarked` at package and class level
- Compilation: 0 errors, 0 checkstyle violations

## ConfigurateMenuLoader (Task 13)
- Created `adapter/config/InvalidMenuException.java` — checked exception capturing file, lineNumber (nullable), nodePath, message
  - `getMessage()` formats as `In <file>:<line> at '<nodePath>': <message>`
- Created `adapter/config/ConfigurateMenuLoader.java` implementing `core.port.MenuLoader`
  - Uses `YamlConfigurationLoader.builder().indent(2).path(path).build()` for Configurate-YAML 4.1.2
  - Stateless and thread-safe — no mutable instance state
  - Parses DeluxeMenus-style YAML: menu_title, menu_type, open_command, open_requirement, items, filler_item
  - Supports both `slot` (int) and `slots` (list of ints) per item definition → multiple SlotTemplates
  - MiniMessage primary text parsing via `TextUtil.parseMiniMessage()`; legacy § fallback
  - MenuType parsing: direct enum match or `CHEST` + `rows` field (default 6 → GENERIC_9x6)
  - Material validation with `Material.matchMaterial()` + `Material.getMaterial()` legacy fallback
  - Enchantments parsed from list-of-maps format; resolved via `Enchantment.getByKey()`
  - Item flags via `ItemFlag.valueOf()`; NBT, skull_texture, custom_model_data, amount, durability
  - Validation: slot bounds (0 to type.size-1), priority ≥ 0, update_interval ≤ 1200
  - Circular parentMenuId detection via DFS in `detectCircularLinks()`
  - Requirement stubs (open_requirement, view_requirement, click_requirements) — real parsing in later task
  - Action strings parsed via injected `ActionParser` port when available
- Updated `adapter/config/InheritedMenuLoader.java` to use new InvalidMenuException constructors with proper arguments
- SpotBugs: fixed 4 warnings (null `getFileName()`, unused `nameToPath` map)
- Compilation: 0 errors, 0 checkstyle violations, 0 SpotBugs violations

## MenuAction Framework (Task 17)
- Created 12 action classes under `core/service/actions/`:
  - **MessageAction(Component)**: sends via `PlayerHandle.sendMessage()` — no scheduling needed
  - **ConsoleAction(String, SchedulerPort)**: dispatch via `Bukkit.dispatchCommand()` on global scheduler
  - **PlayerAction(String, SchedulerPort)**: dispatch via `Bukkit.dispatchCommand()` on player scheduler
  - **CloseAction()**: stubs `ActionResult.Success()` — actual close via packet later
  - **SoundAction(String, float, float, SchedulerPort)**: plays sound via native `Player.playSound()` on player scheduler
  - **RefreshAction()**: stubs `ActionResult.Success()` — triggers refresh in action runner
  - **OpenGuiAction(String, @Nullable Map)**: carries menuId + optional args; actual open deferred to runner
  - **TakeMoneyAction(@Nullable EconomyPort, double, SchedulerPort)**: checks `economy.has()` synchronously, withdraws on player scheduler; returns Failure if economy absent or insufficient funds
  - **GiveMoneyAction(@Nullable EconomyPort, double, SchedulerPort)**: deposits via economy port on player scheduler; returns Failure if economy absent
  - **GiveItemAction(ItemStackSnapshot, SchedulerPort)**: converts snapshot to Bukkit ItemStack and adds to inventory; excess drops at player location
  - **TakeItemAction(ItemStackSnapshot, SchedulerPort)**: removes matching items from player inventory by material type
  - **DelayAction(long, MenuAction)**: record returning `ActionResult.Delayed(ticks, next)` with validation
- All actions:
  - @NullMarked via package-info; explicit @Nullable on optional dependencies (EconomyPort)
  - Constructor-based manual DI for all dependencies
  - Thread-safe execution: world-modifying actions route through `SchedulerPort.runOnPlayer()` or `runOnGlobal()`
  - No Bukkit Inventory API usage for menu/GUI (inventory manipulation only for give/take item)
  - No PacketEvents imports
  - No @SuppressWarnings annotations
  - Graceful null handling: economy actions return `ActionResult.Failure("Economy is not available")` when port is null
- Pre-existing issues fixed to get build passing:
  - `RequirementEvaluator.java`: added missing `import com.cebonk03.packetmenu.core.domain.Requirement`
  - `DeluxeActionParser.java`: fixed `new new new ActionResult.Success()` → `new ActionResult.Success()` (22 sites), removed duplicate NullMarked import
- `./gradlew check` passes: 0 errors, 0 checkstyle violations, 0 SpotBugs violations

## Requirement Framework (Task 19)
- Created `core/domain/LogicMode.java` — enum with AND, OR
- Created `core/domain/RequirementSet.java` — record(LogicMode, Map<String,Requirement>, @Nullable List<MenuAction> denyActions)
  - Compact constructor defensively copies Map and List
  - `denyActions` is @Nullable to allow empty deny-action lists in YAML configs
- Created `core/service/RequirementEvaluator.java` — static utility class
  - `evaluate(RequirementSet, RequirementContext)` defaults ClickType to LEFT for deny actions
  - `evaluate(RequirementSet, RequirementContext, ClickType)` overload for explicit click type
  - AND mode: short-circuits on first `false`; OR mode: short-circuits on first `true`
  - Empty requirement map → AND vacuously true, OR vacuously false
  - On failure, iterates denyActions list calling `MenuAction.execute(ActionContext)`
- Created 10 built-in requirements under `core/service/requirements/`:
  - `HasPermissionRequirement` — single permission via PlayerHandle.hasPermission()
  - `HasPermissionsRequirement` — List<String> permissions + int minimumCount, short-circuits on count ≥ minimum
  - `HasMoneyRequirement` — EconomyPort.has(PlayerHandle, double) for Vault integration
  - `HasItemRequirement` — Bukkit Material + int amount, casts nativePlayer() to org.bukkit.entity.Player, scans PlayerInventory.getContents()
  - `HasExpRequirement` — int levels, casts nativePlayer() to Player, checks player.getLevel()
  - `StringEqualsRequirement` — PlaceholderPort.resolveString() on both input and expected before .equals()
  - `StringContainsRequirement` — PlaceholderPort.resolveString() on both input and expected before .contains()
  - `NumberGreaterRequirement` — parseDouble both sides, returns value > threshold, NumberFormatException → false
  - `NumberLessRequirement` — parseDouble both sides, returns value < threshold, NumberFormatException → false
  - `JavascriptRequirement` — ScriptEngineManager, tries "graal.js" then "nashorn", binds `player` to nativePlayer(), Boolean result or truthy, ScriptException → false
- All requirements implement `Requirement` @FunctionalInterface as standalone `final` classes
- Port dependency injection via constructor for: EconomyPort, PlaceholderPort
- Bukkit API access via PlayerHandle.nativePlayer() cast for: HasItemRequirement, HasExpRequirement
- javax.script for JavascriptRequirement (no GraalJS dep in build — works if server provides it)
- All files @NullMarked (via package-info), explicit @Nullable on optional fields/methods
- Compilation: 0 errors from requirement files (pre-existing errors in BroadcastSoundWorldAction.java and DeluxeActionParser.java from other tasks)

## PlaceholderAPI + Vault Adapters (Task 21)
- Created 3 adapters under `adapter/placeholder/`:
  - **NoOpPlaceholderAdapter** implements `PlaceholderPort`:
    - `resolve()` returns component unchanged; `resolveString()` returns raw unchanged
    - No external deps, always safe to load
  - **PlaceholderAPIAdapter** implements `PlaceholderPort`:
    - Static check: `Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null`
    - `resolve(Component, PlayerHandle)`: serializes via `MiniMessage.serialize()`, applies `PlaceholderAPI.setPlaceholders(Player, String)`, deserializes via `MiniMessage.deserialize()`
    - `resolveString(String, PlayerHandle)`: direct `PlaceholderAPI.setPlaceholders()` call
    - Direct import of `me.clip.placeholderapi.PlaceholderAPI` — class only loaded when PAPI is present (guarded by static boolean)
    - Graceful no-op when PAPI absent (no ClassNotFoundError at load time)
  - **VaultAdapter** implements `EconomyPort`:
    - Static check: `Bukkit.getPluginManager().getPlugin("Vault") != null`
    - Retrieves `Economy` via `Bukkit.getServicesManager().getRegistration(Economy.class).getProvider()`
    - Constructor takes `SchedulerPort` for caller-coordinated async execution
    - All methods: null-guard on `economy` field, try-catch fallback returning false
    - `has()`: `economy.has(Player, double)`
    - `withdraw()`: `economy.withdrawPlayer(Player, double).transactionSuccess()`
    - `deposit()`: `economy.depositPlayer(Player, double).transactionSuccess()`
    - Direct import of `net.milkbowl.vault.economy.Economy` — class only loaded when Vault is present
- Build config changes:
  - `gradle/libs.versions.toml`: added `placeholderapi = "2.11.6"`, `vault = "1.7.1"` versions and libraries
  - `build.gradle.kts`: added repos `repo.extendedclip.com` (PAPI) and `jitpack.io` (Vault)
  - `build.gradle.kts`: added `compileOnly(libs.placeholderapi)`, `compileOnly(libs.vault.api)`
  - `paper { serverDependencies { register("PlaceholderAPI") { required=false }, register("Vault") { required=false } } }`
- Key insight: Direct imports of soft-dependency classes work when class loading is deferred until after the runtime plugin check. The adapter class is only loaded via code paths that first verify the plugin is present
- Compilation: 0 errors, 0 checkstyle violations, 0 SpotBugs violations

## DeluxeActionParser + Action/Requirement Registries (Task 20)
- Created `core/service/ActionRegistry.java`:
  - ConcurrentHashMap<String, Function<List<String>, MenuAction>> storage
  - register(type, factory), get(type), getAll() -- thread-safe
  - Map.copyOf() for unmodifiable snapshot in getAll()
- Created `core/service/RequirementRegistry.java`:
  - Same ConcurrentHashMap pattern for Function<List<String>, Requirement>
  - register(type, factory), get(type), getAll()
- Created `adapter/config/DeluxeActionParser.java` implementing ActionParser:
  - Regex: `\[(\w+)\](?:\s+(.*?))?(?:\s*<delay=(\d+)>)?$`
  - Group 1: action type, Group 2: arguments (optional), Group 3: delay in ticks (optional)
  - Dispatches to ActionRegistry.get(type).apply(args)
  - Wraps with DelayAction (private record) when delay > 0 -> ActionResult.Delayed
  - parseAll(List<String>) convenience method for deny_commands parsing
  - Pre-registers all 20 built-in DeluxeMenus action types in constructor:
    player, console, message, close, sound, refresh, opengui, openguimenu,
    takemoney, givemoney, giveitem, takeitem, givepermission, takepermission,
    broadcast, jsonbroadcast, broadcastsound, placeholder, delay
  - Default implementations: `message`/`broadcast`/`jsonbroadcast` use TextUtil.parseMiniMessage
    + viewer.sendMessage(); others return new ActionResult.Success() as platform-access stubs
- Key insight: registries use `Function<List<String>, T>` as factory shape
- ActionResult.Success is a record -- must use `new ActionResult.Success()`
- All packages @NullMarked (core.service + adapter.config)
- Compilation: DeluxeActionParser compiles clean; remaining checkstyle/spotbugs errors
  in GiveItemAction/TakeItemAction are pre-existing from Tasks 17/18

## Tier 3 Actions: Broadcast, Permission, Placeholder (Task 18)
- Created 6 action classes under `core/service/actions/`:
  - **BroadcastAction(Component, SchedulerPort)**: broadcasts Adventure Component to all online players via `Bukkit.broadcast(Component)` on global scheduler
  - **JsonBroadcastAction(String, SchedulerPort)**: deserializes JSON string via `GsonComponentSerializer.gson().deserialize()` on execute, broadcasts on global scheduler; returns Failure on malformed JSON
  - **BroadcastSoundWorldAction(String, float, float, SchedulerPort)**: plays sound via `Player.playSound(Location, String, float, float)` to all `Bukkit.getOnlinePlayers()` filtered by `player.getWorld().equals(viewer.getWorld())` on global scheduler
  - **GivePermissionAction(String, SchedulerPort)**: grants permission via `player.addAttachment(plugin, node, true)` on player scheduler; null-safe with `Bukkit.getPluginManager().getPlugin("PacketMenu")` and `player.isOnline()` check
  - **TakePermissionAction(String, SchedulerPort)**: denies permission via `player.addAttachment(plugin, node, false)` on player scheduler; same null/online guards
  - **PlaceholderAction(Component, PlaceholderPort)**: resolves via `PlaceholderPort.resolve()` then sends via `PlayerHandle.sendMessage()` — no scheduler needed (matches MenuFactory pattern)
- All actions @NullMarked, final classes, constructor-based DI, no @SuppressWarnings
- Thread safety: world-modifying actions route through SchedulerPort.runOnGlobal() or runOnPlayer()
- Pre-existing fix: DeluxeActionParser.java had `new ActionResult.Success()` (not `ActionResult.Success()`) which the replaceAll tool compounded into `new new new ActionResult.Success()` — had to rewrite the file cleanly
- `./gradlew build -x test` passes: 0 compilation errors, 0 checkstyle violations, 0 SpotBugs violations
