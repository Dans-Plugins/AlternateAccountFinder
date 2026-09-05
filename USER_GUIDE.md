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

On that first startup the plugin also generates an encryption key and prints a warning block about backing it up. Read [IP Address Encryption](#ip-address-encryption) before you start collecting data you would not want to lose.

## IP Address Encryption

The plugin encrypts every IP address it stores, so the raw addresses are not readable directly from the database. Encryption uses AES-256 with a single key that is generated on first startup and kept in the plugin's data folder:

```
plugins/AlternateAccountFinder/ip-encryption.key
```

On Linux and other POSIX systems the plugin restricts this file to owner read/write (`600`) at the moment it creates it. On systems without POSIX file permissions (such as Windows) it logs that permission restrictions are unavailable and leaves the file as-is. Permissions are only applied to a key the plugin generates itself — if you restore the file from a backup, check its permissions yourself.

The other files the plugin keeps in that folder are described under [Files in the data folder](CONFIG.md#files-in-the-data-folder) in the Configuration Guide.

### Back up the key file

**Back up `ip-encryption.key` alongside your database backups, and keep the two together.** The plugin logs a warning block naming the file's full path on every startup, because a database backup without its matching key file is not a usable backup.

If the key file is missing when the server starts, the plugin does not stop — it generates a **new** key and carries on. Addresses stored under the old key cannot be decrypted with the new one, which degrades detection in ways that are easy to miss:

- `/aaf accounts <ip>` no longer finds accounts recorded before the key changed. It encrypts the IP you type with the current key and matches on that, so only logins recorded after the change come back.
- `/aaf alts <player>` still links accounts to each other *within* each key era, because it compares stored addresses against one another rather than decrypting them. It will not link an account recorded before the key changed to one recorded after, even when both used the same IP.

There is no way to recover the old addresses without the original key file. The startup migration will not touch those rows: an address it can neither decrypt nor read as a plaintext IP is left exactly as stored and reported in the log by account UUID, so restoring the original key file later still recovers them.

If the key file exists but is not exactly 32 bytes, the plugin treats it as corrupted and fails to enable rather than quietly generating a replacement — the server keeps running without it. Restore the file from a backup instead of deleting it.

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

The plugin will list every player name that has connected from that IP, along with each account's login count and first/last login timestamps. The account that logged in from that IP most recently is listed first. Banned players are highlighted in red. An account the server has no cached name for — for example one that has not connected since the server's player cache was cleared — is listed by its UUID instead.

### Finding suspected alternate accounts for a player

Use this to get a list of players who share at least one IP address with the target player:

```
/aaf alts <player>
```

Example:

```
/aaf alts Steve
```

The account seen most recently on an address it shares with the target player is listed first. Players who are banned are highlighted in red in the result list. As with `/aaf accounts`, an account the server has no cached name for is listed by its UUID.

### Being told about a suspected alt without running a command

Both commands above have to be typed by somebody who already suspects something. The plugin can also announce a suspected alt by itself, at the moment the account joins.

The announcement fires when both of these hold:

- the joining account has never been seen from that IP address before, and
- that IP address already has at least one other account on record.

Only that account's first join from that address triggers it. An account that keeps connecting from the same address is not re-announced, so a busy shared address does not produce a message on every join.

When it fires, two things happen. The server log gains a line naming the joining player and the accounts it shares an address with:

```
[AlternateAccountFinder] Found potential alts for Steve: Alex, Notch
```

And each account listed under `notify-users` in `config.yml` is sent a notification titled `Steve - potential alts` reading `Steve is potentially an alt of: Alex, Notch`. Accounts are listed most recently seen first, as they are by `/aaf alts`, and an account the server has no cached name for is named by its UUID. No IP address appears in either the log line or the notification.

To choose who is told, put their account UUIDs under `notify-users` — see [notify-users](CONFIG.md#notify-users) in the Configuration Guide for the key itself. **The shipped `config.yml` carries two example UUIDs, which are not yours; replace them with your own staff's before relying on the notification.** An entry that is not a valid UUID is skipped with a warning naming the entry, and the remaining entries are still notified. An empty list disables the notification, and the log line is still written.

If a notification never arrives, the delivery backend is the usual reason. One is chosen at startup: the **Mailboxes** plugin if it is installed, otherwise the **RPKit** notification library, otherwise a plain in-game chat message. Only the first of those reaches a recipient who is offline at the time — with the plain chat fallback, a notification aimed at an offline account is dropped, and with RPKit it needs RPKit to hold a profile for that account, which is reported in the server log as a warning naming the account when it does not.

## Permissions

| Permission   | Default | Description                                      |
|--------------|---------|--------------------------------------------------|
| `aaf.accounts` | op    | Allows viewing all accounts for an IP address    |
| `aaf.alts`     | op    | Allows viewing suspected alt accounts for a player |
