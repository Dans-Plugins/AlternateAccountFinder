package com.dansplugins.detectionsystem;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the connection-pool shutdown helper (issue #97).
 *
 * <p>{@link AlternateAccountFinder} extends {@code JavaPlugin} and cannot be constructed outside a
 * running server, so {@code onDisable} itself is not covered here — only the static helper it
 * delegates to. That the helper is actually wired into {@code onDisable} stays a manual check on a
 * live server.
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
