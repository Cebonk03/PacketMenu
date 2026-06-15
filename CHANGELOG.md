# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.0.0] - 2025-12-15

### Added
- Initial release
- Packet-based GUI rendering (open window, items, clicks, close) via PacketEvents 2.7.0
- YAML-driven menu configuration with DeluxeMenus-compatible format (Tier 1-3)
- 20+ action types (message, console, player, close, sound, refresh, opengui, money, items, permissions, broadcast)
- 10+ requirement types (permission, money, item, exp, string, numeric, javascript)
- Animated items with configurable frame cycling
- Paginated menus with navigation items
- Template inheritance (extends: parent_menu_id)
- PlaceholderAPI integration (optional, graceful fallback)
- Vault economy integration (optional, graceful fallback)
- Folia-native scheduling with runtime Paper/Folia detection
- Hexagonal architecture with full ArchUnit enforcement
- CI/CD matrix testing (Java 21/25 x MC 1.21.4-26.1.2)
