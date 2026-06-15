# Configuration

PacketMenu uses DeluxeMenus-compatible YAML configuration files. Each file in the `plugins/PacketMenu/menus/` directory defines one menu. Files must have a `.yml` or `.yaml` extension and the file name (without extension) becomes the menu identifier used by commands and actions.

## Menu-Level Nodes

```yaml
menu_title: "<gold>My Menu"           # Window title (MiniMessage or legacy)
menu_type: CHEST                        # Layout type (CHEST or GENERIC_9xN)
rows: 6                                 # Number of rows for CHEST type (1-6, default 6)
open_command: example                   # Command to open this menu (string or list)
open_requirement:                       # Optional requirement to open (see requirements docs)
  requirement:
    type: has permission
    permission: some.node
items: {}                               # Slot item definitions
filler_item:                            # Optional filler for empty slots
  material: GRAY_STAINED_GLASS_PANE
  display_name: " "
update_interval: 0                      # Ticks between automatic updates (0 = no updates, max 1200)
close_on_click_outside: true            # Close when clicking outside the menu (default true)
extends: parent_menu_id                 # Optional parent menu for inheritance
parent_menu: parent_menu_id             # The menu that opened this one (for nested menus)
```

### menu_title

The window title displayed at the top of the menu. Supports MiniMessage format and legacy section color codes.

```yaml
menu_title: "<gold>Shop Menu"
menu_title: "&6Shop Menu"              # Legacy format (auto-detected)
menu_title: "<gradient:gold:red>Fancy Title"
```

If no title is specified, the menu identifier (file name stem) is used as a fallback.

### menu_type

Controls the visual layout and slot count of the menu window.

| Value | Slots | Description |
|---|---|---|
| `CHEST` | 9-54 | Standard chest layout (use with `rows` field) |
| `GENERIC_9x1` | 9 | Single row |
| `GENERIC_9x2` | 18 | Two rows |
| `GENERIC_9x3` | 27 | Three rows |
| `GENERIC_9x4` | 36 | Four rows |
| `GENERIC_9x5` | 45 | Five rows |
| `GENERIC_9x6` | 54 | Six rows |
| `GENERIC_3x3` | 9 | 3x3 grid |

When using `CHEST`, the `rows` field determines the size. Default rows is 6 if not specified. Rows are clamped to the range 1-6.

```yaml
menu_type: CHEST
rows: 3        # Creates a 27-slot menu (GENERIC_9x3)
```

### open_command

One or more commands that open this menu. The leading slash is optional and stripped automatically.

```yaml
open_command: shop
open_command: /shop                     # Leading slash is handled
open_command:                           # Multiple commands
  - shop
  - store
  - market
```

### open_requirement (stub)

An optional requirement that must pass for the menu to open. Syntax matches the requirements system. Currently parsed as a pass-through; full evaluation is pending final wiring.

### update_interval

Ticks between automatic re-evaluation of dynamic slots. Set to 0 (default) to disable updates. Maximum is 1200 (60 seconds).

```yaml
update_interval: 20    # Re-evaluate every second
```

### close_on_click_outside

Whether the menu window closes when a player clicks outside the menu area. Defaults to `true`.

### extends

Inherit fields and slots from another menu. The parent menu identifier must match a registered menu template. Inheritance merges scalar fields (child wins) and slots by `(slot, priority)` pair.

```yaml
# child_menu.yml
extends: parent_menu
menu_title: "<gold>Child Menu"       # Overrides parent title
```

See the inheritance section below for full merge rules.

## Items

Items are defined under the `items` map. Each key is an arbitrary item name used only for readability.

```yaml
items:
  my_item_key:                         # Arbitrary item name
    material: DIAMOND_SWORD            # Required: material name
    amount: 1                          # Stack size (default 1)
    slot: 13                           # Single slot (required if no slots)
    slots:                             # Multiple slots (alternative to slot)
      - 0
      - 1
      - 2
    priority: 0                        # Rendering priority (higher = on top, default 0)
    display_name: "<gold>Legendary Sword"
    lore:
      - "<gray>A mighty weapon"
      - "<dark_gray>+5 damage"
    enchantments:
      - sharpness: 5
      - unbreaking: 3
    item_flags:
      - HIDE_ATTRIBUTES
      - HIDE_ENCHANTS
    custom_model_data: 1001
    durability: 0                      # Damage value (0 = undamaged)
    nbt: "{some_nbt_data}"            # Raw NBT string
    skull_texture: "eyJ0ZXh0dXJlcyI6..."  # Base64 skull texture
    update: false                      # Re-evaluate view requirement periodically
    update_interval: 0                 # Update interval for this slot (if update is true)
    view_requirement: {}               # Optional visibility requirement (stub)
    click_requirements: []             # Optional click requirements (stub)
    actions:                           # Actions triggered on click
      - "[player] say Hello!"
      - "[message] <green>Clicked!"
```

### material

Material name in Minecraft 1.21.4 format. Supports both plain names and namespaced keys.

```yaml
material: DIAMOND
material: minecraft:diamond
material: PLAYER_HEAD
```

Resolution uses `Material.matchMaterial()` first, then falls back to `Material.getMaterial()` for legacy names.

### slot / slots

Every item must define either `slot` (single integer) or `slots` (list of integers). The slot index is 0-based and must be within the bounds of the menu type.

```yaml
slot: 22                               # Single slot (top-right in a 6-row chest)
slots: [0, 1, 2, 3, 4, 5, 6, 7, 8]    # Top row
```

### priority

When multiple items occupy the same slot, the item with the highest priority renders on top. Default priority is 0. Priorities must be non-negative.

```yaml
priority: 10
```

When combined with menu inheritance, the `(slot, priority)` pair is used as the merge key. A child item with the same slot and priority overrides the parent item.

### display_name

Item display name in MiniMessage format with legacy section code fallback.

```yaml
display_name: "<green>Health Potion"
display_name: "&c&lWarning Sign"
```

Omitting this field leaves the item with its default Minecraft name.

### lore

A list of lore lines, each parsed through MiniMessage.

```yaml
lore:
  - "<gray>Line one"
  - "<dark_gray>Line two"
  - "<gradient:red:gold>Fancy line"
```

### enchantments

Enchantments are specified as a list of single-entry maps. The key is the Minecraft namespaced enchantment name and the value is the level.

```yaml
enchantments:
  - sharpness: 5
  - unbreaking: 3
  - mending: 1
```

Valid enchantment keys include: `sharpness`, `unbreaking`, `efficiency`, `protection`, `fire_aspect`, `mending`, `silk_touch`, `power`, `punch`, `flame`, `infinity`, `luck_of_the_sea`, `lure`, `respiration`, `aqua_affinity`, `thorns`, `depth_strider`, `frost_walker`, `feather_falling`, `blast_protection`, `fire_protection`, `projectile_protection`, `knockback`, `smite`, `bane_of_arthropods`, `looting`, `sweeping`, `multishot`, `quick_charge`, `piercing`.

### item_flags

Hides or shows item attributes on the tooltip.

```yaml
item_flags:
  - HIDE_ATTRIBUTES
  - HIDE_ENCHANTS
  - HIDE_UNBREAKABLE
  - HIDE_ADDITIONAL_TOOLTIP
```

Valid flags: `HIDE_ENCHANTS`, `HIDE_ATTRIBUTES`, `HIDE_UNBREAKABLE`, `HIDE_DESTROYS`, `HIDE_PLACED_ON`, `HIDE_ADDITIONAL_TOOLTIP`, `HIDE_DYE`, `HIDE_ARMOR_TRIM`.

### custom_model_data

Integer value for custom item model data (used with resource packs).

```yaml
custom_model_data: 1001
```

### skull_texture

Base64-encoded skin texture for player heads. Only applies when `material` is `PLAYER_HEAD`.

```yaml
material: PLAYER_HEAD
skull_texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODY0Zjc3YWEzNmMxZmE5NTQ1MjQyZjY5YzU5MjQ1YjI3NTM4MzA4MzFhMjI2M2MyODQ5YTQ5NDNlYmExYzUifX19"
```

### nbt

Raw NBT string applied to the item. Format varies by Minecraft version.

```yaml
nbt: "{some_nbt_data}"
```

### update / update_interval

When `update` is `true`, the slot's `view_requirement` is re-evaluated periodically. The `update_interval` (per-item, overrides menu-level) controls how often.

```yaml
update: true
update_interval: 10
```

### view_requirement (stub)

Controls whether the slot is visible to the player. Follows the requirements syntax. Currently parsed as a pass-through; all slots are visible.

### click_requirements (stub)

Requirements that must pass before click actions execute. Currently parsed as pass-through; all clicks go through.

### actions

Actions triggered when a player clicks the item. Each entry is a DeluxeMenus-style action string.

```yaml
actions:
  - "[message] <green>You clicked!"
  - "[sound] entity.experience_orb.pickup 1.0 1.0"
  - "[player] say I clicked the menu"
  - "[close]"
```

See the [Actions documentation](actions.md) for the full list.

## Filler Item

The `filler_item` node defines a default item placed in every empty slot of the menu. It uses the same item format as regular items but without `slot` or `slots`.

```yaml
filler_item:
  material: GRAY_STAINED_GLASS_PANE
  display_name: " "
```

If no filler item is defined, empty slots remain empty.

## Menu Inheritance

The `extends` field lets one menu inherit fields and slots from another menu.

### Merge Rules

1. The parent template serves as the base.
2. The child's scalar fields override the parent's:
   - `menu_title`
   - `menu_type`
   - `open_command`
   - `filler_item`
   - `update_interval`
   - `close_on_click_outside`
3. The child's `open_requirement` overrides the parent's (if set).
4. Slots are merged by `(slot, priority)` pair:
   - Parent slots form the base set.
   - Child slots with the same `(slot, priority)` override parent slots.
   - Child slots with new `(slot, priority)` pairs are added.
   - Result is sorted by priority descending (highest first).

### Example

```yaml
# parent.yml
menu_title: "<gold>Base Menu"
menu_type: CHEST
rows: 3
items:
  filler:
    material: GRAY_STAINED_GLASS_PANE
    slot: 0
    priority: 0
    display_name: " "
```

```yaml
# child.yml
extends: parent
menu_title: "<green>Child Menu"       # Overrides title
items:
  diamond:
    material: DIAMOND
    slot: 13
    priority: 1                        # Higher priority than filler
    display_name: "<green>Diamond"
    actions:
      - "[message] You clicked the diamond"
```

### Restrictions

- Maximum inheritance depth is 10 levels.
- Circular inheritance is detected and rejected at load time with a descriptive error.
- The parent menu must be registered before the child (loading order follows alphabetical file ordering).
- If a referenced parent is missing, loading fails with a clear error.

## Validation

Menu configurations are validated eagerly at load time. Errors include:

- Missing or blank `material` on an item
- Unknown material name
- Slot index out of bounds for the menu type (0 to `type.size()-1`)
- Negative priority values
- `update_interval` outside the valid range (0-1200)
- Unknown enchantment names
- Unknown item flags
- Empty `slots` list
- Unknown menu type
- Circular inheritance chains
- Missing parent menu in inheritance

Error messages include the file name, configuration node path, and a description of the problem.
