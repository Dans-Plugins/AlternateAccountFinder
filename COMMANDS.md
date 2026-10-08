# Commands Reference

## AAF Commands

All commands are sub-commands of `/aaf`. Running `/aaf` on its own, or with a sub-command it does not recognise, prints `Usage: /aaf [accounts, alts]`.

---

### /aaf accounts \<ip\>

**Description:** Lists all player accounts that have logged in from the specified IP address, along with each account's login count and first/last login timestamps. Timestamps are recorded and shown in UTC, in ISO-8601 form (for example `2026-10-08T14:03:12`, followed by fractional seconds where the database stores them), not in the server's local time. The most recently seen account is listed first. Banned players are highlighted in red, and accounts the server has no cached name for are listed by UUID. If no account has logged in from the address, the command replies `No accounts found for <ip>`; if the argument cannot be read as an address, it replies `Invalid IP address.`  
**Permission:** `aaf.accounts`  
**Usage:** `/aaf accounts <ip>`  
**Example:** `/aaf accounts 192.168.1.1`

---

### /aaf alts \<player\>

**Description:** Lists the suspected alternate accounts of the specified player (i.e. accounts that share at least one IP address with the player), the most recently seen first. Banned players are highlighted in red, and accounts the server has no cached name for are listed by UUID. If no other account shares an address with the player — including when the name belongs to no account the plugin has recorded — the command replies `No potential alts found for <player>`.  
**Permission:** `aaf.alts`  
**Usage:** `/aaf alts <player>`  
**Example:** `/aaf alts Steve`
