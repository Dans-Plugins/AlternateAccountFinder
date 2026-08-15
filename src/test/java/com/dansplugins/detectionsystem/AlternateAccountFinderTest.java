package com.dansplugins.detectionsystem;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the connection-pool shutdown helper (issue #97) and the dialect parser (issue #101).
 *
 * <p>{@link AlternateAccountFinder} extends {@code JavaPlugin} and cannot be constructed outside a
 * running server, so neither {@code onDisable} nor {@code onEnable} is covered here — only the
 * static helpers they delegate to. That those helpers are actually wired in, and that a rejected
 * dialect disables the plugin rather than throwing, stay manual checks on a live server.
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
