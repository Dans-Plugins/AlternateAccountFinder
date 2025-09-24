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
        IpEncryption ipEncryption = new IpEncryption(getLogger());
        
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
     */
    private void migrateExistingIpAddresses(DSLContext dsl, IpEncryption ipEncryption) {
        try {
            // Check if migration is needed by looking for IP addresses that don't look like Base64
            // Base64 strings will be much longer and contain only valid Base64 characters
            int plaintextCount = dsl.selectCount()
                    .from(com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD)
                    .where(com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD.ADDRESS.notLike("%=%"))
                    .and(com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD.ADDRESS.length().lessThan(50))
                    .fetchOne(0, int.class);
                    
            if (plaintextCount > 0) {
                getLogger().info("Found " + plaintextCount + " plaintext IP addresses to encrypt");
                
                // Fetch all records with plaintext IPs
                var records = dsl.selectFrom(com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD)
                        .where(com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD.ADDRESS.notLike("%=%"))
                        .and(com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD.ADDRESS.length().lessThan(50))
                        .fetch();
                
                int migrated = 0;
                for (var record : records) {
                    try {
                        String plaintextIp = record.getAddress();
                        String encryptedIp = ipEncryption.encrypt(plaintextIp);
                        
                        // Update the record with encrypted IP
                        dsl.update(com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD)
                                .set(com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD.ADDRESS, encryptedIp)
                                .where(com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD.MINECRAFT_UUID.eq(record.getMinecraftUuid()))
                                .and(com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD.ADDRESS.eq(plaintextIp))
                                .execute();
                        
                        migrated++;
                    } catch (Exception e) {
                        getLogger().warning("Failed to encrypt IP for record: " + e.getMessage());
                    }
                }
                
                getLogger().info("Successfully migrated " + migrated + " IP addresses to encrypted format");
            } else {
                getLogger().info("No plaintext IP addresses found - migration not needed");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to migrate existing IP addresses: " + e.getMessage());
            // Don't fail startup - the plugin can still function with new data
        }
    }
}
