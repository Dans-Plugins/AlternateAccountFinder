# CLAUDE.md

Guidance for AI coding agents working in this repository. Read it before making changes, and
verify anything it says against the source before relying on it.

## What This Repository Is

Alternate Account Finder is a Spigot/Paper plugin (API version 1.17+) that records the IP address
each account logs in from and reports accounts that share one, so server staff can identify
alternate accounts.

The plugin is one Gradle module. `AlternateAccountFinder#onEnable` wires everything together:
a HikariCP pool over the configured JDBC URL, Flyway migrations, a jOOQ `DSLContext`,
`IpEncryption`, `LoginRepository`, `LoginService`, one notification service, the
`PlayerJoinListener`, the `/aaf` executor, and bStats.

- **Language / build:** Java 17, Gradle (Groovy DSL) with the Shadow and jOOQ plugins
- **Database:** jOOQ 3.18 with Flyway migrations; embedded H2 in `MODE=MYSQL` by default,
  MariaDB supported
- **Optional integrations:** Mailboxes and RPKit, both detected at runtime
- **Distribution:** SpigotMC; the release workflow uploads `build/libs/*-all.jar`

### Package Layout

All source lives under `src/main/java/com/dansplugins/detectionsystem/`:

| Package | Contents |
|---------|----------|
| (root) | `AlternateAccountFinder` — the `JavaPlugin` entry point and startup wiring |
| `commands/` | `AafCommand` (dispatch and tab completion), `AafAccountsCommand`, `AafAltsCommand`, `PlayerNames` |
| `encryption/` | `IpEncryption` (deterministic AES over stored addresses), `StoredAddressClassifier` |
| `listeners/` | `PlayerJoinListener` |
| `logins/` | `LoginRepository`, `LoginService`, and the `AccountInfo` / `AddressInfo` / `AccountAddressInfo` / `AddressAccountInfo` value types |
| `notifications/` | `NotificationService` and its `Mailboxes`, `Rpk` and `Message` implementations |

Resources live under `src/main/resources/`: `plugin.yml`, `config.yml`, and Flyway migrations
under `com/dansplugins/detectionsystem/db/migration/`.

## Conventions

### Privacy

Player IP addresses are this project's sensitive data, and exposing them is its primary failure
mode. `/aaf ips` was removed for that reason (#44), address-suggesting tab completion was removed
after it reintroduced the same disclosure (#64), and the startup migration was changed to log only
UUIDs (#70). No new code path may surface an address — or a list of addresses — to command output,
the server log, tab-completion suggestions, or hover/click text. When a record has to be named in
output or a log line, name the account, not the address.

### Layering

Database access goes through `LoginRepository`; commands and listeners call `LoginService` and
never reach the `DSLContext` themselves. New classes follow the existing package structure above.

### Encryption

`IpEncryption` uses `AES/ECB/PKCS5Padding` with a single stored key, which makes encryption
deterministic: `encrypt(x).equals(encrypt(x))`. Every equality lookup depends on that —
`getAddressInfo`, `getLoginCount`, the self-join in `getPotentialAlts`, and the `ON CONFLICT`
upsert in `saveLogin` all compare ciphertext. A non-deterministic scheme cannot be introduced
without rewriting those lookups (for example onto a separate lookup digest).

The key file is `ip-encryption.key` in the plugin data folder, created on first startup with
`0600` permissions. Never log it, never commit it, and keep the startup backup warning intact —
losing the key makes every stored address unreadable.

### Migrations

New SQL goes under `src/main/resources/com/dansplugins/detectionsystem/db/migration/` as
`V<n>__<description>.sql`, and must run on both H2-in-`MODE=MYSQL` and MariaDB: use
`MODIFY COLUMN` rather than `ALTER COLUMN`, and restate `NOT NULL` when altering a primary-key
column, since MySQL and MariaDB drop the constraint otherwise. `V2__Encrypt_existing_ip_addresses.sql`
is the worked example.

Data conversion that needs the encryption key runs in the application rather than in SQL, because
the key lives outside the database. Such a conversion must be idempotent or guarded by a marker
file — `AlternateAccountFinder#migrateExistingIpAddresses` writes `ip-migration-v2.complete` only
when no record was left unmigrated, so a partial run retries on the next startup.

### Commands and Permissions

`AafCommand` routes each subcommand to its own executor class and delegates tab completion the
same way. Every subcommand it routes belongs in the `commands.aaf.description` block of
`plugin.yml`, and every permission checked in source belongs under `permissions:` there with a
sensible default (`op` for both existing nodes). Required arguments are written `<ip>` and
`<player>`; square brackets are reserved for optional ones.

`@Override` goes on every method overriding a Bukkit API or superclass method.

### User-Facing Strings

Command output, error messages and notification text are hardcoded at their use site and sent
through `CommandSender.sendMessage(...)`. There is no `lang/` layer; change strings where they are
used. Errors are red (`ChatColor.RED`).

### Notifications

One `NotificationService` is chosen at startup — Mailboxes if that plugin is present, otherwise
RPKit's notification lib, otherwise plain chat messages. Call sites use the selected service and
do not re-implement that fallback.

## Research Grounding

None. No research corpus, benchmark, paper, or dataset backs this repository's design; it is a
Minecraft server plugin. Design decisions are recorded in issues, pull requests and `CHANGELOG.md`,
and those are the things to cite when a change needs a rationale. Do not invent a research
justification for a change here.

## Testing

- **Framework:** JUnit 5 (`org.junit.jupiter:junit-jupiter`, `useJUnitPlatform()` in `build.gradle`)
- **Location:** `src/test/java/`, mirroring the package of the class under test, named
  `<ClassName>Test.java`
- **Commands:** `./gradlew clean build` for a verification build (it runs the tests),
  `./gradlew test` for the tests alone; `.\gradlew.bat` on Windows
- **CI:** the `Build` workflow runs `./gradlew clean build` on every push and pull request
  against `main`

No mock framework is configured. Prefer real or in-memory collaborators — H2 in memory for
database-backed tests, `@TempDir` for filesystem state. Where a Bukkit interface has to be stood
in for, a `java.lang.reflect.Proxy` over that interface keeps the test dependency-free; see
`AafCommandTest`. Branches that reach the running server stay uncovered by design and say so in
the test.

Cover new behavior with a test wherever the class can be exercised without a server. For a bug
fix, confirm the test actually fails without the fix rather than assuming it does. Never weaken an
existing assertion to make a new test pass.

A green build is not coverage of everything: CI exercises the JVM build against H2 only, so it
cannot execute MariaDB-specific SQL or any Bukkit runtime path. Changes to either need hand review
and a real-environment check, and the gap belongs in the pull request description.

## Commit and Pull Request Conventions

These are documented for contributors in [CONTRIBUTING.md](CONTRIBUTING.md); the same rules apply
to agent-authored changes.

- Branch from `main` as `feature/`, `fix/`, `docs/` or `chore/` plus a short hyphenated description
- Write commit subjects in the imperative mood with no trailing period; a Conventional Commits
  type prefix is accepted but not required
- Credit an AI assistant with a `Co-Authored-By:` trailer naming the model that actually ran, kept
  on its own line with a HEREDOC — never copy a model name from an earlier commit
- Write `Closes #<number>` in the pull request description for every issue it resolves; a bare
  `#<number>` links the issue but leaves it open
- Squash merging is the default, which is why most subjects on `main` end in `(#<number>)`
- Stage files by name; `git add -A` sweeps in agent scratch state the project `.gitignore` does
  not cover
- Add a `CHANGELOG.md` entry under `[Unreleased]` for any change a server operator would notice

## Documentation Sources of Truth

Each row below owns its subject. When behavior changes, update the owning document in the same
pull request rather than describing the change only in the pull request body.

| Document | Owns | Check against |
|----------|------|---------------|
| [README.md](README.md) | What the plugin is, installation, test-server setup, links to every other document | The build and Docker files, and the documents it links |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Contributor workflow: branches, commits, issue linking, merging, testing | This file, `build.gradle`, `.github/ISSUE_TEMPLATE/` |
| [USER_GUIDE.md](USER_GUIDE.md) | Server-operator scenarios, key-backup guidance, the permissions table | `plugin.yml`, the command executors, `IpEncryption` |
| [COMMANDS.md](COMMANDS.md) | Every `/aaf` subcommand: usage string, permission node, example | `AafCommand` dispatch and `plugin.yml` |
| [CONFIG.md](CONFIG.md) | Every `config.yml` key with type, default and description, plus the data-folder files | `src/main/resources/config.yml` and the code that reads each key |
| [CHANGELOG.md](CHANGELOG.md) | Operator-visible change history, classified per Keep a Changelog | The pull requests merged since the last release |
| [.github/copilot-instructions.md](.github/copilot-instructions.md) | Repository guidance for the Copilot reviewer | This file and the source tree |

`src/main/resources/plugin.yml` and `src/main/resources/config.yml` are themselves sources of
truth: `plugin.yml` must declare every command routed in `AafCommand` and every permission checked
in source, and `config.yml` must carry every key `CONFIG.md` documents, at the same default.

`.github/copilot-instructions.md` is known to have drifted from the source tree
([#62](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/62)) and is authoritative for
nothing this file also covers; where the two disagree, this file and the source win until that
issue is resolved.
