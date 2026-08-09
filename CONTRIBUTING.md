# Contributing

## Thank You

Thank you for your interest in contributing to Alternate Account Finder! This guide will help you get started.

## Links

- [Website](https://dansplugins.com)
- [Discord](https://discord.gg/xXtuAQ2)

## Requirements

- A GitHub account
- Git installed on your local machine
- A Java IDE or text editor
- A basic understanding of Java

## Getting Started

1. [Sign up for GitHub](https://github.com/signup) if you don't have an account.
2. Fork the repository by clicking **Fork** at the top right of the repo page.
3. Clone your fork: `git clone https://github.com/<your-username>/AlternateAccountFinder.git`
4. Open the project in your IDE.
5. Build the plugin: `./gradlew build`
   If you encounter errors, please open an issue.

## Identifying What to Work On

### Issues

Work items are tracked as [GitHub issues](https://github.com/Dans-Plugins/AlternateAccountFinder/issues).
New issues are opened from a [template](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/new/choose):
**Bug Report** for something the plugin does wrong, **Feature Request** for a new capability or an
improvement to an existing one.

### Milestones

Issues may be grouped into [milestones](https://github.com/Dans-Plugins/AlternateAccountFinder/milestones) when work is being planned for a specific release.

## Making Changes

1. Make sure an issue exists for the work. If not, create one.
2. Switch to `main`: `git checkout main`
3. Create a branch: `git checkout -b <prefix>/<short-description>`
4. Make your changes.
5. Test your changes.
6. Commit: `git commit -m "Describe the change"`
7. Push: `git push origin <branch-name>`
8. Open a pull request against `main`, and write `Closes #<number>` in the description so the
   issue closes when the pull request is merged.
9. Address review feedback.

### Branch Names

Branch from `main` using one of the prefixes already in use in this repository, followed by a
short hyphenated description:

| Prefix | For | Example |
|--------|-----|---------|
| `feature/` | New capabilities and additional test coverage | `feature/login-service-tests` |
| `fix/` | Bug fixes | `fix/nullable-account-names` |
| `docs/` | Documentation-only changes | `docs/fix-docker-test-server-instructions` |
| `chore/` | Releases, build and tooling changes | `chore/release-3.0.0` |

### Commit Messages

Write the subject in the imperative mood — "Add the missing permission node", not "Added" or
"Adds" — with no trailing period. A Conventional Commits type prefix (`fix:`, `chore:`) is
accepted but not required; both forms appear in the history.

If an AI assistant produced the change, credit it with a `Co-Authored-By:` trailer naming the
model that actually ran — not a model name copied from an earlier commit. Git matches the
trailer key case-insensitively, so the `Co-authored-by:` spelling also present in the history
is equivalent. A HEREDOC keeps the trailer on its own line:

```bash
git commit -m "$(cat <<'EOF'
Describe the change in the imperative mood

Co-Authored-By: <model name> <noreply@anthropic.com>
EOF
)"
```

### Linking Issues

Use `Closes #<number>` in the pull request description for every issue the pull request
resolves. A bare `#<number>` links the issue but leaves it open.

### Merging

Squash merging is the default, which is why most commit subjects on `main` end in
`(#<number>)`. A handful of merge commits sit alongside them where a pull request was merged
without squashing; prefer the squash so each pull request lands as one commit.

### Changelog

Any change a server operator would notice — command behavior, configuration, messages,
stored data — gets an entry under `[Unreleased]` in [CHANGELOG.md](CHANGELOG.md), classified
per [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

### User-Facing Strings

User-facing strings (command output, error messages, notification text) are
currently hardcoded in the Java source under `src/main/java/`. There is no
separate language-file layer yet — change the strings at their use site.

## Testing

Run a verification build with:

Linux: `./gradlew clean build`  
Windows: `.\gradlew.bat clean build`

### Unit Tests

Automated tests use JUnit 5 and live under `src/test/java/`, mirroring the package of the class
under test (for example `src/test/java/com/dansplugins/detectionsystem/encryption/IpEncryptionTest.java`).
The verification build runs them; to run only the tests:

Linux: `./gradlew test`  
Windows: `.\gradlew.bat test`

Cover new behavior with a test where the class can be exercised without a running server. Classes
that depend on the Bukkit API (command executors, event listeners) are generally tested through the
service, repository, or helper class holding their logic.

No mock framework is configured; prefer real or in-memory collaborators (H2 in memory for
database-backed tests, `@TempDir` for filesystem state). Where a Bukkit interface has to be stood in
for, a `java.lang.reflect.Proxy` over that interface keeps the test dependency-free — see
`AafCommandTest` for the pattern. Branches that reach the server itself stay uncovered by design and
are noted as such in the test.

### Manual Testing

For manual testing, build and run a local Spigot server with the plugin installed, either with
Docker Compose:

```
docker compose up
```

or with Docker directly:

```
docker build -t aaf-test-server .
docker run -p 25565:25565 aaf-test-server
```

## Questions

Ask in the [Discord server](https://discord.gg/xXtuAQ2).
