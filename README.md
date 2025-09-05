# EntityCarry

A Minecraft plugin that allows players to carry entities and other players.

## Usage

1. **Double-sneak** (sneak twice quickly) to activate carry mode
2. **Right-click** on entities to carry them
3. **Sneak** while carrying to release entities
4. Carry mode auto-expires after 3 seconds

## Requirements

- Minecraft 1.21.4
- Paper server
- Java 21

## Permissions

- `entitycarry.use` - Basic carry functionality
- `entitycarry.carry.player` - Carry other players
- `entitycarry.admin` - Reload configuration
- `entitycarry.bypass.cooldown` - Skip cooldowns

## Commands

- `/carrytoggle` - Toggle being carried by others
- `/carryaccept` - Accept carry request
- `/carryreload` - Reload config (admin)

## Building

```bash
./gradlew build
```

JAR output: `build/libs/EntityCarry-1.0.0.jar`