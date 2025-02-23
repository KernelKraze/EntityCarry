# EntityCarry Plugin

EntityCarry is a powerful and flexible Minecraft plugin that allows players to carry entities and other players. With a comprehensive permission system and intuitive controls, it provides a balanced and controlled way to transport entities within your server.

## Features

- Carry any entity or player with proper permissions
- Intuitive sneak-based carrying mechanism
- Customizable cooldown system
- Permission-based access control
- Player consent system for being carried
- Entity type blacklisting
- Safe entity release mechanism

## Commands

- `/carryaccept` (Aliases: caccept, acceptcarry)
  - Accepts a carry request from another player
  - Requires: entitycarry.be_carried permission

- `/carryreload` (Alias: creload)
  - Reloads the plugin configuration
  - Requires: entitycarry.admin permission

- `/carrytoggle` (Alias: ctoggle)
  - Toggles whether you can be carried by other players
  - Requires: entitycarry.be_carried permission

## Permissions

### Administrative
- `entitycarry.admin`: Allows reloading plugin configuration
- `entitycarry.bypass.cooldown`: Bypasses carry cooldown restrictions

### Basic Usage
- `entitycarry.use`: Basic carrying functionality
- `entitycarry.be_carried`: Allows being carried by others
- `entitycarry.carry.player`: Allows carrying other players

### Entity-Specific
- `entitycarry.entity.*`: Allows carrying all entity types
- `entitycarry.type.passive`: Allows carrying passive mobs
- `entitycarry.type.hostile`: Allows carrying hostile mobs

### Wildcard Permission
- `entitycarry.*`: Grants all plugin permissions

## Installation

1. Download the EntityCarry plugin JAR file
2. Place the JAR in your server's `plugins` folder
3. Restart your server or load the plugin
4. Configure permissions for your users as needed

## Configuration

The plugin uses a configuration file to manage various settings including:
- Cooldown durations for different entity types
- Carry timeout duration
- Entity blacklist

## Support

For issues, suggestions, or contributions, please visit our GitHub repository.

---

*Plugin developed by KernelKraze - Version 1.0.0*