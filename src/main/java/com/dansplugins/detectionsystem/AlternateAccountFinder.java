package com.dansplugins.detectionsystem;

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
     * Migrates existing plaintext IP addresses to encrypted format.
     * This is a one-time operation that runs on startup after the encryption system is initialized.
     * 
     * The detection of plaintext vs encrypted addresses is based on attempting to decrypt each
     * stored value. If decryption succeeds, the value is assumed to be already encrypted. If
     * decryption throws an exception, the value is treated as plaintext and will be encrypted.
     * 
     * All migration operations are performed within a single database transaction to ensure atomicity.
     */
    private void migrateExistingIpAddresses(DSLContext dsl, IpEncryption ipEncryption) {
        try {
            getLogger().info("Checking for plaintext IP addresses that need encryption...");
            
            // Use a transaction to ensure atomicity of the migration
            dsl.transaction(configuration -> {
                DSLContext txDsl = DSL.using(configuration);
                
                // Fetch all login records
                var records = txDsl.selectFrom(com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD)
                        .fetch();
                
                int totalRecords = records.size();
                int alreadyEncrypted = 0;
                int migrated = 0;
                int failed = 0;
                java.util.List<String> failedRecords = new java.util.ArrayList<>();
                
                getLogger().info("Processing " + totalRecords + " login records...");
                
                for (var record : records) {
                    String currentAddress = record.getAddress();
                    
                    // Skip records with no stored address
                    if (currentAddress == null || currentAddress.trim().isEmpty()) {
                        continue;
                    }
                    
                    // Check if this address is already encrypted
                    if (ipEncryption.isEncrypted(currentAddress)) {
                        alreadyEncrypted++;
                        continue;
                    }
                    
                    // This appears to be plaintext - encrypt it
                    try {
                        String encryptedIp = ipEncryption.encrypt(currentAddress);
                        
                        // Update the record with encrypted IP using the record's update method
                        record.setAddress(encryptedIp);
                        record.update();
                        
                        migrated++;
                    } catch (Exception e) {
                        failed++;
                        String recordId = record.getMinecraftUuid() + ":" + currentAddress;
                        failedRecords.add(recordId);
                        getLogger().warning("Failed to encrypt IP for record " + record.getMinecraftUuid() + ": " + e.getMessage());
                    }
                }
                
                // Log summary
                getLogger().info("IP address migration completed:");
                getLogger().info("  Total records: " + totalRecords);
                getLogger().info("  Already encrypted: " + alreadyEncrypted);
                getLogger().info("  Newly encrypted: " + migrated);
                getLogger().info("  Failed: " + failed);
                
                if (failed > 0) {
                    getLogger().warning("The following records failed to encrypt:");
                    for (String failedRecord : failedRecords) {
                        getLogger().warning("  - " + failedRecord);
                    }
                    getLogger().warning("These records may need manual intervention.");
                }
            });
            
        } catch (Exception e) {
            getLogger().severe("Failed to migrate existing IP addresses: " + e.getMessage());
            getLogger().severe("The plugin will continue to run, but historical data may not be accessible.");
            // Don't fail startup - the plugin can still function with new data
        }
    }
}
