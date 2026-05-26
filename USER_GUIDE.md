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
