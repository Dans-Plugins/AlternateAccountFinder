package com.dansplugins.detectionsystem;

import static java.util.logging.Level.SEVERE;

import com.dansplugins.detectionsystem.commands.AafCommand;
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

        // Repositories
        LoginRepository loginRepository = new LoginRepository(dsl);

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
}
