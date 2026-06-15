# Migration from DeluxeMenus

PacketMenu supports a subset of the DeluxeMenus configuration format. This guide covers the differences, compatibility tiers, and step-by-step migration.

## Compatibility Tiers

| Tier | Support | Features |
|---|---|---|
| **Tier 1** | Full | Menu structure, items, slots, actions, basic requirements |
| **Tier 2** | Full with minor differences | Action parsing, MiniMessage format, scheduler behaviour |
| **Tier 3** | Functional but not identical | Economy, permissions, broadcasts, sound effects |
| **Tier 4** | NOT supported | Custom GUI types, NMS features, non-Chest GUIs, private features, JavaScript GUI extensions |

### Tier 1: Core Menu Structure

These features work identically to DeluxeMenus:

- Menu title, type, rows
- Open commands (single and list)
- Item definitions with material, display name, lore
- Slot and slots specification
- Item flags, enchantments, custom model data
- Priority system for overlapping slots
- Filler items

### Tier 2: Action Syntax with Minor Differences

Action strings follow the same `[type] args` format. Differences:

- PacketMenu uses Adventure/MiniMessage for text formatting instead of legacy colour codes. Legacy `&` codes are still supported through automatic fallback conversion.
- PacketMenu uses `SchedulerPort` for thread-safe execution on Folia. All game-state-modifying actions are scheduled through the appropriate region thread.
- The default action parser is registered in `DeluxeActionParser` with all 20 built-in types.

### Tier 3: Economy and Broadcast Actions

These features work but with implementation differences:

- `[takemoney]` and `[givemoney]` require Vault (same as DeluxeMenus). Balance check is synchronous; withdrawal/deposit is scheduled on the player region thread.
- `[broadcast]` uses Adventure Component broadcasting via `Bukkit.broadcast(Component)`.
- `[broadcastsound]` plays the sound on the global scheduler, filtered by world.
- `[givepermission]` and `[takepermission]` use Bukkit `PermissionAttachment` (persists for the session only).

### Tier 4: Not Supported

The following DeluxeMenus features are not planned for PacketMenu:

- Non-chest inventory types (crafting table, anvil, beacon, etc.)
- Custom GUI classes and Java-based GUI extensions
- NMS/Reflection-based features
- Private/internal DeluxeMenus extensions
- Skull hash-based textures via HeadDatabase (use skull_texture with base64 instead)
- DeluxeMenus `%js_%` placeholders in non-JavaScript contexts

## Configuration Format Differences

### Title Format

DeluxeMenus uses `§` colour codes:
```yaml
menu_title: '&6Shop Menu'
```

PacketMenu accepts both MiniMessage and legacy format:
```yaml
menu_title: "<gold>Shop Menu"                  # MiniMessage
menu_title: "&6Shop Menu"                       # Legacy (auto-converted)
```

### Menu Identifier

DeluxeMenus uses an explicit `menu_name` field:
```yaml
menu_name: example
```

PacketMenu uses the YAML file name (without extension) as the identifier. The `id` field is read from raw YAML for inheritance resolution but the registered identifier is the file stem.

### Open Command

DeluxeMenus:
```yaml
open_command: /example
open_commands:
  - /example
  - /ex
```

PacketMenu:
```yaml
open_command: example                          # Slash optional
open_command:                                  # Same format for list
  - example
  - ex
```

### Action Arguments

DeluxeMenus:
```yaml
actions:
  - '[player] say Hello'
  - '[console] give %player_name% diamond 1'
```

PacketMenu (same syntax, but uses MiniMessage for text):
```yaml
actions:
  - "[player] say Hello"
  - "[console] give %player_name% diamond 1"
```

### Enchantments Format

DeluxeMenus uses inline maps:
```yaml
enchantments:
  - sharpness: 5
  - unbreaking: 3
```

PacketMenu uses the same format. Internally, enchantment keys are resolved via `NamespacedKey.fromString()`.

### Requirements Syntax

DeluxeMenus uses nested requirement definitions. PacketMenu requirement parsing is under development and currently uses pass-through stubs for view and click requirements.

## Step-by-Step Migration Guide

### Step 1: Install PacketEvents

PacketMenu requires PacketEvents 2.7.0 as a server plugin. Install it before PacketMenu.

```
plugins/
  PacketEvents-2.7.0.jar
  PacketMenu.jar
```

### Step 2: Copy Menu Files

Copy your DeluxeMenus menu YAML files from `plugins/deluxemenus/gui/` to `plugins/PacketMenu/menus/`.

```bash
cp plugins/deluxemenus/gui/*.yml plugins/PacketMenu/menus/
```

### Step 3: Remove DeluxeMenus-Specific Fields

Remove or convert these DeluxeMenus-specific fields:

| DeluxeMenus Field | Action |
|---|---|
| `menu_name` | Remove. File name becomes the identifier. |
| `open_command` | Keep. Minor format differences handled automatically. |
| `use_filler` | Replace with `filler_item` material definition. |
| `fill_items` | Replace with `filler_item`. |
| `keep_open` | Not supported. Remove or ignore. |
| `auto_refresh` | Replace with `update_interval`. |
| `update` | Supported (per-item). Keep. |
| `view_requirement` | Stub support for now. Keep or leave out. |

### Step 4: Update Action Strings

Most action strings work as-is. Review these cases:

1. Messages with legacy colour codes work automatically but consider migrating to MiniMessage format for richer formatting.
2. Economy actions work if Vault is installed.
3. Permission actions work but only persist for the current session.
4. Broadcast actions work with Adventure Component formatting.

### Step 5: Review Commands

Replace `/dm open <menu>` with `/packetmenu open <menu>`:

```yaml
# DeluxeMenus command
/dm open shop

# PacketMenu command
/packetmenu open shop
```

### Step 6: Check Permissions

| DeluxeMenus | PacketMenu |
|---|---|
| `deluxemenus.use` | `packetmenu.use` |
| `deluxemenus.admin` | `packetmenu.admin` |
| `deluxemenus.open.<menu>` | `packetmenu.open.<menu>` |

### Step 7: Reload

Use `/packetmenu reload` to reload menu configurations.

```bash
/packetmenu reload
```

## Known Limitations

1. **View requirements** are currently parsed as pass-through. All slots are visible regardless of conditions.
2. **Click requirements** are currently parsed as pass-through. All click actions execute regardless of conditions.
3. **Menu inheritance** uses `extends` (the YAML field), not the `parent_menu` field. The `parent_menu` field tracks the menu that opened the current one for nested menus.
4. **Non-chest GUI types** are not supported (no crafting table, anvil, beacon, hopper, etc.).
5. **JavaScript placeholders** in item names and lore (e.g., `%js_...%`) are not supported. Use PlaceholderAPI or MiniMessage for dynamic content.
6. **Custom GUI classes** written in Java for DeluxeMenus cannot be ported directly. See the [API documentation](api.md) for the equivalent PacketMenu extension mechanism.
7. **Folia threading** means actions that modify game state run on region threads. This is transparent in most cases but may cause issues with code that assumes the main server thread.
8. **PermissionAttachment** for `givepermission`/`takepermission` only lasts for the player's current session.

## Quick Reference

```
DeluxeMenus concept            PacketMenu equivalent
──────────────────────────────────────────────────
menu_name                      File name (stem)
menu_title (legacy)            menu_title (MiniMessage)
open_command                   open_command
items/{name}                   items/{name}
slots/slot                     slots/slot
priority                       priority
material                       material
display_name                   display_name
lore                           lore
enchantments                   enchantments
item_flags                     item_flags
custom_model_data              custom_model_data
filler_item                    filler_item
update_interval                update_interval
[player] command               [player] command
[console] command              [console] command
[message] text                 [message] text
[close]                        [close]
[sound] key vol pitch          [sound] key vol pitch
[open] menu                    [openguimenu] menu
[takemoney] amt                [takemoney] amt
[givemoney] amt                [givemoney] amt
[giveitem] spec                [giveitem] spec
[takeitem] spec                [takeitem] spec
[broadcast] msg                [broadcast] msg
[refresh]                      [refresh]
deluxemenus.use                packetmenu.use
deluxemenus.admin              packetmenu.admin
/deluxemenus open              /packetmenu open
/deluxemenus reload            /packetmenu reload
/deluxemenus list              /packetmenu list
```
