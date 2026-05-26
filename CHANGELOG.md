# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Removed

- Removed the `/aaf ips` sub-command and its `aaf.ips` permission. Exposing the list of IP addresses a player has used was a privacy concern (see [#44](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/44)). Click/hover actions on `/aaf accounts` and `/aaf alts` results no longer invoke `/aaf ips`.

## [2.0.0]

### Changed

- Rewrote the plugin for Spigot 1.17+ (v2 by Ren Binden).
- Migrated data layer to jOOQ with Flyway migrations.
- Added HikariCP connection pooling.
- Added support for MariaDB in addition to the embedded H2 database.
- Added integration with the Mailboxes and RPKit notification systems.
