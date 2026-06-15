# PacketMenu — Work Plan

## TL;DR

> **Quick Summary**: Build an enterprise-grade packet-based virtual GUI menu plugin for Paper/Folia servers (1.21.4–26.1.2), using PacketEvents 2.0 for protocol abstraction and Folia-native scheduling. No Bukkit Inventory API, no NMS reflection. DeluxeMenus-compatible YAML config format with hexagonal architecture.
>
> **Deliverables**: 
> - Fully functional Paper/Folia plugin JAR (shaded + reobfuscated)
> - Packet-based GUI rendering (open window, items, clicks, close)
> - YAML-driven menu configuration (DeluxeMenus-compatible superset)
> - 20+ action types and 10+ requirement types
> - Animated items, pagination, template inheritance
> - Full CI/CD pipeline (matrix test across MC versions)
> - Developer API for extensions
>
> **Estimated Effort**: XL (7 phases, 33+ tasks)
> **Parallel Execution**: YES — 8 waves
> **Critical Path**: Task 1 → Task 5 → Task 9 → Task 13 → Task 17 → Task 22 → Task 26 → Task 30 → F1-F4

---

## Context

### Original Request
Create a Minecraft plugin called PacketMenu — a packet-based virtual GUI menu system for Paper/Folia servers. Pure packet architecture (no Bukkit Inventory API), Folia-native scheduling, hexagonal architecture, DeluxeMenus-compatible YAML config format, supporting Paper 1.21.4 through 26.1.2.

### Interview Summary
**Key Discussions**:
- Architecture fully reviewed in previous session and confirmed ("no need change just write plan")
- Heavy build/test processes MUST run on GitHub Actions — local device cannot handle heavy compilation
- Paper vs Folia: Folia-native scheduling with runtime detection (supports both)
- DeluxeMenus compatibility: Tier 1-3 support with explicit OUT-of-scope items
- PacketEvents: External required dependency (not shaded)
- Java 21 —release 21 compile target for maximum compatibility

**Research Findings**:
- Greenfield project — empty repo with just README.md
- No SDD framework detected
- GitHub remote: git@github.com:Cebonk03/PacketMenu.git
- PacketEvents 2.7.0+ required for 1.21.4 support
- Paperweight-userdev may not be needed (no NMS access required — standard Paper API + PacketEvents suffices)

### Metis Review
**Identified Gaps** (addressed):
- **Version pinning**: PacketEvents pinned to 2.7.0 (minimum for 1.21.4); Gradle 9.5.1 validated in CI
- **paperweight-userdev**: Replaced with standard paper-api dependency (no NMS access needed)
- **DeluxeMenus tiers**: Defined explicitly (Tier 1/2/3 + OUT) to prevent scope creep
- **Folia vs Paper**: Runtime detection via SchedulerPort adapter
- **Test strategy**: Tests-after, full suites on GitHub Actions matrix
- **Missing ACs**: All acceptance criteria now agent-executable via CI

---

## Work Objectives

### Core Objective
Build a production-ready, packet-based virtual GUI menu plugin for Paper/Folia servers with zero Bukkit Inventory API usage, full Folia-native scheduling, and DeluxeMenus-compatible YAML configuration.

### Concrete Deliverables
- Plugin JAR (`PacketMenu-{version}-all.jar`) loadable on Paper 1.21.4–26.1.2
- GitHub Actions CI passing matrix builds and tests
- YAML-driven menu configs in DeluxeMenus-compatible format
- Packet-rendered inventory windows with items, clicks, and animations
- Action/requirement engine with 20+ actions and 10+ requirements
- Developer API for third-party extensions

### Definition of Done
- [ ] `./gradlew build` produces runnable JAR with zero errors
- [ ] Plugin loads on Paper 1.21.4 without errors
- [ ] Plugin loads on Folia 1.21.4 without errors
- [ ] Menu opens via packet-rendered window (visible on client)
- [ ] Click handling triggers configured actions
- [ ] All GitHub Actions workflows green on matrix combinations

### Must Have
- Packet-only GUI rendering (no Bukkit `Inventory` objects)
- Folia-compatible scheduling throughout
- DeluxeMenus-style YAML configuration (Tier 1-3 support)
- 20+ built-in actions (message, console, player, close, sound, refresh, opengui, money, items, permissions, broadcast)
- 10+ built-in requirements (permission, money, item, exp, string, numeric, javascript)
- PlaceholderAPI integration (optional, graceful fallback)
- Vault economy integration (optional, graceful fallback)
- GitHub Actions CI with matrix testing (Java 21/25 × MC 1.21.4/1.21.7/1.21.11/26.1/26.1.2)
- Animated items, pagination, template inheritance (Phase 4)

### Must NOT Have (Guardrails)
- No Bukkit `Inventory` API usage (packet-only)
- No `BukkitRunnable` or `Bukkit.getScheduler()` — use Paper/Folia schedulers exclusively
- No NMS reflection / `Class.forName()` — PacketEvents handles all version abstraction
- No shading of PacketEvents — it is an external plugin dependency
- No `synchronized` blocks on virtual-thread paths
- No raw `null` without `@Nullable` annotation (JSpecify enforced)
- No version-specific `if/else` chains — use PacketEvents server version abstraction
- No DeluxeMenus features beyond Tier 3 (BungeeCord actions, conditional slot types, advanced template system are OUT)
- No local heavy builds — all compilation/testing runs in GitHub Actions
- No `System.out.println` — use plugin logger

### DeluxeMenus Compatibility Tiers
| Tier | Coverage | Actions | Requirements |
|------|----------|---------|--------------|
| **Tier 1** | Menu definitions, items, basic clicks | message, console, player, close, sound | has_permission |
| **Tier 2** | Money, items, requirements | opengui/openguimenu, refresh, takemoney, givemoney, giveitem, takeitem | has_money, has_item, has_exp |
| **Tier 3** | Broadcast, permissions, placeholders | givepermission, takepermission, broadcast, jsonbroadcast, broadcastsoundworld, placeholder, delay | string_equals, string_contains, number_greater, number_less, has_permissions, javascript |
| **OUT** | BungeeCord, advanced templates | BungeeCord server switching, template system, conditional slot types | N/A |

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** — ALL verification is agent-executed via CI/CD.
> Heavy processes run on GitHub Actions, NOT on local device.

### Test Decision
- **Infrastructure exists**: NO (will be set up in Task 2)
- **Automated tests**: YES (tests-after — full suites in GitHub Actions)
- **Framework**: JUnit 5 + Mockito + ArchUnit + MockBukkit
- **Local execution**: Minimal — compile-only checks
- **CI execution**: Full matrix build, test, lint, security scan

### QA Policy
Every task MUST include agent-executable QA scenarios verified through CI pipeline.
- **Build/compile**: GitHub Actions `build.yml` workflow
- **Code quality**: GitHub Actions `quality.yml` (Checkstyle, SpotBugs, ArchUnit)
- **Unit tests**: `./gradlew test` in CI
- **Integration tests**: MockBukkit in CI
- **Runtime tests**: Minecraft server in Docker via GitHub Actions
- **Evidence**: CI artifacts + workflow run logs

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately — Build Infrastructure):
├── Task 1: Build system, version catalogs, dependency config
├── Task 2: GitHub Actions CI/CD (build + quality + release)
├── Task 3: Code quality (Checkstyle config, SpotBugs filter)
├── Task 4: Project scaffolding (package structure, bootstrap)

Wave 2 (After Wave 1 — Domain & Ports):
├── Task 5: Domain model records (MenuSession, SlotItem, MenuType, ItemStackSnapshot, etc.)
├── Task 6: Port interfaces (PacketComposer, SchedulerPort, MenuLoader, ActionParser, etc.)
├── Task 7: Bootstrap plugin class + lifecycle
├── Task 8: Utility classes (Validation, TextUtil)

Wave 3 (After Wave 2 — Packet Infrastructure):
├── Task 9: PacketEventsComposer (open/close/update windows via packets)
├── Task 10: PacketEventBus (click handling, packet filtering)
├── Task 11: FoliaSchedulerAdapter + PlayerHandle
├── Task 12: VersionCapabilities + Container ID allocator

Wave 4 (After Wave 3 — Menu Engine):
├── Task 13: ConfigurateMenuLoader (YAML parser, validation)
├── Task 14: ItemTemplateCompiler (YAML item → ItemStackSnapshot)
├── Task 15: MenuRegistry + MenuFactory (template → session)
├── Task 16: InheritedMenuLoader (extends: parent)

Wave 5 (After Wave 4 — Actions & Requirements):
├── Task 17: MenuAction framework + Tier 1-2 actions (message, console, player, close, sound, opengui, refresh, money, item)
├── Task 18: Tier 3 actions (broadcast, permission, placeholder, delay)
├── Task 19: Requirement framework + all built-in requirements
├── Task 20: DeluxeActionParser + registries
├── Task 21: PlaceholderAPI + Vault adapters (optional, graceful)

Wave 6 (After Wave 5 — Advanced Features):
├── Task 22: AnimatedSlotTemplate + MenuUpdateEngine
├── Task 23: PaginatedMenuTemplate + Paginator
├── Task 24: Player cache (Caffeine LRU)
├── Task 25: Argument binding (%arg_1%, %arg_2%)

Wave 7 (After Wave 6 — Quality Assurance):
├── Task 26: Unit tests — domain model + utilities
├── Task 27: Unit tests — config loader + action parser
├── Task 28: Architecture tests (ArchUnit)
├── Task 29: Integration tests (MockBukkit)

Wave 8 (After Wave 7 — Polish & Release):
├── Task 30: Performance optimization (pooling, lazy construction, batching)
├── Task 31: Memory leak hardening + metrics
├── Task 32: Documentation (README, docs/, migration guide)
├── Task 33: CHANGELOG, LICENSE, release finalization

Wave FINAL (After ALL tasks):
├── Task F1: Plan compliance audit (oracle)
├── Task F2: Code quality review (unspecified-high)
├── Task F3: Integration pipeline test (CI verification)
└── Task F4: Scope fidelity check (deep)

Critical Path: Task 1 → Task 5 → Task 9 → Task 13 → Task 17 → Task 22 → Task 26 → Task 30 → F1-F4
Parallel Speedup: ~60% faster than sequential
Max Concurrent: 5 (Wave 2-6)
```

### Dependency Matrix
- **Tasks 1-4**: No deps (Wave 1, start immediately)
- **Task 5**: Blocks: 6, 7, 8. Depends: 1 (build system)
- **Task 6**: Depends: 5. Blocks: 9, 10, 11, 12
- **Task 7**: Depends: 5. Blocks: 9, 10, 11
- **Task 8**: Depends: 5. No downstream blocks (utility)
- **Task 9-12**: Depends: 1, 5, 6, 7. Blocks: 13, 14, 15, 16
- **Task 13-16**: Depends: Wave 3. Blocks: Wave 5
- **Task 17-21**: Depends: Wave 4. Blocks: Wave 6
- **Task 22-25**: Depends: Wave 5. Blocks: Wave 7
- **Task 26-29**: Depends: Wave 6. No downstream blocks
- **Task 30-33**: Depends: Wave 6 (can partially overlap with Wave 7). Blocks: F1-F4
- **F1-F4**: Depends: ALL tasks complete

### Agent Dispatch Summary
- **Wave 1**: **4** — T1-T4 → `unspecified-high` (build config)
- **Wave 2**: **4** — T5, T6, T7 → `deep` (architecture-critical), T8 → `quick`
- **Wave 3**: **4** — T9, T10 → `deep` (packet infra is hardest), T11 → `unspecified-high`, T12 → `quick`
- **Wave 4**: **4** — T13, T14, T15 → `unspecified-high`, T16 → `unspecified-high`
- **Wave 5**: **5** — T17, T18 → `unspecified-high`, T19 → `unspecified-high`, T20 → `unspecified-high`, T21 → `quick`
- **Wave 6**: **4** — T22, T23 → `unspecified-high`, T24 → `quick`, T25 → `quick`
- **Wave 7**: **4** — T26, T27 → `quick` (unit tests), T28 → `quick`, T29 → `unspecified-high`
- **Wave 8**: **4** — T30, T31 → `unspecified-high`, T32 → `writing`, T33 → `quick`
- **FINAL**: **4** — F1 → `oracle`, F2 → `unspecified-high`, F3 → `deep`, F4 → `deep`

---

## TODOs
- [x] 1. Build system — Gradle, version catalog, dependencies

  **What to do**:
  - Create `settings.gradle.kts` with version catalog (`libs.versions.toml`)
  - Create `build.gradle.kts` with: paper-api dependency (NOT paperweight-userdev),
    PacketEvents 2.7.0 compileOnly, Configurate-YAML, Caffeine, JSpecify, Adventure,
    JUnit 5, Mockito, ArchUnit, Checkstyle, SpotBugs
  - Create `gradle.properties` with JVM args, encoding
  - Set up shadowJar for Configurate + Caffeine relocation
  - Set `org.gradle.configuration-cache=false` (Gradle 9.x compat)
  - Add `gradlew` and `gradlew.bat` wrapper (Gradle 9.5.1)

  **Must NOT do**:
  - Do NOT add paperweight-userdev (no NMS access needed)
  - Do NOT shade PacketEvents (external plugin dependency)
  - Do NOT use version ranges — pin exact versions

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high` (build configuration is critical infrastructure)
  - **Skills**: [] (no specialized skills needed for Gradle config)

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 2, 3, 4)
  - **Blocks**: Tasks 5–33
  - **Blocked By**: None (start immediately)

  **References**:
  - `https://docs.gradle.org/current/userguide/kotlin_dsl.html` — Gradle Kotlin DSL reference
  - `https://docs.papermc.io/paper/dev` — Paper API dependency setup
  - `https://docs.packetevents.com/` — PacketEvents API dependency

  **Acceptance Criteria**:
  - [ ] `./gradlew build` completes successfully (runs on GitHub Actions)
  - [ ] JAR file produced at `build/libs/PacketMenu-*.jar`
  - [ ] ShadowJAR produced at `build/libs/PacketMenu-*-all.jar` with Configurate relocated

  **QA Scenarios**:
  ```
  Scenario: Build pipeline produces valid JAR
    Tool: Bash (GitHub Actions `gradlew build`)
    Preconditions: Gradle wrapper installed, JDK 21 available
    Steps:
      1. Run `./gradlew build --no-daemon`
      2. Check `ls build/libs/PacketMenu-*.jar` exists
      3. Check `unzip -l build/libs/PacketMenu-*.jar | grep paper-plugin.yml` — config present
    Expected Result: exit 0, JAR contains paper-plugin.yml
    Evidence: CI workflow log (build job)

  Scenario: Build fails on invalid config
    Tool: Bash
    Preconditions: Corrupt build.gradle.kts
    Steps:
      1. Introduce syntax error in build.gradle.kts
      2. Run `./gradlew build`
    Expected Result: Non-zero exit code, descriptive error message
    Evidence: CI workflow log
  ```

  **Evidence to Capture**:
  - [ ] CI build log — `./gradlew build` succeeds
  - [ ] JAR artifact uploaded to GitHub Actions

  **Commit**: YES
  - Message: `build: initial Gradle build system with version catalog`
  - Files: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/`, `gradlew`, `gradlew.bat`
  - Pre-commit: `./gradlew build` (runs in CI)

- [x] 2. GitHub Actions CI/CD pipeline

  **What to do**:
  - Create `.github/workflows/build.yml` — build + matrix runtime test
  - Matrix: Java 21/25 × MC 1.21.4, 1.21.7, 1.21.11, 26.1, 26.1.2
  - Exclude invalid combos (Java 21 + MC 26.x = impossible)
  - Include: checkout, setup-java@v4, setup-gradle, build, spotbugsMain, checkstyleMain, test, archive artifacts
  - Create `.github/workflows/quality.yml` — Checkstyle, SpotBugs, ArchUnit, coverage
  - Create `.github/workflows/release.yml` — tag-based publish to GitHub Releases
  - Set `org.gradle.configuration-cache=false` in CI env

  **Must NOT do**:
  - Do NOT require local execution (user's device cannot handle builds)

  **Parallelization**: Wave 1 (with 1, 3, 4). Blocks: all downstream.

  **Acceptance Criteria**:
  - [ ] `.github/workflows/build.yml` triggers on push — all matrix combos succeed
  - [ ] Artifact upload includes plugin JAR

  **Commit**: YES
  - Message: `ci: add GitHub Actions workflows for build, quality, release`
  - Files: `.github/workflows/build.yml`, `.github/workflows/quality.yml`, `.github/workflows/release.yml`

- [x] 3. Code quality configuration

  **What to do**:
  - Create `config/checkstyle/checkstyle.xml` — Minecraft-adapted Sun/Google hybrid
    - Max line length: 120, Javadoc on public API only, forbid wildcard imports
    - Enforce `final` on params/locals, require `@Override`, maxErrors=0, maxWarnings=0
  - Create `config/spotbugs/exclude.xml` — exclude EI_EXPOSE_REP, SE_NO_SERIALVERSIONID, RCN_REDUNDANT_NULLCHECK
  - Configure SpotBugs: effort=MAX, reportLevel=MEDIUM
  - Wire into build.gradle.kts

  **Parallelization**: Wave 1 (with 1, 2, 4).

  **Commit**: YES (with Task 1)
  - Message: `build: add Checkstyle and SpotBugs quality configuration`
  - Files: `config/checkstyle/checkstyle.xml`, `config/spotbugs/exclude.xml`

- [x] 4. Project scaffolding — package structure + bootstrap

  **What to do**:
  - Create full hexagonal package structure under `com.cebonk03.packetmenu/`
  - Create `bootstrap/PacketMenuPlugin.java` — extends JavaPlugin, onEnable/onDisable stubs
  - Configure paper-plugin.yml via plugin-yml Gradle plugin (foliaSupported=true, packetevents dep)
  - Create `plugin.yml` fallback
  - Add `@NullMarked` package annotations

  **Parallelization**: Wave 1 (with 1, 2, 3). Blocks: 5-7.

  **Acceptance Criteria**:
  - [ ] `./gradlew build` succeeds with all packages in JAR

  **Commit**: YES
  - Message: `chore: scaffold hexagonal package structure and bootstrap`
  - Files: Package dirs, PacketMenuPlugin.java, package-info.java files

- [x] 5. Domain model records

  **What to do**:
  - Create immutable records in `core/domain/`:
    - `MenuSession` — containerId, menuId, MenuType, Component title, List<SlotItem>, revisionId, notifyOnClose, @Nullable parentMenu
    - `SlotItem` — int slot, ItemStackSnapshot, @Nullable ClickHandler, @Nullable ViewRequirement
    - `MenuType` — enum with GENERIC_9x1 through 9x6, GENERIC_3x3; size + protocolTypeId fields
    - `ItemStackSnapshot` — materialKey, amount, displayName, lore, enchantments, itemFlags, nbt, customModelData, durability, skullTexture
    - `SlotTemplate` — slot, priority, baseItem, viewRequirement, clickActions, clickRequirements, update, updateInterval
    - `ClickType` — enum for LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT, MIDDLE, DOUBLE_CLICK, DROP, CONTROL_DROP
    - `ActionResult` — sealed interface with Success, Failure(String reason), Delayed(long ticks, MenuAction next)
    - `MenuTemplate` — id, title, type, openCommands, openRequirement, slotTemplates, fillerItems, updateInterval, closeOnClickOutside, parentMenuId
  - All records must be `@NullMarked` with JSpecify annotations
  - Use Java 21 pattern matching in switches
  - Include `withRevision()` and `withItem()` convenience methods on MenuSession

  **Must NOT do**:
  - Do NOT add Bukkit/PacketEvents dependencies in domain packages (ArchUnit will enforce this)
  - Do NOT add mutable fields or setters (domain is pure data)

  **Parallelization**: Wave 2 (with 6, 7, 8). Blocks: 9-12. Blocked by: 1 (build).

  **Acceptance Criteria**:
  - [ ] All records compile without errors
  - [ ] ArchUnit test: no Bukkit/PacketEvents imports in core.domain

  **QA Scenarios**:
  ```
  Scenario: Records compile with @NullMarked
    Tool: Bash (GitHub Actions `./gradlew build`)
    Preconditions: Build system from Task 1
    Steps: 1. Run `./gradlew compileJava`
    Expected Result: exit 0, no compilation errors
    Evidence: CI log
  ```

  **Commit**: YES
  - Message: `feat(core): add domain model records`
  - Files: All files under `core/domain/`

- [x] 6. Port interfaces

  **What to do**:
  - Create port interfaces in `core/port/`:
    - `PacketComposer` — openWindow(PlayerHandle, MenuSession), sendItems(PlayerHandle, MenuSession), setSlot(...), closeWindow(PlayerHandle, int)
    - `SchedulerPort` — runOnPlayer, runOnGlobal, runAsync, runDelayedOnPlayer
    - `MenuLoader` — load(Path) → MenuTemplate, loadAll(Path) → Map<String, MenuTemplate>
    - `ActionParser` — parse(String raw) → MenuAction
    - `PlaceholderPort` — resolve(Component, PlayerHandle) → Component, resolveString(String, PlayerHandle) → String
    - `EconomyPort` — has(PlayerHandle, double), withdraw(PlayerHandle, double), deposit(PlayerHandle, double)
    - `PlayerHandle` — interface abstracting PaperPlayer: nativePlayer(), sendMessage(), hasPermission(), getUniqueId(), getName()
  - All interfaces must be `@NullMarked`
  - Document each method with javadoc

  **Must NOT do**:
  - Do NOT add Bukkit-specific types in port interface signatures (use PlayerHandle, Component, etc.)

  **Parallelization**: Wave 2 (with 5, 7, 8). Blocks: 9-12. Blocked by: 5.

  **Acceptance Criteria**:
  - [ ] All port interfaces compile
  - [ ] No Bukkit imports in port package

  **Commit**: YES
  - Message: `feat(core): add port interfaces for hexagonal architecture`
  - Files: `core/port/*.java`

- [x] 7. Bootstrap plugin class + lifecycle

  **What to do**:
  - Complete `bootstrap/PacketMenuPlugin.java`:
    - onEnable(): verify PacketEvents is loaded (log error if missing), initialize scheduler adapter, register packet listener, register /packetmenu command
    - onDisable(): close all active menu sessions, cancel all scheduled tasks, clean up resources
    - onLoad(): initialize configuration directory
    - Wire up ServiceLocator or simple DI registry for port/adapter wiring
  - Dependency injection approach: manual constructor injection + registry (no framework)

  **Parallelization**: Wave 2 (with 5, 6, 8). Blocks: 9-12. Blocked by: 5.

  **Commit**: YES
  - Message: `feat(bootstrap): implement plugin lifecycle and DI wiring`
  - Files: `bootstrap/PacketMenuPlugin.java` + any DI registry class

- [x] 8. Utility classes

  **What to do**:
  - Create `util/Validation.java`: pre-condition checks, slot bounds validation, material name validation
  - Create `util/TextUtil.java`: MiniMessage parsing helpers, legacy color code conversion, PlaceholderAPI string detection

  **Parallelization**: Wave 2 (with 5, 6, 7). Blocked by: 5. No downstream blocks.

  **Commit**: YES (with Task 5 or 6)
  - Message: `feat(util): add Validation and TextUtil utility classes`
  - Files: `util/Validation.java`, `util/TextUtil.java`

- [x] 9. PacketEventsComposer — packet send/recv implementation

  **What to do**:
  - Implement `adapter/packetevents/PacketEventsComposer.java` implementing `PacketComposer`:
    - `openWindow()`: Build `WrapperPlayServerOpenWindow` with containerId, protocolTypeId, title
    - `sendItems()`: Build `WrapperPlayServerWindowItems` with revisionId, full item list
    - `setSlot()`: Build `WrapperPlayServerSetSlot` for single-slot updates
    - `closeWindow()`: Build `WrapperPlayServerCloseWindow`
  - Component conversion: Adventure Component → PacketEvents component via Gson bridge
  - Item conversion: `ItemStackSnapshot` → PacketEvents `ItemStack` (material, amount, NBT, displayName, lore, enchants)
  - Send all packets via `PacketEvents.getAPI().getPlayerManager().sendPacket()`
  - All packet sends must route through EntityScheduler (not direct)

  **Must NOT do**:
  - Do NOT use `WrapperPlayServerOpenWindow` with deprecated constructors
  - Do NOT use `PacketEvent.getAPI().getPlayerManager().sendPacket()` from async threads without scheduler

  **Parallelization**: Wave 3 (with 10, 11, 12). Blocks: 13-16. Blocked by: 1, 5, 6, 7.

  **Acceptance Criteria**:
  - [ ] All wrapper classes compile against PacketEvents 2.7.0
  - [ ] Packet sending is routed through EntityScheduler
  - [ ] Component conversion handles MiniMessage, legacy, Gson

  **Commit**: YES
  - Message: `feat(packet): implement PacketEventsComposer with window/item/close packets`
  - Files: `adapter/packetevents/PacketEventsComposer.java`

- [x] 10. PacketEventBus — click handling and packet filtering

  **What to do**:
  - Implement `adapter/packetevents/PacketEventBus.java`:
    - Register `PacketReceiveEvent` listener for `CLICK_WINDOW`
    - Parse click type from packet: slot, button, action mode
    - Map to domain `ClickType` enum
    - Route to `ClickInterpreter` that maps slot → SlotTemplate → ClickHandler
    - Register `PacketSendEvent` listener (optional) for packet filtering
    - Thread-safe: ensure clicks are handled on player's entity scheduler
    - Handle edge cases: click outside window, invalid slot index, closed window
  - Create `ClickInterpreter` in core/service that resolves click targets

  **Must NOT do**:
  - Do NOT process clicks on the netty IO thread — always schedule to entity scheduler

  **Parallelization**: Wave 3 (with 9, 11, 12). Blocked by: 1, 5, 6, 7.

  **Commit**: YES
  - Message: `feat(packet): implement PacketEventBus for click handling`
  - Files: `adapter/packetevents/PacketEventBus.java`, `core/service/ClickInterpreter.java`

- [x] 11. FoliaSchedulerAdapter + PlayerHandle

  **What to do**:
  - Implement `adapter/paper/FoliaSchedulerAdapter.java` implementing `SchedulerPort`:
    - `runOnPlayer(PlayerHandle, Runnable)`: Use `EntityScheduler` (player.getScheduler().run())
    - `runOnGlobal(Runnable)`: Use `Bukkit.getGlobalRegionScheduler().run()`
    - `runAsync(Runnable)`: Use `Bukkit.getAsyncScheduler().runNow()`
    - `runDelayedOnPlayer(PlayerHandle, long ticks, Runnable)`: Use `EntityScheduler.runDelayed()`
    - Detect Folia availability at runtime (fallback behavior on non-Folia Paper)
  - Implement `adapter/paper/PaperPlayerHandle.java` implementing `PlayerHandle`:
    - Wrap Paper `Player` methods
    - `nativePlayer()` returns the underlying Player

  **Parallelization**: Wave 3 (with 9, 10, 12). Blocked by: 1, 5, 6, 7.

  **Commit**: YES
  - Message: `feat(scheduler): implement FoliaSchedulerAdapter and PaperPlayerHandle`
  - Files: `adapter/paper/FoliaSchedulerAdapter.java`, `adapter/paper/PaperPlayerHandle.java`

- [x] 12. VersionCapabilities + Container ID allocator

  **What to do**:
  - Implement `adapter/packetevents/VersionCapabilities.java`:
    - Detect server version via `PacketEvents.getAPI().getServerManager().getVersion()`
    - Provide boolean checks: supportsModernComponentFormat(), supportsItemComponents(), etc.
  - Implement `ContainerIdAllocator` class in core/service:
    - `AtomicInteger` starting at 101 (reserve 0-100 for vanilla)
    - `ConcurrentHashMap<UUID, Deque<Integer>>` for per-player reclaim tracking
    - `allocate(UUID playerId)` → int
    - `reclaim(UUID playerId, int containerId)` → void
    - Thread-safe for concurrent access

  **Parallelization**: Wave 3 (with 9, 10, 11). Blocked by: 1, 5, 6, 7.

  **Commit**: YES
  - Message: `feat(packet): add VersionCapabilities and ContainerIdAllocator`
  - Files: `adapter/packetevents/VersionCapabilities.java`, `core/service/ContainerIdAllocator.java`

- [x] 13. ConfigurateMenuLoader — YAML parser

  **What to do**:
  - Implement `adapter/config/ConfigurateMenuLoader.java` implementing `MenuLoader`:
    - Use Configurate-YAML `ConfigurationLoader<CommentedConfigurationNode>`
    - Parse DeluxeMenus-style YAML structure: menu_title, menu_type, open_command, open_requirement, items, etc.
    - Validate: slot bounds (0 to type.size-1), priority ≥ 0, material existence, update_interval ≤ 1200
    - Circular menu linking detection (parentMenuId cycles)
    - Parse Component text via MiniMessage primary, legacy & fallback
    - Support both `slot` (int) and `slots` (list of ints) per item
    - Parse NBT compound, enchantments, item_flags, skull_texture, custom_model_data
    - Thread-safe loading via AsyncScheduler
  - Create `InvalidMenuException` for descriptive parse errors (file + line number)

  **Parallelization**: Wave 4 (with 14, 15, 16). Blocks: 17-21. Blocked by: 1, 5, 6, 9.

  **Commit**: YES
  - Message: `feat(config): implement ConfigurateMenuLoader with YAML parsing`
  - Files: `adapter/config/ConfigurateMenuLoader.java`, `adapter/config/InvalidMenuException.java`

- [x] 14. ItemTemplateCompiler — YAML item → ItemStackSnapshot

  **What to do**:
  - Implement `adapter/config/ItemTemplateCompiler.java`:
    - Parse item YAML nodes into `ItemStackSnapshot` records
    - Material resolution: Bukkit Material name → PacketEvents ItemType
    - Component parsing: display_name, lore lines → Adventure Component (MiniMessage)
    - Enchantment parsing: map string key + int level
    - Item flags: HIDE_ATTRIBUTES, HIDE_ENCHANTS, etc.
    - NBT: custom model data, raw NBT nodes
    - Skull textures: Base64 skull value parsing
  - Material validation: fail-fast on unknown materials with descriptive error

  **Parallelization**: Wave 4 (with 13, 15, 16). Blocked by: 1, 5, 6, 9.

  **Commit**: YES (with Task 13)
  - Message: `feat(config): implement ItemTemplateCompiler for item parsing`
  - Files: `adapter/config/ItemTemplateCompiler.java`

- [x] 15. MenuRegistry + MenuFactory

  **What to do**:
  - Implement `core/service/MenuRegistry.java`:
    - `ConcurrentHashMap<String, MenuTemplate>` for thread-safe template storage
    - `register(MenuTemplate)`, `unregister(String)`, `get(String)`, `getAll()`
    - `reloadAll()` — reload all menus from disk, hot-swap without leaking sessions
  - Implement `core/service/MenuFactory.java`:
    - `create(PlayerHandle, MenuTemplate, List<String> args)` → `MenuSession`
    - Apply open_requirement before creating session (fail with deny commands if not met)
    - Resolve placeholders in title and items
    - Apply view_requirements to filter visible items
    - Assign container ID via ContainerIdAllocator
    - Schedule dynamic item updates if template has update_interval

  **Parallelization**: Wave 4 (with 13, 14, 16). Blocked by: 1, 5, 6, 9.

  **Commit**: YES
  - Message: `feat(menu): implement MenuRegistry and MenuFactory`
  - Files: `core/service/MenuRegistry.java`, `core/service/MenuFactory.java`

- [x] 16. InheritedMenuLoader — template inheritance

  **What to do**:
  - Implement `adapter/config/InheritedMenuLoader.java`:
    - Parse `extends: parent_menu_id` in YAML root
    - Load parent template from registry
    - Merge: child title/type/open_commands override parent; child slots override by priority (child wins ties)
    - Recursive inheritance support (extends chain, depth limit to prevent cycles)
    - Cycle detection: track inheritance chain, reject if cycle found
  - Integration with ConfigurateMenuLoader for base loading

  **Parallelization**: Wave 4 (with 13, 14, 15). Blocked by: 1, 5, 6, 9, 13.

  **Commit**: YES
  - Message: `feat(config): implement InheritedMenuLoader with extends support`
  - Files: `adapter/config/InheritedMenuLoader.java`

- [x] 17. MenuAction framework + Tier 1-2 actions

  **What to do**:
  - Create `MenuAction` functional interface in core/domain
  - Create `ActionContext` record: viewer, session, slot, clickType, sourceAction
  - Create `ActionResult` sealed interface (Success, Failure, Delayed)
  - Implement actions (Tier 1-2):
    - `MessageAction` — send Component to player
    - `ConsoleAction` — execute command as console
    - `PlayerAction` — execute command as player
    - `CloseAction` — close current menu
    - `SoundAction` — play sound at player's location
    - `RefreshAction` — re-evaluate view requirements and update displayed items
    - `OpenGuiAction` — open another menu by ID (with arg support)
    - `TakeMoneyAction` / `GiveMoneyAction` — Vault economy integration (graceful if missing)
    - `GiveItemAction` / `TakeItemAction` — give/take items from player inventory
    - `DelayAction` — wrap another action with delayed execution
  - All actions must handle null/absent dependencies gracefully (Vault not installed, etc.)
  - Thread safety: world-modifying actions route through appropriate schedulers

  **Parallelization**: Wave 5 (with 18, 19, 20, 21). Blocks: 22-25. Blocked by: Wave 4.

  **Commit**: YES
  - Message: `feat(action): implement MenuAction framework and Tier 1-2 actions`
  - Files: `core/domain/MenuAction.java`, `core/domain/ActionContext.java`, `core/domain/ActionResult.java`, all action implementation files

- [x] 18. Tier 3 actions (broadcast, permission, placeholder)

  **What to do**:
  - Implement Tier 3 actions:
    - `GivePermissionAction` / `TakePermissionAction` — permission management via Vault or direct
    - `BroadcastAction` — broadcast Component to all online players
    - `JsonBroadcastAction` — broadcast JSON-formatted component
    - `BroadcastSoundWorldAction` — play sound to all players in same world
    - `PlaceholderAction` — send placeholder-resolved message to player

  **Parallelization**: Wave 5 (with 17, 19, 20, 21). Blocked by: Wave 4.

  **Commit**: YES (with Task 17)
  - Message: `feat(action): implement Tier 3 actions`
  - Files: Action implementation classes for Tier 3

- [x] 19. Requirement framework + built-in requirements

  **What to do**:
  - Create `Requirement` functional interface: `boolean test(RequirementContext ctx)`
  - Create `RequirementContext` record: viewer, session, slot
  - Create `RequirementSet` record: LogicMode (AND/OR), Map<String, Requirement>, denyActions
  - Implement RequirementEvaluator: AND short-circuits on first false, OR short-circuits on first true
  - Implement built-in requirements:
    - `HasPermissionRequirement` — check single permission
    - `HasPermissionsRequirement` — check multiple permissions with minimum count
    - `HasMoneyRequirement` — Vault economy check
    - `HasItemRequirement` — check player inventory for specific item + amount
    - `HasExpRequirement` — check player XP levels
    - `StringEqualsRequirement` / `StringContainsRequirement` — placeholder-resolved string comparison
    - `NumberGreaterRequirement` / `NumberLessRequirement` — numeric comparison
    - `JavascriptRequirement` — Nashorn JS engine evaluation (sandboxed)

  **Parallelization**: Wave 5 (with 17, 18, 20, 21). Blocked by: Wave 4.

  **Commit**: YES
  - Message: `feat(requirement): implement Requirement framework and all built-in types`
  - Files: All requirement interfaces and implementations

- [x] 20. DeluxeActionParser + registries

  **What to do**:
  - Implement `DeluxeActionParser` implementing `ActionParser`:
    - Parse `[action] arg1 arg2 ... <delay=20>` format
    - Regex match action type + arguments + optional delay
    - Dispatch to appropriate MenuAction constructor
    - Wrap with DelayAction if delay > 0
  - Implement `ActionRegistry`: ConcurrentHashMap-based, register/get action types
  - Implement `RequirementRegistry`: same pattern
  - Parse deny_commands from requirement config into actions

  **Parallelization**: Wave 5 (with 17, 18, 19, 21). Blocked by: Wave 4.

  **Commit**: YES
  - Message: `feat(action): implement DeluxeActionParser and action/requirement registries`
  - Files: `adapter/config/DeluxeActionParser.java`, `core/service/ActionRegistry.java`, `core/service/RequirementRegistry.java`

- [x] 21. PlaceholderAPI + Vault adapters

  **What to do**:
  - Implement `adapter/placeholder/PlaceholderAPIAdapter.java` implementing `PlaceholderPort`:
    - Check if PlaceholderAPI is present at runtime (soft dependency)
    - `resolve(Component, PlayerHandle)`: serialize component, apply placeholders, re-deserialize
    - `resolveString(String, PlayerHandle)`: apply `%placeholder%` expansion
  - Implement `adapter/placeholder/VaultAdapter.java` implementing `EconomyPort`:
    - Check if Vault + EconomyService are present at runtime (soft dependency)
    - `has()`, `withdraw()`, `deposit()` — wrap Vault economy calls
    - Schedule economy operations via AsyncScheduler (Vault may use DB)
  - Create `adapter/placeholder/NoOpPlaceholderAdapter.java` — fallback that passes text through unchanged

  **Parallelization**: Wave 5 (with 17, 18, 19, 20). Blocked by: Wave 4.

  **Commit**: YES
  - Message: `feat(adapter): implement PlaceholderAPI and Vault adapters with graceful fallback`
  - Files: All adapter implementations

- [ ] 22. AnimatedSlotTemplate + MenuUpdateEngine

  **What to do**:
  - Create `AnimatedSlotTemplate` record:
    - `List<AnimationFrame> frames`, `int ticksPerFrame`, `boolean loop`
    - `AnimationFrame` record: ItemStackSnapshot, viewRequirement, Map<ClickType, List<MenuAction>> actions
    - Frame cycling logic: current frame = (tick / ticksPerFrame) % frames.size()
  - Implement `MenuUpdateEngine`:
    - Track active sessions with dynamic/updating slots
    - For each tick interval: rebuild slot items, send SetSlot packets for changed items
    - Cancel all update tasks when session closes
    - Use EntityScheduler for per-player update tasks
    - Use revision ID increment on every slot update
  - Wire into MenuFactory: if template has update_interval, start update engine

  **Parallelization**: Wave 6 (with 23, 24, 25). Blocked by: Wave 5.

  **Commit**: YES
  - Message: `feat(menu): implement AnimatedSlotTemplate and MenuUpdateEngine`
  - Files: `core/domain/AnimatedSlotTemplate.java`, `core/service/MenuUpdateEngine.java`

- [ ] 23. PaginatedMenuTemplate + Paginator

  **What to do**:
  - Create `PaginatedMenuTemplate` record:
    - itemsPerPage, nextPageSlot, prevPageSlot, pageIndicatorSlot
    - nextPageItem, prevPageItem, pageIndicatorItem (ItemStackSnapshot templates)
  - Implement `Paginator` service:
    - `generatePages(PlayerHandle, MenuTemplate, List<SlotItem>)` → List<MenuSession>
    - Partition data items into pages of itemsPerPage
    - Inject navigation items: next page, previous page, page indicator
    - First page: disable previous button (grayed out or hidden)
    - Last page: disable next button
    - Page indicator: show "Page X of Y" as item display name

  **Parallelization**: Wave 6 (with 22, 24, 25). Blocked by: Wave 5.

  **Commit**: YES
  - Message: `feat(menu): implement PaginatedMenuTemplate and Paginator`
  - Files: `core/domain/PaginatedMenuTemplate.java`, `core/service/Paginator.java`

- [ ] 24. Player-specific caching (Caffeine)

  **What to do**:
  - Set up Caffeine cache for player-specific data:
    - Placeholder resolution cache: key = playerId + templateString, value = Component, TTL = 30s, max size = 1000 per player
    - Requirement result cache: key = playerId + requirementId, value = Boolean, TTL = 5s (short TTL for dynamic requirements)
    - Menu session cache: active session per player (ConcurrentHashMap, no eviction)
  - Wire into MenuFactory and PlaceholderAdapter
  - Ensure cache is invalidated on player quit
  - Thread-safe: Caffeine caches are thread-safe by default

  **Parallelization**: Wave 6 (with 22, 23, 25). Blocked by: Wave 5.

  **Commit**: YES
  - Message: `feat(cache): implement Caffeine-based player cache for placeholders and requirements`
  - Files: `core/service/PlayerCache.java`

- [ ] 25. Argument binding (%arg_1%, %arg_2%)

  **What to do**:
  - Implement argument binding in MenuFactory:
    - When creating session from command `/menu <arg1> <arg2>`, pass args to factory
    - Replace `%arg_1%`, `%arg_2%`, etc. in all placeholders during session creation
    - Apply argument resolution BEFORE PlaceholderAPI resolution (args take precedence)
    - Support up to 10 arguments (`%arg_1%` through `%arg_10%`)
  - Empty args resolve to empty string
  - Integration: `/packetmenu open <menu> [args...]` command handler

  **Parallelization**: Wave 6 (with 22, 23, 24). Blocked by: Wave 5.

  **Commit**: YES
  - Message: `feat(menu): implement argument binding (%arg_N% placeholders)`
  - Files: Updates to MenuFactory, command handler

- [ ] 26. Unit tests — domain model + utilities

  **What to do**:
  - Create `src/test/java/unit/domain/` test classes:
    - `MenuSessionTest` — immutability, revision increment, withRevision/withItem
    - `ItemStackSnapshotTest` — equality, builder, material validation
    - `MenuTypeTest` — size validation, protocolTypeId mapping
    - `RequirementSetTest` — AND/OR logic, short-circuit evaluation
    - `ActionResultTest` — sealed interface patterns
  - Create `src/test/java/unit/service/` test classes:
    - `ContainerIdAllocatorTest` — allocation order, reclaim, thread safety
    - `ValidationTest` — slot bounds, material name validation
    - `TextUtilTest` — MiniMessage parsing, legacy conversion
  - Use JUnit 5 + Mockito
  - All tests runnable via `./gradlew test` (in CI only)

  **Parallelization**: Wave 7 (with 27, 28, 29). Blocked by: Wave 6.

  **Commit**: YES
  - Message: `test(unit): add domain model and utility unit tests`
  - Files: Test files under `src/test/java/unit/`

- [ ] 27. Unit tests — config loader + action parser

  **What to do**:
  - Create test YAML files for ConfigurateMenuLoader:
    - Valid menu with all features
    - Invalid menu (bad slot, missing field, wrong type)
    - Minimally valid menu (title + type only)
    - Menus with MiniMessage, legacy, mixed formatting
  - Test ItemTemplateCompiler: material resolution, enchantment parsing, NBT parsing, skull texture
  - Test DeluxeActionParser: all 20+ action types, delay wrapping, invalid action error
  - Test RequirementEvaluator: all requirement types, deny commands on failure
  - Test InheritedMenuLoader: inheritance merge, cycle detection, depth limiting

  **Parallelization**: Wave 7 (with 26, 28, 29). Blocked by: Wave 6.

  **Commit**: YES (with Task 26)
  - Message: `test(unit): add config loader and action parser unit tests`
  - Files: Test files + test YAML resources

- [ ] 28. Architecture tests (ArchUnit)

  **What to do**:
  - Create `src/test/java/architecture/ArchitectureTest.java`:
    - Hexagonal architecture test: domain → no deps, core → no framework deps, adapter → depends on core
    - No Bukkit in core: classes in core.* packages must not import org.bukkit.*
    - No PacketEvents in core: classes in core.* must not import PacketEvents
    - Naming convention: service classes end with Service, adapter classes end with adapter name
    - Cyclic dependency check: no cycles between packages
    - JSpecify annotation check: all public classes are @NullMarked
  - Configure ArchUnit with @AnalyzeClasses(packagesOf = PacketMenuPlugin.class)
  - MUST pass in CI quality.yml

  **Parallelization**: Wave 7 (with 26, 27, 29). Blocked by: Wave 6.

  **Commit**: YES
  - Message: `test(arch): add ArchUnit architecture tests enforcing hexagonal boundaries`
  - Files: `src/test/java/architecture/ArchitectureTest.java`

- [ ] 29. Integration tests (MockBukkit)

  **What to do**:
  - Create `src/test/java/integration/` tests:
    - `MenuLifecycleTest` — use MockBukkit to simulate server startup, menu open, click, and close sequence
    - `PluginLoadTest` — verify plugin enables correctly, PacketEvents check passes
    - `ConfigReloadTest` — simulate /packetmenu reload, verify hot-swap
    - `PlayerDisconnectTest` — verify sessions are cleaned up on disconnect
    - `MultiPlayerTest` — two players open same menu independently
  - Use MockBukkit v1.21 (com.github.seeseemelk:MockBukkit-v1.21)
  - Test full pipeline: YAML → MenuTemplate → MenuSession → packet send (MockBukkit captures packets)

  **Parallelization**: Wave 7 (with 26, 27, 28). Blocked by: Wave 6.

  **Commit**: YES
  - Message: `test(integration): add MockBukkit integration tests for full menu pipeline`
  - Files: `src/test/java/integration/*.java`

- [ ] 30. Performance optimization — pooling, lazy construction, batching

  **What to do**:
  - Implement `ItemStackSnapshotPool`: ConcurrentHashMap-based canonical instance deduplication
    - Pool filler items (black glass panes, etc.) — identical items share one instance
    - `canonical(ItemStackSnapshot)` → interned instance
  - Lazy packet construction:
    - Defer `WrapperPlayServerWindowItems` building until actual send
    - Cache serialized bytes for identical item sets
  - View requirement batching:
    - Evaluate ALL view requirements for a menu in a single pass on open
    - Cache results per-player with short TTL (5s)
  - NBT deduplication: intern NBT compounds for identical items
  - Metrics: add simple counters (active viewers, menus loaded, packets/sec) via plugin logger

  **Must NOT do**:
  - Do NOT optimize before profiling — only what's measurable
  - Do NOT add JMH benchmarks (cannot run locally)

  **Parallelization**: Wave 8 (with 31, 32, 33). Blocked by: Wave 6.

  **Commit**: YES
  - Message: `perf: implement item pooling, lazy packets, and view requirement batching`
  - Files: `core/service/ItemStackSnapshotPool.java`, updates to PacketEventsComposer, MenuFactory

- [ ] 31. Memory leak hardening + metrics

  **What to do**:
  - Ensure all EntityScheduler tasks are cancelled on session close
  - Clear MenuSession references on player quit (PlayerQuitEvent listener)
  - Clear player cache on quit
  - Cancel all update tasks on plugin disable
  - Add WeakReference test: open/close 1000 menus, verify no session retention
  - Add simple metrics:
    - Counter: active viewers
    - Counter: total menus loaded
    - Counter: packets sent
    - Log metrics every 5 minutes at INFO level
  - Graceful degradation: if PacketEvents is missing, log fatal error and disable plugin

  **Parallelization**: Wave 8 (with 30, 32, 33). Blocked by: Wave 6.

  **Commit**: YES
  - Message: `fix(memory): harden session lifecycle and add metrics`
  - Files: Updates to plugin lifecycle, menu session manager, cache

- [ ] 32. Documentation — README, docs folder, migration guide

  **What to do**:
  - Write `README.md` with:
    - Badges (CI status, license, version)
    - Feature overview
    - Installation instructions (dependencies: Paper 1.21.4+, PacketEvents, optional Vault/PAPI)
    - Quick start: create first menu YAML
    - Permissions reference
    - Commands reference
  - Create `docs/` folder:
    - `configuration.md` — complete YAML spec with examples for every node
    - `actions.md` — all 20+ actions with syntax and examples
    - `requirements.md` — all 10+ requirements with examples
    - `api.md` — developer API for creating custom actions/requirements
    - `migration.md` — migrating from DeluxeMenus (config differences, compatibility tiers, known issues)
  - DeluxeMenus compatibility tiers clearly documented in migration guide

  **Parallelization**: Wave 8 (with 30, 31, 33). Blocked by: Wave 6.

  **Commit**: YES
  - Message: `docs: add comprehensive README and documentation`
  - Files: `README.md`, `docs/*.md`

- [ ] 33. CHANGELOG, LICENSE, release finalization

  **What to do**:
  - Create `CHANGELOG.md` following Keep a Changelog format
  - Create `LICENSE` — MIT
  - Finalize `paper-plugin.yml` with correct version, authors
  - Verify GitHub Actions release.yml works with tag push
  - Create release script or action step for Hangar/Modrinth (prep structure)
  - Final verification: clean checkout, `./gradlew build`, JAR loads on Paper 1.21.4

  **Parallelization**: Wave 8 (with 30, 31, 32). Blocked by: Wave 6.

  **Commit**: YES
  - Message: `chore: finalize CHANGELOG, LICENSE, and release configuration`
  - Files: `CHANGELOG.md`, `LICENSE`, release pipeline

---

## Final Verification Wave

- [ ] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists (read files, check Gradle build, verify CI). For each "Must NOT Have": search codebase for forbidden patterns — reject with file:line if found. Check evidence files exist in CI artifacts. Compare deliverables against plan.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [ ] F2. **Code Quality Review** — `unspecified-high`
  Run `./gradlew build checkstyleMain spotbugsMain test` (via GitHub Actions). Review all changed files for: `as any`/`@SuppressWarnings`, empty catches, commented-out code, unused imports. Check AI slop: excessive comments, over-abstraction, generic names.
  Output: `Build [PASS/FAIL] | Lint [PASS/FAIL] | Tests [N pass/N fail] | Files [N clean/N issues] | VERDICT`

- [ ] F3. **Integration Pipeline Test** — `deep`
  Verify full pipeline from code push to CI green: 1) git push, 2) GitHub Actions build.yml triggers, 3) all matrix combinations pass, 4) quality.yml passes, 5) JAR artifact is downloadable. Cross-task integration: actions working with menus, requirements with items.
  Output: `Pipeline [N/N steps pass] | Matrix [N/N combos] | VERDICT`

- [ ] F4. **Scope Fidelity Check** — `deep`
  For each task: read "What to do", read actual diff. Verify 1:1 — everything in spec was built (no missing), nothing beyond spec was built (no creep). Check "Must NOT do" compliance. Detect cross-task contamination. Flag unaccounted changes.
  Output: `Tasks [N/N compliant] | Contamination [CLEAN/N issues] | Unaccounted [CLEAN/N files] | VERDICT`

---

## Commit Strategy

| Task(s) | Commit Type | Message |
|---------|------------|---------|
| 1 | build | `build: initial Gradle build system with version catalog` |
| 2 | ci | `ci: add GitHub Actions workflows for build, quality, release` |
| 3 | build | `build: add Checkstyle and SpotBugs quality configuration` |
| 4 | chore | `chore: scaffold hexagonal package structure and bootstrap` |
| 5 | feat(core) | `feat(core): add domain model records` |
| 6 | feat(core) | `feat(core): add port interfaces for hexagonal architecture` |
| 7 | feat(bootstrap) | `feat(bootstrap): implement plugin lifecycle and DI wiring` |
| 8 | feat(util) | `feat(util): add Validation and TextUtil utility classes` |
| 9 | feat(packet) | `feat(packet): implement PacketEventsComposer with window/item/close packets` |
| 10 | feat(packet) | `feat(packet): implement PacketEventBus for click handling` |
| 11 | feat(scheduler) | `feat(scheduler): implement FoliaSchedulerAdapter and PaperPlayerHandle` |
| 12 | feat(packet) | `feat(packet): add VersionCapabilities and ContainerIdAllocator` |
| 13 | feat(config) | `feat(config): implement ConfigurateMenuLoader with YAML parsing` |
| 14 | feat(config) | `feat(config): implement ItemTemplateCompiler for item parsing` |
| 15 | feat(menu) | `feat(menu): implement MenuRegistry and MenuFactory` |
| 16 | feat(config) | `feat(config): implement InheritedMenuLoader with extends support` |
| 17 | feat(action) | `feat(action): implement MenuAction framework and Tier 1-2 actions` |
| 18 | feat(action) | `feat(action): implement Tier 3 actions` |
| 19 | feat(requirement) | `feat(requirement): implement Requirement framework and all built-in types` |
| 20 | feat(action) | `feat(action): implement DeluxeActionParser and action/requirement registries` |
| 21 | feat(adapter) | `feat(adapter): implement PlaceholderAPI and Vault adapters` |
| 22 | feat(menu) | `feat(menu): implement AnimatedSlotTemplate and MenuUpdateEngine` |
| 23 | feat(menu) | `feat(menu): implement PaginatedMenuTemplate and Paginator` |
| 24 | feat(cache) | `feat(cache): implement Caffeine-based player cache` |
| 25 | feat(menu) | `feat(menu): implement argument binding (%arg_N% placeholders)` |
| 26 | test(unit) | `test(unit): add domain model and utility unit tests` |
| 27 | test(unit) | `test(unit): add config loader and action parser unit tests` |
| 28 | test(arch) | `test(arch): add ArchUnit architecture tests` |
| 29 | test(integration) | `test(integration): add MockBukkit integration tests` |
| 30 | perf | `perf: implement item pooling, lazy packets, and view requirement batching` |
| 31 | fix(memory) | `fix(memory): harden session lifecycle and add metrics` |
| 32 | docs | `docs: add comprehensive README and documentation` |
| 33 | chore | `chore: finalize CHANGELOG, LICENSE, and release configuration` |

---

## Success Criteria

### Verification Commands (run via GitHub Actions, NOT locally)
```bash
# Build verification
./gradlew build --no-daemon

# Code quality
./gradlew checkstyleMain spotbugsMain

# Tests
./gradlew test

# Architecture
./gradlew test --tests "architecture.*"
```

### Final Checklist
- [ ] All "Must Have" present in codebase
- [ ] All "Must NOT Have" absent from codebase
- [ ] All GitHub Actions matrix combinations pass
- [ ] Plugin loads on Paper 1.21.4 without errors
- [ ] Plugin loads on Folia 1.21.4 without errors
- [ ] Menu opens via packet rendering (client-visible)
- [ ] Click handling triggers configured actions
- [ ] PlaceholderAPI integration works (when installed)
- [ ] Vault economy integration works (when installed)
- [ ] DeluxeMenus Tier 1-3 configs work without changes
- [ ] No memory leaks after 1000 open/close cycles
