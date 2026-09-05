# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added

- A "Being told about a suspected alt without running a command" scenario in the [User Guide](USER_GUIDE.md), covering the join-time alt notification: what triggers it, that it fires only on the first login from a given address, what the log line and the notification say, how recipients are chosen through `notify-users`, that the shipped entries are examples to be replaced, and why a notification may never arrive. The behaviour itself is unchanged; it was previously documented nowhere an operator would look for it (see [#107](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/107)).
- A `Dev Release` workflow, which republishes a rolling `dev` prerelease of `main` on every non-documentation push. This is what Dan's Plugin Manager's experimental channel installs from: `/dpm get alternateaccountfinder --experimental` reads `releases/tags/dev`, so without it there is nothing for that command to download. The prerelease is unreleased, unreviewed code and is marked as such.

### Fixed

- Alt notifications delivered through the RPKit notification library now skip a recipient RPKit cannot resolve, with a warning naming that account, instead of throwing a `NullPointerException` on a background thread. A `notify-users` entry RPKit holds no Minecraft profile for — an account that has not joined, or one that predates the RPKit installation — previously produced a bare stack trace in the console naming no configuration entry, and the same happened when either RPKit service was unavailable, which is reachable while RPKit is still starting up. The other two backends were unaffected. No IP address appears in the new warnings (see [#106](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/106)).
- `/aaf accounts <ip>` and `/aaf alts <player>` now list accounts most recently seen first, with the account UUID breaking ties, instead of in an order decided by a hash map and by whatever the database happened to return. The first entry a moderator reads is now the account most likely to still be in play, and two runs of the same command list the same accounts in the same sequence. For `/aaf alts`, the timestamp that decides the position is the last time the candidate was seen on an address it shares with the subject. The same order applies to the join notification and the "Found potential alts" log line (see [#104](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/104)).
- `/aaf alts <player>` no longer resolves the named account on the server's main thread. Bukkit's name-to-account lookup may make a blocking web request whenever the name is not in the server's user cache — a misspelling, or a player who has never joined — which stalled the whole server for the duration of that request. The lookup now runs inside the asynchronous task that was already doing the rest of the command's work (see [#103](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/103)).
- A misspelled or empty `database.dialect` in `config.yml` is now reported as a startup message naming the key, quoting the offending value and giving the two dialects the plugin ships drivers for, after which the plugin disables itself. Previously the value went straight to jOOQ, which answered with `No enum constant org.jooq.SQLDialect.mariadb` — a message that names neither `config.yml` nor an acceptable value — and did so only after the connection pool had already been opened. The value is also matched case-insensitively now, so `h2` and `mariadb` are accepted alongside `H2` and `MARIADB` (see [#101](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/101)).
- The `Dev Release` workflow now retries publishing the `dev` prerelease before giving up. The release and its tag have to be deleted and recreated for the tag to move to the new commit, and a transient API failure inside that window previously left the repository with no `dev` release at all until the workflow was re-run by hand. Each attempt now starts from a clean slate, and an exhausted retry fails loudly.
- The database connection pool is now closed when the plugin is disabled. It previously stayed open for the lifetime of the server process, so every `/reload` — and every disable performed by a plugin manager such as Dan's Plugin Manager — left the old pool's connections and threads running while the next startup built a second pool beside them (see [#97](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/97)).

## [3.0.0-SNAPSHOT-8-8-2026] – 2026-08-08

### Changed

- AlternateAccountFinder is now developed AI-first. Day-to-day feature work, grooming, review and maintenance run through AI agents working directly against this repository, with the maintainers setting direction and approving what lands. The major version bump marks that change in how the project is built — it is not a break in behaviour, configuration or stored data, and existing installations can upgrade in place. Released as `3.0.0-SNAPSHOT-8-8-2026`: the AI-first line has not yet been verified in live operation, and the dated snapshot designation stays until it has.
- The "You do not have permission to use this command." message from `/aaf accounts` and `/aaf alts` is now red, like every other error message those commands send (see [#75](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/75)).
- The usage messages of `/aaf accounts` and `/aaf alts`, and the command descriptions shown by `plugin.yml`, now write their required argument as `<ip>` and `<player>` rather than `[ip]` and `[player]`. Square brackets conventionally mark an argument as optional, while both arguments are mandatory; the angle-bracket form already used by `COMMANDS.md` is now used everywhere (see [#83](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/83)).
- `/aaf alts` tab-completion now suggests only online players instead of every account the server has cached data for. On a long-lived server that offline list can number in the tens of thousands, and Bukkit builds an `OfflinePlayer` for each one on every keystroke; a moderator checking an offline account can still type its full name (see [#76](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/76)).

### Removed

- Removed the `/aaf ips` sub-command and its `aaf.ips` permission. Exposing the list of IP addresses a player has used was a privacy concern (see [#44](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/44)). Click/hover actions on `/aaf accounts` and `/aaf alts` results no longer invoke `/aaf ips`.

### Fixed

- An account that shares more than one IP address with the joining player is no longer listed once per shared address. `/aaf alts`, the join notification, and the "Found potential alts" log line each named such an account repeatedly; the potential-alt lookup is now distinct (see [#80](https://github.com/Dans-Plugins/AlternateAccountFinder/pull/80)).
- Join notifications and the "Found potential alts" log line no longer print the literal string `null` for a potential alt the server has no cached name for. Such an account is now identified by its UUID, as it already was in `/aaf accounts` and `/aaf alts` output (see [#80](https://github.com/Dans-Plugins/AlternateAccountFinder/pull/80)).
- `/aaf alts` tab-completion no longer fails when the server has an account with no cached name. Such an account's name is reported as `null` by Bukkit, which previously threw a `NullPointerException` inside the completer and dropped every suggestion; unknown names are now skipped instead. Account listings in `/aaf accounts` and `/aaf alts` also fall back to the account's UUID rather than printing the literal string `null` (see [#74](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/74)).
- The startup IP migration no longer re-encrypts addresses it cannot read. Previously any value that failed to decrypt was assumed to be plaintext, so if the encryption key file was lost or replaced while the migration marker was absent, existing ciphertext was encrypted a second time and reported as a successful migration. A value is now only encrypted when it also parses as an IPv4 or IPv6 address; anything else is left untouched, reported in the startup log with the likely cause, and counted as unmigrated so the marker is not written (see [#67](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/67)).
- Fixed a possible `NullPointerException` (and a silently dropped login record) when a player disconnects immediately after joining, before the async login-recording task runs. The player's address is now resolved on the main thread while it is still available (see [#65](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/65)).
- A malformed entry in the `notify-users` config list no longer throws an unhandled exception that silently drops every recipient listed after it. Invalid entries are now skipped with a warning naming the offending value, and the remaining recipients are still notified (see [#66](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/66)).

### Security

- IP addresses are now encrypted at rest using deterministic AES-256 (ECB mode) instead of being stored as plaintext, so lookups (accounts-by-IP, alt detection) still work while the raw address is no longer readable directly from the database (see [#45](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/45)). The encryption key is generated on first startup and stored in the plugin's data folder with `0600` permissions.
- Existing plaintext IP addresses from installs predating this change are automatically migrated to the encrypted format on plugin startup, with a completion marker so the migration only runs once (see [#46](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/46)).
- The startup IP migration no longer writes IP addresses to the server log. When a record could not be encrypted, the log line included the raw address alongside the player's UUID; it now names the account only (see [#70](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/70)).
- `/aaf accounts` tab-completion no longer suggests the IP addresses of online players. Pressing Tab after `/aaf accounts` now returns no suggestions, since enumerating raw IPs there reintroduced the disclosure `/aaf ips` was removed for (see [#64](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/64)).

## [2.0.0]

### Changed

- Rewrote the plugin for Spigot 1.17+ (v2 by Ren Binden).
- Migrated data layer to jOOQ with Flyway migrations.
- Added HikariCP connection pooling.
- Added support for MariaDB in addition to the embedded H2 database.
- Added integration with the Mailboxes and RPKit notification systems.
