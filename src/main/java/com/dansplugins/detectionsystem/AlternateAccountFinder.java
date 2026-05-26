package com.dansplugins.detectionsystem;

import static com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD;
import static java.util.logging.Level.SEVERE;

import com.dansplugins.detectionsystem.commands.AafCommand;
import com.dansplugins.detectionsystem.encryption.IpEncryption;
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
     * Detection of plaintext vs encrypted addresses works by attempting to decrypt each stored
     * value. If decryption succeeds, the value is assumed to already be encrypted. If decryption
     * throws, the value is treated as plaintext and will be encrypted.
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
            int[] failedCount = {0};
            dsl.transaction(configuration -> {
                DSLContext txDsl = DSL.using(configuration);

                var records = txDsl.selectFrom(AAF_LOGIN_RECORD).fetch();

                int totalRecords = records.size();
                int alreadyEncrypted = 0;
                int migrated = 0;
                List<String> failedRecords = new ArrayList<>();

                getLogger().info("Processing " + totalRecords + " login records...");

                for (var record : records) {
                    String currentAddress = record.getAddress();

                    if (currentAddress == null || currentAddress.trim().isEmpty()) {
                        continue;
                    }

                    if (ipEncryption.isEncrypted(currentAddress)) {
                        alreadyEncrypted++;
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
                        failedRecords.add(record.getMinecraftUuid() + ":" + currentAddress);
                        getLogger().warning("Failed to encrypt IP for record " + record.getMinecraftUuid() + ": " + e.getMessage());
                    }
                }

                failedCount[0] = failedRecords.size();

                getLogger().info("IP address migration completed:");
                getLogger().info("  Total records: " + totalRecords);
                getLogger().info("  Already encrypted: " + alreadyEncrypted);
                getLogger().info("  Newly encrypted: " + migrated);
                getLogger().info("  Failed: " + failedRecords.size());

                if (!failedRecords.isEmpty()) {
                    getLogger().warning("The following records failed to encrypt:");
                    for (String failedRecord : failedRecords) {
                        getLogger().warning("  - " + failedRecord);
                    }
                    getLogger().warning("These records may need manual intervention.");
                }
            });

            if (failedCount[0] == 0) {
                writeMigrationMarker(markerFile);
            } else {
                getLogger().warning("Migration completed with " + failedCount[0]
                        + " failed record(s); the marker file will not be written so the migration "
                        + "will retry on next startup.");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to migrate existing IP addresses: " + e.getMessage());
            getLogger().severe("The plugin will continue to run, but historical data may not be accessible.");
            // Don't fail startup - the plugin can still function with new data
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
