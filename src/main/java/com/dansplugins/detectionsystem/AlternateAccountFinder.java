package com.dansplugins.detectionsystem;

import static com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD;
import static java.util.logging.Level.SEVERE;

import com.dansplugins.detectionsystem.commands.AafCommand;
import com.dansplugins.detectionsystem.encryption.IpEncryption;
import com.dansplugins.detectionsystem.encryption.StoredAddressClassifier;
import com.dansplugins.detectionsystem.listeners.PlayerJoinListener;
import com.dansplugins.detectionsystem.logins.LoginRepository;
import com.dansplugins.detectionsystem.logins.LoginService;
import com.dansplugins.detectionsystem.notifications.MailboxesNotificationService;
import com.dansplugins.detectionsystem.notifications.MessageNotificationService;
import com.dansplugins.detectionsystem.notifications.NotificationService;
import com.dansplugins.detectionsystem.notifications.RpkNotificationService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bstats.bukkit.Metrics;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class AlternateAccountFinder extends JavaPlugin implements Listener {

    private DataSource dataSource;
    private LoginService loginService;
    private NotificationService notificationService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Ensure database drivers are loaded
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException exception) {
            getLogger().log(SEVERE, "Failed to load H2 driver", exception);
        }
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            getLogger().log(SEVERE, "Failed to load MariaDB driver", exception);
        }

        // Connection pool
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(getConfig().getString("database.url"));
        String databaseUsername = getConfig().getString("database.username");
        if (databaseUsername != null) {
            hikariConfig.setUsername(databaseUsername);
        }
        String databasePassword = getConfig().getString("database.password");
        if (databasePassword != null) {
            hikariConfig.setPassword(databasePassword);
        }
        dataSource = new HikariDataSource(hikariConfig);

        // Migrations
        Flyway flyway = Flyway.configure(getClassLoader())
                .dataSource(dataSource)
                .locations("classpath:com/dansplugins/detectionsystem/db/migration")
                .table("aaf_schema_history")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(false)
                .load();
        flyway.migrate();

        // jOOQ
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("org.jooq.no-tips", "true");
        SQLDialect dialect = SQLDialect.valueOf(getConfig().getString("database.dialect"));
        Settings jooqSettings = new Settings().withRenderSchema(false);
        DSLContext dsl = DSL.using(
                dataSource,
                dialect,
                jooqSettings
        );

        // Encryption
        IpEncryption ipEncryption = new IpEncryption(getLogger(), getDataFolder());
        
        // Migrate existing plaintext IP addresses to encrypted format
        migrateExistingIpAddresses(dsl, ipEncryption);

        // Repositories
        LoginRepository loginRepository = new LoginRepository(dsl, ipEncryption);

        // Services
        loginService = new LoginService(loginRepository);

        if (getServer().getPluginManager().getPlugin("Mailboxes") != null) {
            notificationService = new MailboxesNotificationService(this);
        } else if (getServer().getPluginManager().getPlugin("rpk-notification-lib-bukkit") != null) {
            notificationService = new RpkNotificationService(this);
        } else {
            notificationService = new MessageNotificationService(this);
        }

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // Commands
        getCommand("aaf").setExecutor(new AafCommand(this));

        // Metrics
        int pluginId = 9834;
        new Metrics(this, pluginId);
    }

    public LoginService getLoginService() {
        return loginService;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }
    
    /**
     * Marker file written next to the encryption key once every plaintext IP in the database has
     * been migrated to ciphertext. Its presence lets subsequent startups skip the (otherwise
     * O(total-login-records)) scan + per-record decrypt probe that {@link #migrateExistingIpAddresses}
     * has to do to find unmigrated rows.
     */
    private static final String MIGRATION_MARKER_FILENAME = "ip-migration-v2.complete";

    /**
     * Migrates existing plaintext IP addresses to encrypted format.
     *
     * Each stored value is classified by {@link StoredAddressClassifier}: a value that decrypts
     * with the current key is already encrypted, and a value that does not decrypt is only
     * encrypted if it also parses as an IP literal. A value that is neither — most commonly
     * ciphertext written under a key that is no longer present — is left untouched and reported,
     * because encrypting it would double-encrypt the row.
     *
     * All migration operations run inside a single database transaction so the table is left
     * either fully migrated or unchanged. On full success a marker file is written to the plugin
     * data folder so subsequent startups skip the scan instead of decrypt-probing every row.
     */
    private void migrateExistingIpAddresses(DSLContext dsl, IpEncryption ipEncryption) {
        File markerFile = new File(getDataFolder(), MIGRATION_MARKER_FILENAME);
        if (markerFile.exists()) {
            return;
        }

        try {
            getLogger().info("Checking for plaintext IP addresses that need encryption...");

            // Use a transaction to ensure atomicity of the migration
            int[] unmigratedCount = {0};
            StoredAddressClassifier classifier = new StoredAddressClassifier(ipEncryption);
            dsl.transaction(configuration -> {
                DSLContext txDsl = DSL.using(configuration);

                var records = txDsl.selectFrom(AAF_LOGIN_RECORD).fetch();

                int totalRecords = records.size();
                int alreadyEncrypted = 0;
                int migrated = 0;
                // Only UUIDs are collected here: the addresses themselves must never reach the log.
                List<String> failedRecords = new ArrayList<>();
                List<String> unrecognizedRecords = new ArrayList<>();

                getLogger().info("Processing " + totalRecords + " login records...");

                for (var record : records) {
                    String currentAddress = record.getAddress();

                    StoredAddressClassifier.Classification classification = classifier.classify(currentAddress);

                    if (classification == StoredAddressClassifier.Classification.BLANK) {
                        continue;
                    }

                    if (classification == StoredAddressClassifier.Classification.ENCRYPTED) {
                        alreadyEncrypted++;
                        continue;
                    }

                    if (classification == StoredAddressClassifier.Classification.UNRECOGNIZED) {
                        unrecognizedRecords.add(String.valueOf(record.getMinecraftUuid()));
                        continue;
                    }

                    try {
                        String encryptedIp = ipEncryption.encrypt(currentAddress);
                        // jOOQ's UpdatableRecord.update() uses the *original* loaded primary-key
                        // values for the WHERE clause, so changing ADDRESS (part of the PK) here
                        // still targets the correct row.
                        record.setAddress(encryptedIp);
                        record.update();
                        migrated++;
                    } catch (Exception e) {
                        failedRecords.add(String.valueOf(record.getMinecraftUuid()));
                        getLogger().warning("Failed to encrypt IP for record " + record.getMinecraftUuid() + ": " + e.getMessage());
                    }
                }

                unmigratedCount[0] = failedRecords.size() + unrecognizedRecords.size();

                getLogger().info("IP address migration completed:");
                getLogger().info("  Total records: " + totalRecords);
                getLogger().info("  Already encrypted: " + alreadyEncrypted);
                getLogger().info("  Newly encrypted: " + migrated);
                getLogger().info("  Unrecognized (left unchanged): " + unrecognizedRecords.size());
                getLogger().info("  Failed: " + failedRecords.size());

                if (!unrecognizedRecords.isEmpty()) {
                    getLogger().warning(unrecognizedRecords.size() + " stored address(es) could not be read with the "
                            + "current encryption key and are not plaintext IP addresses either.");
                    getLogger().warning("This usually means the IP encryption key file is missing or has been "
                            + "replaced, so data encrypted with the previous key can no longer be decrypted.");
                    getLogger().warning("These records were left unchanged - encrypting them again would corrupt them "
                            + "beyond recovery. Restore the original key file from backup and restart.");
                    logAffectedRecords(unrecognizedRecords);
                }

                if (!failedRecords.isEmpty()) {
                    getLogger().warning("The following records failed to encrypt and may need manual intervention:");
                    logAffectedRecords(failedRecords);
                }
            });

            if (unmigratedCount[0] == 0) {
                writeMigrationMarker(markerFile);
            } else {
                getLogger().warning("Migration completed with " + unmigratedCount[0]
                        + " unmigrated record(s); the marker file will not be written so the migration "
                        + "will retry on next startup.");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to migrate existing IP addresses: " + e.getMessage());
            getLogger().severe("The plugin will continue to run, but historical data may not be accessible.");
            // Don't fail startup - the plugin can still function with new data
        }
    }

    /**
     * Logs the accounts owning records the migration could not handle. Only the UUID is logged -
     * the stored address is never written to the log, since on the unmigrated paths it is either a
     * player's plaintext IP or unreadable ciphertext.
     */
    private void logAffectedRecords(List<String> minecraftUuids) {
        for (String minecraftUuid : minecraftUuids) {
            getLogger().warning("  - " + minecraftUuid);
        }
    }

    private void writeMigrationMarker(File markerFile) {
        try {
            Files.createDirectories(markerFile.getParentFile().toPath());
            Files.writeString(markerFile.toPath(),
                    "IP encryption migration completed. Delete this file to force a re-scan on the next startup.\n");
        } catch (IOException e) {
            getLogger().warning("Could not write migration marker file " + markerFile.getAbsolutePath()
                    + "; migration scan will run again next startup. Reason: " + e.getMessage());
        }
    }
}
