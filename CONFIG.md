# Configuration Guide

All options are set in `plugins/AlternateAccountFinder/config.yml`. The file is created automatically on first run.

The plugin also writes two non-configuration files into the same folder — see [Files in the data folder](#files-in-the-data-folder) at the end of this guide.

---

## database.url

**Type:** string  
**Default:** `jdbc:h2:./plugins/AlternateAccountFinder/aaf;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false`  
**Description:** The JDBC connection URL for the plugin's data store. The default uses an embedded H2 database stored in the plugin's data folder. To use MariaDB/MySQL instead, replace this with a `jdbc:mariadb://` URL and set `database.dialect` to `MARIADB`.

**Example (H2 — default):**

```yaml
database:
  url: 'jdbc:h2:./plugins/AlternateAccountFinder/aaf;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false'
```

**Example (MariaDB):**

```yaml
database:
  url: 'jdbc:mariadb://localhost:3306/aaf'
```

---

## database.dialect

**Type:** string  
**Default:** `H2`  
**Description:** The SQL dialect that jOOQ uses when generating queries. Set to `H2` for the embedded database or `MARIADB` for MariaDB/MySQL.

**Example:**

```yaml
database:
  dialect: H2
```

---

## database.username

**Type:** string  
**Default:** `sa`  
**Description:** The username used to authenticate with the database. The default `sa` account is used for H2. Change this when connecting to an external database.

**Example:**

```yaml
database:
  username: 'aafuser'
```

---

## database.password

**Type:** string  
**Default:** `''` (empty)  
**Description:** The password used to authenticate with the database. Leave empty for the default H2 setup. Set an appropriate password when connecting to an external database.

**Example:**

```yaml
database:
  password: 'supersecret'
```

---

## notify-users

**Type:** list of UUIDs  
**Default:** *(example UUIDs — replace with your own)*  
**Description:** A list of player UUIDs that will be notified when a player joins for the *first time* from a given IP and that IP already has at least one other associated account on record. Notifications are not re-sent for subsequent joins from the same IP. Remove all entries or leave the list empty to disable notifications.

Notification delivery depends on which optional plugins are installed:

- If the **Mailboxes** plugin is present, notifications are delivered as mailbox messages.
- Otherwise, if the **RPKit** notification library is present, notifications go through the RPKit notification system.
- If neither is installed, notifications fall back to a plain in-game chat message sent to the recipient if they are online.

**Example:**

```yaml
notify-users:
  - 0a9fa342-3139-49d7-8acb-fcf4d9c1f0ef
```

---

## Files in the data folder

`plugins/AlternateAccountFinder/` holds two files besides `config.yml`. Neither is configuration, and neither should be edited by hand.

| File | Created | Purpose |
|------|---------|---------|
| `ip-encryption.key` | First startup | The AES-256 key used to encrypt stored IP addresses. Restricted to owner read/write (`600`) on POSIX systems. **Must be backed up with your database** — see [IP Address Encryption](USER_GUIDE.md#ip-address-encryption). |
| `ip-migration-v2.complete` | After the plaintext-to-encrypted migration finishes | A marker that records the one-time migration of pre-existing plaintext IP addresses. While it is present, startup skips the migration scan entirely. |

### ip-encryption.key

Losing this file means every IP address already stored becomes permanently unreadable, and the plugin will generate a replacement key and continue running rather than stopping. The [User Guide](USER_GUIDE.md#back-up-the-key-file) describes exactly which lookups stop working. If the file is present but not exactly 32 bytes, the plugin treats it as corrupted and refuses to start — restore it from a backup rather than deleting it.

### ip-migration-v2.complete

On the first startup after upgrading from a version that stored IP addresses as plaintext, the plugin scans every login record and encrypts any address that is still plaintext. It writes this marker once that pass completes with no failures, so later startups skip the scan instead of re-checking every record. If the pass had failures, the marker is not written and the migration is retried on the next startup.

Deleting the marker forces the scan to run again on the next startup. Do not delete it as a routine measure, and in particular do not delete it if `ip-encryption.key` has been lost or replaced: the scan identifies plaintext by attempting to decrypt each stored value with the current key, so addresses encrypted under a key that is no longer present would be misread as plaintext and encrypted a second time.
