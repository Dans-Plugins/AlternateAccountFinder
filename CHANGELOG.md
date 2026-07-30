# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Security

- IP addresses are now encrypted at rest using deterministic AES-256 (ECB mode) instead of being stored as plaintext, so lookups (accounts-by-IP, alt detection) still work while the raw address is no longer readable directly from the database (see [#45](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/45)). The encryption key is generated on first startup and stored in the plugin's data folder with `0600` permissions.
- Existing plaintext IP addresses from installs predating this change are automatically migrated to the encrypted format on plugin startup, with a completion marker so the migration only runs once (see [#46](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/46)).
- The startup IP migration no longer writes IP addresses to the server log. When a record could not be encrypted, the log line included the raw address alongside the player's UUID; it now names the account only (see [#70](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/70)).
- `/aaf accounts` tab-completion no longer suggests the IP addresses of online players. Pressing Tab after `/aaf accounts` now returns no suggestions, since enumerating raw IPs there reintroduced the disclosure `/aaf ips` was removed for (see [#64](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/64)).

### Changed

- The "You do not have permission to use this command." message from `/aaf accounts` and `/aaf alts` is now red, like every other error message those commands send (see [#75](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/75)).
- `/aaf alts` tab-completion now suggests only online players instead of every account the server has cached data for. On a long-lived server that offline list can number in the tens of thousands, and Bukkit builds an `OfflinePlayer` for each one on every keystroke; a moderator checking an offline account can still type its full name (see [#76](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/76)).

### Fixed

- `/aaf alts` tab-completion no longer fails when the server has an account with no cached name. Such an account's name is reported as `null` by Bukkit, which previously threw a `NullPointerException` inside the completer and dropped every suggestion; unknown names are now skipped instead. Account listings in `/aaf accounts` and `/aaf alts` also fall back to the account's UUID rather than printing the literal string `null` (see [#74](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/74)).
- The startup IP migration no longer re-encrypts addresses it cannot read. Previously any value that failed to decrypt was assumed to be plaintext, so if the encryption key file was lost or replaced while the migration marker was absent, existing ciphertext was encrypted a second time and reported as a successful migration. A value is now only encrypted when it also parses as an IPv4 or IPv6 address; anything else is left untouched, reported in the startup log with the likely cause, and counted as unmigrated so the marker is not written (see [#67](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/67)).
- Fixed a possible `NullPointerException` (and a silently dropped login record) when a player disconnects immediately after joining, before the async login-recording task runs. The player's address is now resolved on the main thread while it is still available (see [#65](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/65)).
- A malformed entry in the `notify-users` config list no longer throws an unhandled exception that silently drops every recipient listed after it. Invalid entries are now skipped with a warning naming the offending value, and the remaining recipients are still notified (see [#66](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/66)).

### Removed

- Removed the `/aaf ips` sub-command and its `aaf.ips` permission. Exposing the list of IP addresses a player has used was a privacy concern (see [#44](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/44)). Click/hover actions on `/aaf accounts` and `/aaf alts` results no longer invoke `/aaf ips`.

## [2.0.0]

### Changed

- Rewrote the plugin for Spigot 1.17+ (v2 by Ren Binden).
- Migrated data layer to jOOQ with Flyway migrations.
- Added HikariCP connection pooling.
- Added support for MariaDB in addition to the embedded H2 database.
- Added integration with the Mailboxes and RPKit notification systems.
