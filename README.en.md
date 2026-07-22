<p align="center"><a href="README.md">简体中文</a> · <strong>English</strong></p>

# 🌌 The Singularity — GodEquip Plugin

GodEquip is a high-power equipment plugin for Luminode/Paper servers. It introduces the “Singularity” set: concept-level armor and weapons with extreme attributes, flight, and damage immunity, balanced by a continuous experience cost and restrictive side effects.

## Features

- Infinite-style armor, toughness, and weapon attributes
- Damage immunity while the wearer still has experience energy
- Creative-like flight when the full armor set is equipped
- Netherite equipment with Silence trim and diamond material styling
- Experience drain, pickup restrictions, and high weapon activation costs
- Persistent-data identification designed to coexist with other flight plugins

## Installation

1. Build or download `GodEquip-1.2-SNAPSHOT.jar`.
2. Place the JAR in the server's `plugins` directory.
3. Restart the server.

## Commands and Permissions

| Command or permission | Purpose |
| --- | --- |
| `/godset` | Give the complete Singularity equipment set |
| `godequip.admin` | Allow `/godset` |
| `godequip.admin.fly` | Exempt an administrator from the plugin's flight-state enforcement |

## Compatibility

- Paper or Spigot 1.20+
- A server build with Armor Trim API support
- Designed to avoid interfering with flight granted by CMI, Essentials, AdvancedEnchantments, and similar plugins

## Balance Model

The armor drains 1,000 XP per second. When energy is exhausted, armor and flight effects are removed. The sword and bow require at least 10,000 XP and consume 10,000 XP per use.

Use the plugin only on servers where you are authorized to install and configure gameplay extensions.

## License

No license file is currently included. All rights remain with the copyright holder unless a license is added.
