# Configuration Guide

All options are set in `plugins/AlternateAccountFinder/config.yml`. The file is created automatically on first run.

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
