package com.dansplugins.detectionsystem;

import com.dansplugins.detectionsystem.encryption.IpEncryption;
import com.dansplugins.detectionsystem.trace.TraceClient;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the connection-pool shutdown helper (issue #97), the dialect parser (issue #101), the
 * startup-failure message (issue #111) and the usage-reporting client builder.
 *
 * <p>{@link AlternateAccountFinder} extends {@code JavaPlugin} and cannot be constructed outside a
 * running server, so neither {@code onDisable} nor {@code onEnable} is covered here — only the
 * static helpers they delegate to. That those helpers are actually wired in, and that a rejected
 * dialect, an unreachable database, a failed migration or a corrupted key file disables the plugin
 * rather than throwing, stay manual checks on a live server.
 */
class AlternateAccountFinderTest {

    @Test
    void closesAPoolThatIsCloseable() {
        HikariConfig config = new HikariConfig();
        // An in-memory database keeps the pool real — a genuine HikariDataSource with a genuine
        // connection behind it — without leaving anything on disk.
        config.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MYSQL;DATABASE_TO_UPPER=false");
        config.setUsername("sa");
        config.setPassword("");
        HikariDataSource dataSource = new HikariDataSource(config);

        AlternateAccountFinder.closeDataSource(dataSource, recordingLogger(new ArrayList<>()));

        assertTrue(dataSource.isClosed());
    }

    @Test
    void doesNothingWhenNoPoolWasEverBuilt() {
        // onEnable assigns the field after building the pool, so a startup that failed earlier
        // than that leaves it null when onDisable runs.
        List<LogRecord> logged = new ArrayList<>();

        assertDoesNotThrow(() -> AlternateAccountFinder.closeDataSource(null, recordingLogger(logged)));

        assertEquals(List.of(), logged);
    }

    @Test
    void leavesADataSourceThatIsNotCloseableAlone() {
        List<String> invoked = new ArrayList<>();
        DataSource dataSource = (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(),
                new Class<?>[]{DataSource.class},
                (proxy, method, args) -> {
                    invoked.add(method.getName());
                    return null;
                });
        List<LogRecord> logged = new ArrayList<>();

        assertDoesNotThrow(() -> AlternateAccountFinder.closeDataSource(dataSource, recordingLogger(logged)));

        assertEquals(List.of(), invoked);
        assertEquals(List.of(), logged);
    }

    @Test
    void warnsRatherThanThrowsWhenTheCloseFails() {
        // An exception out of onDisable is reported by Bukkit as a plugin fault, so a pool that
        // refuses to shut down has to end as a log line and nothing more.
        DataSource dataSource = (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(),
                new Class<?>[]{DataSource.class, AutoCloseable.class},
                (InvocationHandler) (proxy, method, args) -> {
                    if (method.getName().equals("close")) {
                        throw new SQLException("pool is stuck");
                    }
                    return null;
                });
        List<LogRecord> logged = new ArrayList<>();

        assertDoesNotThrow(() -> AlternateAccountFinder.closeDataSource(dataSource, recordingLogger(logged)));

        assertEquals(1, logged.size());
        assertEquals(Level.WARNING, logged.get(0).getLevel());
        assertEquals("pool is stuck", logged.get(0).getThrown().getMessage());
    }

    @Test
    void theStartupFailureMessageNamesTheStepTheCauseAndTheRemedy() {
        String message = AlternateAccountFinder.startupFailureMessage(
                AlternateAccountFinder.StartupStep.CONNECTION_POOL,
                new RuntimeException("Failed to initialize pool: Connection refused"));

        assertTrue(message.startsWith("Failed while opening the database connection pool: "), message);
        assertTrue(message.contains("Failed to initialize pool: Connection refused"), message);
        assertTrue(message.contains("database.url"), message);
        assertTrue(message.contains("config.yml"), message);
        assertTrue(message.endsWith("The plugin has disabled itself."), message);
    }

    @Test
    void theStartupFailureMessageSpellsOutAMessageLessCause() {
        // Hikari's and Flyway's wrappers do not always carry a message; "null" in the console
        // would send an operator looking for a bug that is not there.
        String message = AlternateAccountFinder.startupFailureMessage(
                AlternateAccountFinder.StartupStep.MIGRATIONS, new IllegalStateException());

        assertTrue(message.startsWith("Failed while applying the database migrations: IllegalStateException "), message);
        assertFalse(message.contains("null"), message);
    }

    @Test
    void theCorruptedKeyFailureKeepsTheRestoreFromBackupGuidance(@TempDir Path dataFolder) throws IOException {
        // The exception is the real one: a key file that is not 32 bytes is what IpEncryption
        // rejects, and USER_GUIDE.md promises that the plugin fails to enable and that the file
        // should be restored rather than deleted. That guidance now has to survive into the one
        // line an operator reads before the plugin disables itself.
        Files.write(dataFolder.resolve("ip-encryption.key"), new byte[31]);
        RuntimeException cause = assertThrows(RuntimeException.class,
                () -> new IpEncryption(recordingLogger(new ArrayList<>()), dataFolder.toFile()));

        String message = AlternateAccountFinder.startupFailureMessage(
                AlternateAccountFinder.StartupStep.ENCRYPTION_KEY, cause);

        assertTrue(message.startsWith("Failed while loading the IP encryption key: Corrupted encryption key file."), message);
        assertTrue(message.contains("ip-encryption.key"), message);
        assertTrue(message.contains("restore it from a backup rather than deleting it"), message);
    }

    @Test
    void theKeyFailureGuidanceAlsoFitsAFreshInstallThatCannotWriteTheKey(@TempDir Path dataFolder) throws IOException {
        // The same catch covers a first startup whose data folder cannot be written, where there
        // is no key file, no backup and nothing stored; the line must not read as if there were.
        // A regular file where the data folder should be makes createDirectories fail.
        Path notAFolder = dataFolder.resolve("AlternateAccountFinder");
        Files.writeString(notAFolder, "");
        RuntimeException cause = assertThrows(RuntimeException.class,
                () -> new IpEncryption(recordingLogger(new ArrayList<>()), notAFolder.toFile()));

        String message = AlternateAccountFinder.startupFailureMessage(
                AlternateAccountFinder.StartupStep.ENCRYPTION_KEY, cause);

        assertTrue(message.startsWith("Failed while loading the IP encryption key: Key generation failed"), message);
        assertTrue(message.contains("If it does not exist yet, check that the data folder can be read and written."), message);
    }

    @Test
    void parsesTheDialectsTheDefaultConfigDocuments() {
        assertEquals(SQLDialect.H2, AlternateAccountFinder.parseDialect("H2"));
        assertEquals(SQLDialect.MARIADB, AlternateAccountFinder.parseDialect("MARIADB"));
    }

    @Test
    void parsesADialectWhateverCaseItIsWrittenIn() {
        // SQLDialect's constants are uppercase, so the spellings an operator reaches for first
        // used to be rejected outright.
        assertEquals(SQLDialect.MARIADB, AlternateAccountFinder.parseDialect("mariadb"));
        assertEquals(SQLDialect.MARIADB, AlternateAccountFinder.parseDialect("MariaDB"));
        assertEquals(SQLDialect.H2, AlternateAccountFinder.parseDialect("h2"));
    }

    @Test
    void parsesADialectSurroundedByWhitespace() {
        assertEquals(SQLDialect.H2, AlternateAccountFinder.parseDialect("  H2 "));
    }

    @Test
    void stillAcceptsADialectThisPluginShipsNoDriverFor() {
        // Narrowing the accepted set to H2 and MariaDB would break an operator running MySQL
        // through the MariaDB driver, so anything jOOQ recognises is let through.
        assertEquals(SQLDialect.MYSQL, AlternateAccountFinder.parseDialect("MYSQL"));
    }

    @Test
    void rejectsAMissingDialectWithAMessageNamingTheConfigKey() {
        // A key left out of the operator's config.yml resolves to the jar's bundled H2 default
        // rather than to null, so this branch is defensive: it covers the case where the bundled
        // config is what has lost the key. Enum.valueOf answered null with "Name is null".
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AlternateAccountFinder.parseDialect(null));

        assertTrue(exception.getMessage().contains("database.dialect"), exception.getMessage());
        assertTrue(exception.getMessage().contains("config.yml"), exception.getMessage());
        assertTrue(exception.getMessage().contains("H2"), exception.getMessage());
        assertTrue(exception.getMessage().contains("MARIADB"), exception.getMessage());
    }

    @Test
    void rejectsABlankDialectTheSameWayAsAMissingOne() {
        assertEquals(
                assertThrows(IllegalArgumentException.class, () -> AlternateAccountFinder.parseDialect(null))
                        .getMessage(),
                assertThrows(IllegalArgumentException.class, () -> AlternateAccountFinder.parseDialect("   "))
                        .getMessage());
    }

    @Test
    void rejectsAnUnknownDialectWithAMessageQuotingTheOffendingValue() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AlternateAccountFinder.parseDialect("postgres-ish"));

        assertTrue(exception.getMessage().contains("database.dialect"), exception.getMessage());
        assertTrue(exception.getMessage().contains("\"postgres-ish\""), exception.getMessage());
        assertTrue(exception.getMessage().contains("MARIADB"), exception.getMessage());
    }

    @Test
    void readsTheBundledUsageReportingBlockWhenTheOperatorsConfigPredatesIt() {
        // saveDefaultConfig() never rewrites an existing config.yml, so a server upgraded from a
        // version before usage reporting has no usage-reporting block on disk. Bukkit registers
        // the jar's config.yml as that file's defaults; this is an operator's file from such a
        // version, with the real bundled config behind it, and reporting must come out enabled.
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.set("database.dialect", "H2");
        onDisk.setDefaults(bundledConfig());

        TraceClient trace = AlternateAccountFinder.buildTraceClient(onDisk, "AlternateAccountFinder", recordingLogger(new ArrayList<>()), null);

        assertTrue(trace.isEnabled());
        trace.close();
    }

    @Test
    void theBundledDefaultsAreOnlyReachedByTheOneArgumentGetters() {
        // The reason the builder uses the one-argument getters: the two-argument ones answer with
        // their explicit fallback rather than the bundled default, which for the key would have
        // turned reporting off on every upgraded installation. Pinned against YamlConfiguration
        // itself so the choice is not undone as a tidy-up.
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.setDefaults(bundledConfig());

        assertNotNull(onDisk.getString("usage-reporting.key"));
        assertFalse(onDisk.getString("usage-reporting.key").isBlank());
        assertTrue(onDisk.getBoolean("usage-reporting.enabled"));
        assertEquals("", onDisk.getString("usage-reporting.key", ""));
        assertFalse(onDisk.getBoolean("usage-reporting.enabled", false));
    }

    @Test
    void turnsReportingOffWhenTheOperatorDisablesIt() {
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.set("usage-reporting.enabled", false);
        onDisk.setDefaults(bundledConfig());

        TraceClient trace = AlternateAccountFinder.buildTraceClient(onDisk, "AlternateAccountFinder", recordingLogger(new ArrayList<>()), null);

        assertFalse(trace.isEnabled());
    }

    @Test
    void turnsReportingOffWhenTheOperatorBlanksTheKey() {
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.set("usage-reporting.key", "");
        onDisk.setDefaults(bundledConfig());

        TraceClient trace = AlternateAccountFinder.buildTraceClient(onDisk, "AlternateAccountFinder", recordingLogger(new ArrayList<>()), null);

        assertFalse(trace.isEnabled());
    }

    @Test
    void staysOffWhenNoConfigCarriesTheBlockAtAll() {
        // Only reachable if the bundled config.yml itself has lost the block; the fallbacks are
        // then the author's endpoint and an empty key, and an empty key means off.
        TraceClient trace = AlternateAccountFinder.buildTraceClient(new YamlConfiguration(), "AlternateAccountFinder", recordingLogger(new ArrayList<>()), null);

        assertFalse(trace.isEnabled());
    }

    @Test
    void theServerWideSwitchWinsOverThePluginsOwnConfig(@TempDir Path plugins) throws IOException {
        // plugins/trace/config.yml is shared by every plugin on the server that reports to trace;
        // enabled: false there turns this plugin off even though its own config says on, and the
        // reason names the file so the startup line points the operator at it.
        Path serverWide = plugins.resolve("trace").resolve("config.yml");
        Files.createDirectories(serverWide.getParent());
        Files.writeString(serverWide, "enabled: false\n");
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.setDefaults(bundledConfig());

        TraceClient trace = AlternateAccountFinder.buildTraceClient(onDisk, "AlternateAccountFinder", recordingLogger(new ArrayList<>()), plugins.toFile());

        assertFalse(trace.isEnabled());
        assertEquals(TraceClient.REASON_SERVER_WIDE, trace.disabledReason());
    }

    @Test
    void createsTheServerWideSwitchWhenItIsMissing(@TempDir Path plugins) {
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.setDefaults(bundledConfig());

        TraceClient trace = AlternateAccountFinder.buildTraceClient(onDisk, "AlternateAccountFinder", recordingLogger(new ArrayList<>()), plugins.toFile());

        assertTrue(Files.exists(plugins.resolve("trace").resolve("config.yml")));
        assertTrue(trace.isEnabled(), "a freshly created switch file means enabled");
        trace.close();
    }

    @Test
    void copiesTheBundledUsageReportingBlockOntoAConfigThatPredatesIt() {
        // The operator's file has no usage-reporting block; the bundled defaults are behind it.
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.set("database.dialect", "H2");
        onDisk.setDefaults(bundledConfig());
        assertFalse(onDisk.isSet("usage-reporting"), "the on-disk file must lack the block for this test to mean anything");

        assertTrue(AlternateAccountFinder.copyBundledUsageReportingBlock(onDisk));

        // Now in the file itself (read without falling through to the defaults), with the jar's
        // values, and a second pass has nothing to do.
        assertEquals(bundledConfig().get("usage-reporting.key"), onDisk.get("usage-reporting.key", null));
        assertEquals(bundledConfig().get("usage-reporting.endpoint"), onDisk.get("usage-reporting.endpoint", null));
        assertEquals(Boolean.TRUE, onDisk.get("usage-reporting.enabled", null));
        assertEquals("H2", onDisk.getString("database.dialect"), "everything else is left alone");
        assertFalse(AlternateAccountFinder.copyBundledUsageReportingBlock(onDisk));
    }

    @Test
    void leavesAnOperatorsOwnUsageReportingBlockAlone() {
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.set("usage-reporting.enabled", false);
        onDisk.setDefaults(bundledConfig());

        assertFalse(AlternateAccountFinder.copyBundledUsageReportingBlock(onDisk));

        assertFalse(onDisk.getBoolean("usage-reporting.enabled"));
        assertFalse(onDisk.isSet("usage-reporting.key"), "an operator's block is not completed from the jar");
    }

    @Test
    void theStartupNoticeSaysWhatIsSentAndWhereToTurnItOff() {
        String on = AlternateAccountFinder.usageReportingNotice("AlternateAccountFinder", null);

        assertTrue(on.startsWith("Usage reporting is on: AlternateAccountFinder sends its name, version and command names to https://trace.danielstephenson.dev"), on);
        assertTrue(on.contains("usage-reporting.enabled: false"), on);
        assertTrue(on.contains("plugins/trace/config.yml"), on);
        assertTrue(on.endsWith("Details: https://github.com/Stephenson-Software/trace#usage-reporting"), on);
    }

    @Test
    void theStartupNoticeSaysWhyReportingIsOff() {
        assertEquals("Usage reporting is off (config.yml).",
                AlternateAccountFinder.usageReportingNotice("AlternateAccountFinder", TraceClient.REASON_CONFIG));
        assertEquals("Usage reporting is off (server-wide config: plugins/trace/config.yml).",
                AlternateAccountFinder.usageReportingNotice("AlternateAccountFinder", TraceClient.REASON_SERVER_WIDE));
    }

    /**
     * The {@code config.yml} shipped in the jar, exactly as Bukkit registers it as the defaults
     * for the operator's copy.
     */
    private static YamlConfiguration bundledConfig() {
        InputStream bundled = AlternateAccountFinderTest.class.getResourceAsStream("/config.yml");
        assertNotNull(bundled, "the bundled config.yml is missing from the classpath");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(bundled, StandardCharsets.UTF_8));
    }

    /**
     * A logger that collects what it is given instead of printing it, so a test can assert on the
     * warning without the failure path also cluttering the build output.
     */
    private static Logger recordingLogger(List<LogRecord> logged) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return logger;
    }
}
