# Copilot Instructions

This repository follows the DPC (Dans Plugins Community) conventions defined at
https://github.com/Dans-Plugins/dpc-conventions. Read those conventions before
making any changes.

## Technology Stack

- Language: Java
- Build tool: Gradle (Groovy DSL) with Shadow plugin for fat JARs
- Target platform: Spigot / Paper (API version 1.17+)
- Test framework: Not yet configured (prefer JUnit 5; add dependencies in build.gradle before writing tests)
- Database: jOOQ with Flyway migrations; H2 by default, MariaDB supported

## Project Structure

- `src/main/java/com/dansplugins/detectionsystem/` – Plugin source code
  - `commands/` – Command executors (`AafCommand`, `AafAccountsCommand`, `AafIpsCommand`, `AafAltsCommand`)
  - `listeners/` – Bukkit event listeners (e.g. `PlayerJoinListener`)
  - `logins/` – Business logic and repository for login/IP tracking
  - `notifications/` – Notification service integrations (Mailboxes, RPKit, message)
- `src/main/resources/` – `plugin.yml`, `config.yml`, and database migration scripts
- `src/test/java/` – (intended) unit test sources; create this directory when adding tests

## Coding Conventions

- All user-facing messages are sent directly via `CommandSender.sendMessage()`; if a `lang/` directory is added in the future, use it for every user-facing string instead of hard-coding messages.
- Follow the existing package structure (`com.dansplugins.detectionsystem.*`) when adding new classes.
- Annotate every command executor and event listener with `@Override` where applicable.
- Database access goes through the repository layer (`LoginRepository`) and is exposed via the service layer (`LoginService`).

## Contribution Workflow

- Branch from `main` for all changes.
- Open a pull request against `main`.
- Reference the related GitHub issue in every pull request description.
