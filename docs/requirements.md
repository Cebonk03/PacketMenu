# Requirements

Requirements control access to menus and slots. They can be used as:

- **Open requirements** -- the player must satisfy them to open a menu
- **View requirements** -- the slot is only visible if the requirement passes
- **Click requirements** -- the click actions only fire if the requirement passes

## Syntax

Requirements are defined using the `type` field with type-specific arguments.

```yaml
open_requirement:
  requirement:
    type: has permission
    permission: some.node
```

## Built-in Requirements

### has_permission

Passes when the player has a specific permission node.

```yaml
requirement:
  type: has permission
  permission: myplugin.vip
```

- Arguments: `permission` (string, the permission node to check)
- Delegates to `PlayerHandle.hasPermission()`

### has_permissions

Passes when the player has at least a minimum number of permissions from a list.

```yaml
requirement:
  type: has permissions
  permissions:
    - myplugin.vip
    - myplugin.moderator
    - myplugin.admin
  minimum_count: 2
```

- Arguments:
  - `permissions` (list of strings, the permission nodes to check)
  - `minimum_count` (integer, minimum number of permissions the player must have)
- Short-circuits on reaching the minimum count

### has_money

Passes when the player has at least the specified amount of in-game currency via Vault.

```yaml
requirement:
  type: has money
  amount: 100.0
```

- Arguments: `amount` (decimal, minimum currency required)
- Requires Vault on the server (returns `false` if Vault is absent)
- Delegates to `EconomyPort.has()`

### has_item

Passes when the player's inventory contains at least the specified amount of a given material.

```yaml
requirement:
  type: has item
  material: DIAMOND
  amount: 5
```

- Arguments:
  - `material` (string, Bukkit Material name)
  - `amount` (integer, minimum number of items)
- Access the native Bukkit Player inventory via `PlayerHandle.nativePlayer()`
- Scans all inventory slots, including hotbar and armor (uses `PlayerInventory.getContents()`)

### has_exp

Passes when the player has at least the specified number of experience levels.

```yaml
requirement:
  type: has exp
  levels: 10
```

- Arguments: `levels` (integer, minimum XP levels required)
- Checks `player.getLevel() >= levels`

### string_equals

Passes when a placeholder-resolved input string equals an expected value.

```yaml
requirement:
  type: string equals
  input: "%player_name%"
  expected: "Notch"
```

- Arguments:
  - `input` (string, placeholder template to resolve)
  - `expected` (string, expected value after resolution)
- Both `input` and `expected` are resolved through PlaceholderPort before comparison
- The comparison is `.equals()` (exact match)

### string_contains

Passes when a placeholder-resolved input string contains an expected substring.

```yaml
requirement:
  type: string contains
  input: "%vault_eco_balance%"
  expected: "."
```

- Arguments:
  - `input` (string, placeholder template to resolve)
  - `expected` (string, expected substring after resolution)
- Both values are resolved through PlaceholderPort before checking `.contains()`

### number_greater

Passes when a placeholder-resolved numeric value is greater than a threshold.

```yaml
requirement:
  type: number greater
  value: "%vault_eco_balance%"
  threshold: "100"
```

- Arguments:
  - `value` (string, placeholder template resolving to a number)
  - `threshold` (string, placeholder template resolving to a number)
- Both values are parsed as `double` after placeholder resolution
- Returns `false` if either value cannot be parsed as a number
- Returns `true` when `value > threshold`

### number_less

Passes when a placeholder-resolved numeric value is less than a threshold.

```yaml
requirement:
  type: number less
  value: "%player_level%"
  threshold: "50"
```

- Arguments:
  - `value` (string, placeholder template resolving to a number)
  - `threshold` (string, placeholder template resolving to a number)
- Both values are parsed as `double` after placeholder resolution
- Returns `false` if either value cannot be parsed as a number
- Returns `true` when `value < threshold`

### javascript

Passes when a JavaScript expression evaluates to `true` (or truthy). The `player` variable is bound to the native Bukkit Player object.

```yaml
requirement:
  type: javascript
  expression: "player.getLevel() > 10 && player.getGameMode() == 'SURVIVAL'"
```

- Arguments: `expression` (string, JavaScript expression)
- Uses `javax.script.ScriptEngineManager` with Graal.js or Nashorn engine
- The `player` variable is bound to `context.viewer().nativePlayer()` (Bukkit Player)
- Returns `false` if no JavaScript engine is available or if the expression throws a `ScriptException`
- Non-boolean results are treated as truthy (any non-null value passes)

## Requirement Groups (AND/OR)

Multiple requirements can be combined using `requirement-groups` with a logic mode.

```yaml
open_requirement:
  requirement-groups:
    mode: AND
    groups:
      - requirement:
          type: has permission
          permission: myplugin.vip
      - requirement:
          type: has money
          amount: 50.0
    deny_commands:
      - "[message] &cYou need VIP rank and 50 coins to open this!"
```

- `mode`: `AND` or `OR`
  - `AND` -- all requirements must pass (vacuously true when empty)
  - `OR` -- at least one requirement must pass (vacuously false when empty)
- `deny_commands`: actions executed when the combined requirement fails

## Deny Actions

When a requirement fails, deny actions run instead of the normal click actions. These follow the same syntax as regular actions.

```yaml
click_requirements:
  - requirement:
      type: has money
      amount: 100.0
    deny_commands:
      - "[message] &cYou need at least 100 coins!"
      - "[sound] entity.villager.no 1.0 1.0"
```

## Open Requirements

Open requirements control whether a player can open a menu at all.

```yaml
open_requirement:
  requirement:
    type: has permission
    permission: mymenu.access
```

If the requirement fails, `MenuFactory.create()` returns `null` and the player sees an access denied message.

## View Requirements (stub)

View requirements control slot visibility. Currently parsed as pass-through (all slots are visible). Full evaluation is pending final wiring.

## Requirement Evaluation

- Requirements are evaluated through `RequirementEvaluator.evaluate(RequirementSet, RequirementContext)`
- AND mode short-circuits on the first `false`
- OR mode short-circuits on the first `true`
- Empty requirement map: AND = vacuously true, OR = vacuously false
- On failure, deny actions are executed in order with a default `ClickType.LEFT`

## Requirement List

| Type | Description | Requires |
|---|---|---|
| `has permission` | Player has a permission node | none |
| `has permissions` | Player has N of listed permissions | none |
| `has money` | Player has at least X currency | Vault |
| `has item` | Player has at least X of material | none |
| `has exp` | Player has at least X XP levels | none |
| `string equals` | Resolved string equals expected | PlaceholderAPI (optional) |
| `string contains` | Resolved string contains expected | PlaceholderAPI (optional) |
| `number greater` | Resolved number > threshold | PlaceholderAPI (optional) |
| `number less` | Resolved number < threshold | PlaceholderAPI (optional) |
| `javascript` | JavaScript expression is truthy | Graal.js or Nashorn |
