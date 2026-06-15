# Actions

Actions are the primary way to define behaviour when a player clicks a menu slot. Each action is a single-line string following the DeluxeMenus format:

```
[actionType] arguments... <delay=N>
```

The delay suffix (in server ticks) is optional. When specified, the action execution is deferred by the given number of ticks.

## Syntax

```
[actionType] arg1 arg2 ... <delay=20>
```

- `actionType` -- the action identifier (alphanumeric)
- `arg1 arg2 ...` -- space-separated arguments for the action
- `<delay=N>` -- optional delay in server ticks (20 ticks = 1 second)

## Action Reference

### [message]

Sends a formatted message to the clicking player's chat.

```
[message] <green>Hello! This is a message
[message] &cYou don't have permission to do that
```

- Arguments: MiniMessage-formatted text (joined with spaces)
- Scheduling: none (synchronous)
- Notes: Supports MiniMessage gradients, colours, hover events, and click events. Legacy section codes (&c, &a, etc.) are also supported.

### [console]

Executes a command from the server console.

```
[console] give %player_name% diamond 1
[console] ban %player_name% Cheating
```

- Arguments: command string (without leading slash)
- Scheduling: global server thread via SchedulerPort
- Notes: Runs with full operator privileges. Use `%player_name%` or other placeholders to reference the clicking player.

### [player]

Executes a command as the clicking player.

```
[player] say Hello everyone!
[player] spawn
[player] home
```

- Arguments: command string (without leading slash)
- Scheduling: player region thread via SchedulerPort
- Notes: The command runs as the player who clicked. Respects permission checks for the command.

### [close]

Closes the currently open menu.

```
[close]
```

- Arguments: none
- Scheduling: handled by action runner (packet-based close)

### [sound]

Plays a sound effect for the clicking player.

```
[sound] entity.experience_orb.pickup 1.0 1.0
[sound] minecraft:block.note_block.pling 0.5 2.0
[sound] ui.button.click 1.0 1.0
```

- Arguments: `<soundKey> [volume=1.0] [pitch=1.0]`
- Scheduling: player region thread via SchedulerPort
- Notes: The sound key can be a namespaced key or a short name (namespace defaults to `minecraft:`).

### [refresh]

Re-evaluates view requirements and refreshes the displayed slots.

```
[refresh]
```

- Arguments: none
- Scheduling: handled by action runner
- Notes: Useful after an action that changes slot visibility conditions (e.g. giving a permission that makes a new slot visible).

### [opengui]

Opens a menu by its file name (without `.yml` extension).

```
[opengui] shop
[opengui] example_menu
```

- Arguments: menu identifier
- Scheduling: deferred to action runner
- Notes: Not yet implemented in the current build. Use `[openguimenu]` instead.

### [openguimenu]

Opens a menu by its registered identifier.

```
[openguimenu] example
[openguimenu] shop_items
[openguimenu] admin_panel
```

- Arguments: menu identifier (as registered in the MenuRegistry)
- Scheduling: deferred to action runner
- Notes: Opens the target menu for the clicking player. The menu must be registered and loaded.

### [takemoney]

Withdraws virtual currency from the player via Vault.

```
[takemoney] 100.0
[takemoney] 50.5
```

- Arguments: amount (decimal number)
- Scheduling: player region thread via SchedulerPort (withdrawal only; balance check is synchronous)
- Notes: Requires Vault on the server. Returns `ActionResult.Failure("Economy is not available")` if Vault is absent. Returns `ActionResult.Failure("Insufficient funds")` if the player does not have enough money. The balance check is performed synchronously before the withdrawal.

### [givemoney]

Deposits virtual currency into the player's balance via Vault.

```
[givemoney] 100.0
[givemoney] 50.0
```

- Arguments: amount (decimal number)
- Scheduling: player region thread via SchedulerPort
- Notes: Requires Vault on the server. Returns `ActionResult.Failure("Economy is not available")` if Vault is absent.

### [giveitem]

Gives an item to the player's inventory. Excess items (when inventory is full) drop on the ground at the player's location.

```
[giveitem] material:DIAMOND amount:1 name:<green>Special Diamond lore:<gray>Given by menu
```

- Arguments: item specification string (parsed by the action registry)
- Scheduling: player region thread via SchedulerPort
- Notes: The item is constructed from the `ItemStackSnapshot` definition. If the material is unknown the action returns `ActionResult.Failure`.

### [takeitem]

Removes matching items from the player's inventory by material type.

```
[takeitem] material:DIAMOND amount:1
[takeitem] material:STONE amount:5
```

- Arguments: item specification string
- Scheduling: player region thread via SchedulerPort
- Notes: Items are matched by material type only (ignoring display name, lore, enchantments, etc.). The action removes up to the specified amount of the matching material from the player's inventory.

### [givepermission]

Grants a permission node to the player via Bukkit PermissionAttachment. The permission persists until the player disconnects.

```
[givepermission] some.permission.node
[givepermission] myplugin.vip
```

- Arguments: permission node
- Scheduling: player region thread via SchedulerPort
- Notes: The permission is attached to the player for the current session. It is automatically cleaned up when the player disconnects from the server.

### [takepermission]

Removes a permission node from the player by adding a negative override via Bukkit PermissionAttachment.

```
[takepermission] some.permission.node
[takepermission] myplugin.vip
```

- Arguments: permission node
- Scheduling: player region thread via SchedulerPort
- Notes: Adds a negative override (`false`) for the permission, ensuring it evaluates to `false` regardless of other grants. The override is automatically cleaned up when the player disconnects.

### [broadcast]

Sends a formatted message to all online players on the server.

```
[broadcast] <gold>An important announcement!
[broadcast] &cThe server will restart in 5 minutes
```

- Arguments: MiniMessage-formatted text (joined with spaces)
- Scheduling: global server thread via SchedulerPort
- Notes: Supports MiniMessage formatting. The message is broadcast to every player currently online.

### [jsonbroadcast]

Parses a JSON-serialized Adventure Component string and broadcasts the result to all online players.

```
[jsonbroadcast] {"text":"Hello!","color":"gold","bold":true}
```

- Arguments: JSON string (Adventure Component format)
- Scheduling: global server thread via SchedulerPort
- Notes: Uses GsonComponentSerializer to deserialize the JSON. If the JSON is malformed, the action returns `ActionResult.Failure` with a descriptive message, and nothing is broadcast.

### [broadcastsound]

Plays a sound for all players in the same world as the clicking player.

```
[broadcastsound] entity.lightning_bolt.thunder 1.0 1.0
[broadcastsound] minecraft:entity.firework_rocket.launch 0.5 1.5
```

- Arguments: `<soundKey> [volume=1.0] [pitch=1.0]`
- Scheduling: global server thread via SchedulerPort
- Notes: Only players in the same world as the clicking player hear the sound. Useful for world-specific announcements.

### [placeholder]

Resolves placeholders in a message for the clicking player and sends the resolved message.

```
[placeholder] <green>Your balance: %vault_eco_balance%
[placeholder] <yellow>Your rank: %luckperms_primary_group_name%
```

- Arguments: MiniMessage-formatted text with placeholders
- Scheduling: synchronous (resolved immediately via PlaceholderPort)
- Notes: Requires PlaceholderAPI on the server. Resolves all `%placeholder%` patterns for the clicking player before sending. Falls back gracefully to no-op when PlaceholderAPI is absent.

### [delay]

Pauses action execution for the specified number of ticks. This action wraps the next action in the sequence.

```
[action] [message] First message
[action] [delay] 20
[action] [message] This appears 1 second later
```

- Arguments: ticks (integer, number of server ticks to wait)
- Scheduling: handled by action runner as `ActionResult.Delayed`
- Notes: The delay value must be non-negative. A delay of 0 ticks causes immediate re-execution. The action wraps the following action into a `Delayed` result.

## Combining Actions

Multiple actions can be listed under a slot. They execute in order from top to bottom.

```yaml
items:
  example:
    material: DIAMOND
    slot: 13
    display_name: "<green>Click me"
    actions:
      - "[message] <yellow>First message"
      - "[sound] entity.experience_orb.pickup 1.0 1.0"
      - "[console] give %player_name% diamond 1"
      - "[close]"
```

## Delayed Execution

Append `<delay=N>` to any action to defer its execution.

```yaml
actions:
  - "[message] <red>Warning..." <delay=20>
  - "[message] <green>Go! <delay=20>
```

This sends the first message immediately, waits 20 ticks (1 second), then sends the second message.

## Placeholder Arguments

Actions that accept text support `%placeholder%` patterns when PlaceholderAPI is installed.

```yaml
actions:
  - "[message] <green>Hello %player_name%!"
  - "[console] eco give %player_name% 100"
  - "[placeholder] <gold>Balance: %vault_eco_balance%"
```

## Error Handling

- Unknown action types are silently ignored (no-op action returning `ActionResult.Success`).
- Invalid or blank action strings produce a no-op action.
- Economy actions gracefully return failure messages when Vault is absent.
- Malformed JSON in `[jsonbroadcast]` produces a failure (nothing is broadcast).
- Invalid material in `[giveitem]` returns a failure.
- Actions that require platform access (sounds, commands, Vault, permissions) are routed through the appropriate scheduler to ensure thread safety on Folia.

## Full Action List

| Type | Description | Requires |
|---|---|---|
| `message` | Send chat message | none |
| `console` | Execute console command | none |
| `player` | Execute command as player | none |
| `close` | Close menu | none |
| `sound` | Play sound | none |
| `refresh` | Refresh menu display | none |
| `opengui` | Open menu by file name | none |
| `openguimenu` | Open menu by identifier | none |
| `takemoney` | Withdraw money | Vault |
| `givemoney` | Deposit money | Vault |
| `giveitem` | Give item to player | none |
| `takeitem` | Remove item from player | none |
| `givepermission` | Grant permission | none |
| `takepermission` | Revoke permission | none |
| `broadcast` | Broadcast message | none |
| `jsonbroadcast` | Broadcast JSON component | none |
| `broadcastsound` | Play sound for all in world | none |
| `placeholder` | Send resolved placeholder text | PlaceholderAPI |
| `delay` | Delay next action | none |
