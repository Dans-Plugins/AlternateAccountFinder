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

## Development

### Test Server

A Docker-based test server is available for development.

#### Setup

1. Build the plugin: `./gradlew build`
2. Start the test server: `docker compose up`

#### Stopping the Test Server

```
docker compose down
```

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
- [Planned Improvements](https://github.com/Dans-Plugins/AlternateAccountFinder/issues?q=is%3Aopen+is%3Aissue+label%3Aenhancement)

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a history of changes.
