# PacketMenu

[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4%20--%2026.1.2-brightgreen)](https://papermc.io)
[![Paper](https://img.shields.io/badge/Paper-1.21.4--R0.1--SNAPSHOT-brightgreen)](https://papermc.io)
[![Release](https://img.shields.io/github/v/release/Cebonk03/PacketMenu?include_prereleases)](https://github.com/Cebonk03/PacketMenu/releases)
[![Build](https://img.shields.io/github/actions/workflow/status/Cebonk03/PacketMenu/build.yml?branch=main)](https://github.com/Cebonk03/PacketMenu/actions)

Packet-based GUI menus for Paper and Folia servers. No Bukkit Inventory API. Packets all the way down.

PacketMenu sends menu windows using Minecraft protocol packets via PacketEvents 2.7.0. This means no inventory click events, no Bukkit Inventory API, and full control over the window state. It supports Paper 1.21.4 through 26.1.2 and also runs on Folia.

## Features

- Packet-based menu rendering -- opens, items, and slot updates all go through the Minecraft protocol directly
- No Bukkit Inventory API -- menus are virtual windows rendered entirely through packets
- DeluxeMenus-compatible YAML configuration -- drop-in familiar syntax with extensions
- 20+ action types including economy (Vault), placeholders (PlaceholderAPI), commands, sounds, permissions, broadcasts, and pagination
- 10 requirement types for access control and slot visibility
- Menu inheritance via `extends` field -- reuse and override parent menu definitions
- Animated slots -- cycle through multiple item frames at configurable intervals
- Paginated menus -- split data items across multiple pages with navigation buttons
- Folia-native scheduling with automatic Paper/Folia runtime detection
- PlaceholderAPI support for text, lore, titles, and requirement evaluation
- Vault economy integration for money actions and money requirements
- Configurate-YAML engine with descriptive error messages including file paths and node locations
- Player cache with three tiers: placeholder resolution, requirement results, and active sessions

## Dependencies

**Required (must be installed on the server):**

| Dependency | Version | Notes |
|---|---|---|
| Paper | 1.21.4 through 26.1.2 | Folia also supported |
| Java | 21 or later | |
| PacketEvents | 2.7.0 | External plugin, not shaded |

**Optional (compileOnly, runtime check):**

| Dependency | Version | Notes |
|---|---|---|
| PlaceholderAPI | 2.11.6 | Text placeholder resolution |
| Vault | 1.7.1 | Economy actions and requirements |

**Shaded dependencies (bundled inside the jar):**

| Dependency | Version |
|---|---|
| Configurate-YAML | 4.1.2 |
| Caffeine | 3.1.8 |

## Installation

1. Download `PacketEvents-2.7.0.jar` from the [CodeMC repository](https://github.com/retrooper/packetevents) and place it in your server's `plugins/` folder.
2. Download the latest `PacketMenu.jar` from the [GitHub Releases](https://github.com/Cebonk03/PacketMenu/releases) page.
3. Place `PacketMenu.jar` in your server's `plugins/` folder.
4. Restart or reload the server.
5. Create menu YAML files in `plugins/PacketMenu/menus/`.

PacketEvents must load before PacketMenu. The plugin will refuse to enable if PacketEvents is not present.

## Quick Start

Create `plugins/PacketMenu/menus/example.yml`:

```yaml
menu_title: "<gold>Example Menu"
menu_type: CHEST
rows: 3
open_command: example

items:
  diamond:
    material: DIAMOND
    slot: 13
    display_name: "<green>Click Me!"
    lore:
      - "<gray>This is an example item"
      - "<dark_gray>Right-click to close"
    actions:
      - "[message] <green>You clicked the diamond!"
      - "[sound] entity.experience_orb.pickup 1.0 1.0"
      - "[close]"

filler_item:
  material: GRAY_STAINED_GLASS_PANE
  display_name: " "
```

Run `/packetmenu open example` in-game to open the menu.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `packetmenu.use` | true | Open menus and use basic commands |
| `packetmenu.admin` | op | Admin access to reload and management commands |
| `packetmenu.open.<menuId>` | true | Open a specific menu by its identifier |

Menu commands check `packetmenu.use` for the basic `open` subcommand and `packetmenu.admin` for `reload` and `list`.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/packetmenu open <menuId> [player] [args...]` | Open a menu for yourself or another player | `packetmenu.use` |
| `/packetmenu close` | Close your currently open menu | `packetmenu.use` |
| `/packetmenu reload` | Reload all menu configurations from disk | `packetmenu.admin` |
| `/packetmenu list` | Show active menu sessions | `packetmenu.admin` |
| `/packetmenu version` | Display the plugin version | none |

Tab completion works for subcommands, menu identifiers, and player names.

## Documentation

Full documentation is available in the `docs/` folder:

- [Configuration](docs/configuration.md) -- Complete YAML spec with examples
- [Actions](docs/actions.md) -- All action types with syntax and examples
- [Requirements](docs/requirements.md) -- All requirement types with syntax and examples
- [Developer API](docs/api.md) -- Custom actions and requirements
- [Migration](docs/migration.md) -- Migrating from DeluxeMenus

## Building from Source

```bash
./gradlew build
```

The compiled jar will be in `build/libs/PacketMenu-<version>.jar`.

Checkstyle and SpotBugs run during the build. Use `./gradlew check` to run all checks and tests.

## License

MIT License. See [LICENSE](LICENSE) for details.
