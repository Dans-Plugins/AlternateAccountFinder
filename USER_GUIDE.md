# User Guide

## Prerequisites

- A Minecraft server running Spigot or Paper (API version 1.17 or higher).
- Operator (`op`) privileges on the server, or the relevant `aaf.*` permissions granted to your role.

## First Steps

After placing the plugin JAR in your `plugins/` folder and restarting the server, Alternate Account Finder will automatically begin recording the IP addresses of connecting players. No additional setup is required to start collecting data.

To verify the plugin loaded correctly, run:

```
/aaf
```

You should see a usage message listing the available sub-commands.

On that first startup the plugin also generates an encryption key and prints a warning block about backing it up. Read [IP Address Encryption](#ip-address-encryption) before you go any further — it is the one piece of setup that cannot be repaired after the fact.

## IP Address Encryption

The plugin encrypts every IP address it stores, so the raw addresses are not readable directly from the database. Encryption uses AES-256 with a single key that is generated on first startup and kept in the plugin's data folder:

```
plugins/AlternateAccountFinder/ip-encryption.key
```

On Linux and other POSIX systems the plugin restricts this file to owner read/write (`600`) after creating it. On systems without POSIX file permissions (such as Windows) it logs that permission restrictions are unavailable and leaves the file as-is.

### Back up the key file

**Back up `ip-encryption.key` alongside your database backups, and keep the two together.** The plugin logs a warning block naming the file's full path on every startup, because a database backup without its matching key file is not a usable backup.

If the key file is missing when the server starts, the plugin does not stop — it generates a **new** key and carries on. Addresses stored under the old key cannot be decrypted with the new one, which degrades detection in ways that are easy to miss:

- `/aaf accounts <ip>` no longer finds accounts recorded before the key changed. It encrypts the IP you type with the current key and matches on that, so only logins recorded after the change come back.
- `/aaf alts <player>` still links accounts to each other *within* each key era, because it compares stored addresses against one another rather than decrypting them. It will not link an account recorded before the key changed to one recorded after, even when both used the same IP.

There is no way to recover the old addresses without the original key file.

If the key file exists but is not exactly 32 bytes, the plugin treats it as corrupted and refuses to start rather than quietly generating a replacement. Restore the file from a backup instead of deleting it.

### Files in the data folder

Besides `config.yml`, the plugin writes two files into `plugins/AlternateAccountFinder/` that are not configuration and should not be edited by hand. See the [Configuration Guide](CONFIG.md#files-in-the-data-folder) for details.

## Common Scenarios

### Finding all accounts for a known IP address

Use this when you suspect multiple players are sharing one IP:

```
/aaf accounts <ip>
```

Example:

```
/aaf accounts 192.168.1.1
```

The plugin will list every player name that has connected from that IP, along with each account's login count and first/last login timestamps. Banned players are highlighted in red.

### Finding suspected alternate accounts for a player

Use this to get a list of players who share at least one IP address with the target player:

```
/aaf alts <player>
```

Example:

```
/aaf alts Steve
```

Players who are banned are highlighted in red in the result list.

## Permissions

| Permission   | Default | Description                                      |
|--------------|---------|--------------------------------------------------|
| `aaf.accounts` | op    | Allows viewing all accounts for an IP address    |
| `aaf.alts`     | op    | Allows viewing suspected alt accounts for a player |
