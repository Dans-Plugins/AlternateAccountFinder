# Alternate Account Finder

## Description

Alternate Account Finder is a Minecraft plugin that attempts to identify accounts that have used the same IP address, helping server administrators detect and manage alternate (alt) accounts.

## Installation

### First Time Installation

1. Download the plugin from [SpigotMC](https://www.spigotmc.org/resources/alternate-account-finder.83290/).
2. Place the jar in the `plugins` folder of your server.
3. Restart your server.

## Usage

### Documentation

- [User Guide](USER_GUIDE.md) – Getting started and common scenarios
- [Commands Reference](COMMANDS.md) – Complete list of all commands
- [Configuration Guide](CONFIG.md) – Detailed configuration options

### Wiki & Additional Resources

- [FAQ](https://github.com/Dans-Plugins/AlternateAccountFinder/wiki/FAQ)

## Support

You can find the support Discord server [here](https://discord.gg/xXtuAQ2).

### Experiencing a bug?

Please fill out a bug report [here](https://github.com/Dans-Plugins/AlternateAccountFinder/issues/new).

## Contributing

- [CONTRIBUTING.md](CONTRIBUTING.md)
- [Notes for Developers](https://github.com/Dans-Plugins/AlternateAccountFinder/wiki/Developer-Notes)

## Testing

### Verification Build

Linux:

```
./gradlew clean build
```

Windows:

```
.\gradlew.bat clean build
```

If you see `BUILD SUCCESSFUL`, the build has passed.

### Unit Tests

The project has a JUnit 5 test suite under `src/test/java/`. The verification build above runs it, and it can also be run on its own:

Linux:

```
./gradlew test
```

Windows:

```
.\gradlew.bat test
```

## Development

### Test Server

A `Dockerfile` and a `compose.yml` are available to build and run a Spigot test server with the plugin installed.

#### Setup with Docker Compose

1. Build and start the test server: `docker compose up`

The compose file builds the image from the `Dockerfile` in this repository and publishes port 25565.

#### Setup with Docker

1. Build the test server image: `docker build -t aaf-test-server .`
2. Run the test server: `docker run -p 25565:25565 aaf-test-server`

#### Stopping the Test Server

Press `Ctrl+C`. If the server was started in the background, run `docker compose down` when Docker Compose was used, or `docker stop <container-id>` otherwise.

## Authors and Acknowledgements

### Developers

| Name              | Main Contributions |
|-------------------|--------------------|
| Daniel Stephenson | Creator            |
| Ren Binden        | v2                 |

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE) (GPL-3.0).

You are free to use, modify, and distribute this software, provided that:

- Source code is made available under the same license when distributed.
- Changes are documented and attributed.
- No additional restrictions are applied.

See the [LICENSE](LICENSE) file for the full text of the GPL-3.0 license.

## Project Status

This project is in active development.

### bStats

You can view the bStats page for the plugin [here](https://bstats.org/plugin/bukkit/Alternate%20Account%20Finder/9834).

## Roadmap

- [Known Bugs](https://github.com/Dans-Plugins/AlternateAccountFinder/issues?q=is%3Aopen+is%3Aissue+label%3Abug)
- [Planned Features](https://github.com/Dans-Plugins/AlternateAccountFinder/issues?q=is%3Aopen+is%3Aissue+label%3AEpic)
- [Planned Improvements](https://github.com/Dans-Plugins/AlternateAccountFinder/issues?q=is%3Aopen+is%3Aissue+label%3Aimprovement)

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a history of changes.
