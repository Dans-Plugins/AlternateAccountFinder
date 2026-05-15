# Commands Reference

## AAF Commands

All commands are sub-commands of `/aaf`.

---

### /aaf accounts \<ip\>

**Description:** Lists all player accounts that have logged in from the specified IP address.  
**Permission:** `aaf.accounts`  
**Usage:** `/aaf accounts <ip>`  
**Example:** `/aaf accounts 192.168.1.1`

---

### /aaf ips \<player\>

**Description:** Lists all IP addresses that the specified player has logged in from.  
**Permission:** `aaf.ips`  
**Usage:** `/aaf ips <player>`  
**Example:** `/aaf ips Steve`

---

### /aaf alts \<player\>

**Description:** Lists the suspected alternate accounts of the specified player (i.e. accounts that share at least one IP address with the player). Banned players are highlighted in red. Each result is clickable and runs `/aaf ips` for that account.  
**Permission:** `aaf.alts`  
**Usage:** `/aaf alts <player>`  
**Example:** `/aaf alts Steve`
