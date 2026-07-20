# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Security

- IP addresses are now encrypted at rest using deterministic AES-256 (ECB mode) instead of being stored as plaintext, so lookups (accounts-by-IP, alt detection) still work while the raw address is no longer readable directly from the database (see [#45](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/45)). The encryption key is generated on first startup and stored in the plugin's data folder with `0600` permissions.
- Existing plaintext IP addresses from installs predating this change are automatically migrated to the encrypted format on plugin startup, with a completion marker so the migration only runs once (see [#46](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/46)).

### Removed

- Removed the `/aaf ips` sub-command and its `aaf.ips` permission. Exposing the list of IP addresses a player has used was a privacy concern (see [#44](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/44)). Click/hover actions on `/aaf accounts` and `/aaf alts` results no longer invoke `/aaf ips`.

## [2.0.0]

### Changed

- Rewrote the plugin for Spigot 1.17+ (v2 by Ren Binden).
- Migrated data layer to jOOQ with Flyway migrations.
- Added HikariCP connection pooling.
- Added support for MariaDB in addition to the embedded H2 database.
- Added integration with the Mailboxes and RPKit notification systems.
