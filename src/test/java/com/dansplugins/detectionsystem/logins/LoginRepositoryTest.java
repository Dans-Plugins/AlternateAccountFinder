package com.dansplugins.detectionsystem.logins;

import com.dansplugins.detectionsystem.encryption.IpEncryption;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRepositoryTest {

    @TempDir
    File dataFolder;

    private LoginRepository repository;
    private IpEncryption ipEncryption;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        // A fresh, uniquely-named in-memory H2 database per test avoids cross-test
        // state leaking through JVM-shared H2 connections. DB_CLOSE_DELAY=-1 keeps
        // the in-memory database alive across the separate connections Flyway and
        // jOOQ each open against it, rather than being destroyed when one closes.
        String jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("classpath:com/dansplugins/detectionsystem/db/migration")
                .table("aaf_schema_history")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(false)
                .load();
        flyway.migrate();

        connection = DriverManager.getConnection(jdbcUrl, "sa", "");
        DSLContext dsl = DSL.using(connection, SQLDialect.H2);

        ipEncryption = new IpEncryption(Logger.getLogger(LoginRepositoryTest.class.getName()), dataFolder);
        repository = new LoginRepository(dsl, ipEncryption);
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    private InetAddress address(String literal) throws UnknownHostException {
        return InetAddress.getByName(literal);
    }

    @Test
    void savedLoginIsFoundByAddress() throws UnknownHostException {
        UUID player = UUID.randomUUID();
        InetAddress ip = address("192.168.1.1");

        repository.saveLogin(player, ip);

        AddressAccountInfo info = repository.getAddressInfo(ip);
        assertEquals(List.of(player), info.getAccounts());
        assertEquals(1, info.getAccountInfo(player).getLogins());
    }

    @Test
    void repeatedLoginsFromSameAddressIncrementCount() throws UnknownHostException {
        UUID player = UUID.randomUUID();
        InetAddress ip = address("10.0.0.5");

        repository.saveLogin(player, ip);
        repository.saveLogin(player, ip);
        repository.saveLogin(player, ip);

        assertEquals(3, repository.getLoginCount(player, ip));
    }

    @Test
    void loginCountIsZeroWhenNoRecordExists() throws UnknownHostException {
        assertEquals(0, repository.getLoginCount(UUID.randomUUID(), address("10.0.0.9")));
    }

    @Test
    void distinctAddressesForSamePlayerAreTrackedSeparately() throws UnknownHostException {
        UUID player = UUID.randomUUID();
        InetAddress first = address("10.0.0.1");
        InetAddress second = address("10.0.0.2");

        repository.saveLogin(player, first);
        repository.saveLogin(player, second);
        repository.saveLogin(player, second);

        assertEquals(1, repository.getLoginCount(player, first));
        assertEquals(2, repository.getLoginCount(player, second));
    }

    @Test
    void accountInfoDecryptsAddressesBackToOriginalIp() throws UnknownHostException {
        UUID player = UUID.randomUUID();
        InetAddress ip = address("172.16.0.1");
        repository.saveLogin(player, ip);

        AccountAddressInfo info = repository.getAccountInfo(player);

        assertEquals(List.of(ip), info.getAddresses());
        assertEquals(1, info.getAddressInfo(ip).getLogins());
    }

    @Test
    void potentialAltsAreAccountsSharingAnAddress() throws UnknownHostException {
        UUID owner = UUID.randomUUID();
        UUID alt = UUID.randomUUID();
        UUID unrelated = UUID.randomUUID();
        InetAddress sharedIp = address("192.168.0.100");
        InetAddress otherIp = address("192.168.0.200");

        repository.saveLogin(owner, sharedIp);
        repository.saveLogin(alt, sharedIp);
        repository.saveLogin(unrelated, otherIp);

        List<UUID> alts = repository.getPotentialAlts(owner);

        assertEquals(List.of(alt), alts);
    }

    @Test
    void potentialAltSharingSeveralAddressesIsListedOnce() throws UnknownHostException {
        // The query joins the login records against themselves, producing one row per shared
        // address; without a distinct select the same account came back once per address and
        // was printed repeatedly in /aaf alts and in join notifications.
        UUID owner = UUID.randomUUID();
        UUID alt = UUID.randomUUID();
        InetAddress first = address("192.168.5.1");
        InetAddress second = address("192.168.5.2");
        InetAddress third = address("192.168.5.3");

        repository.saveLogin(owner, first);
        repository.saveLogin(owner, second);
        repository.saveLogin(owner, third);
        repository.saveLogin(alt, first);
        repository.saveLogin(alt, second);
        repository.saveLogin(alt, third);

        assertEquals(List.of(alt), repository.getPotentialAlts(owner));
    }

    @Test
    void everyPotentialAltSharingSeveralAddressesIsListedOnce() throws UnknownHostException {
        UUID owner = UUID.randomUUID();
        UUID firstAlt = UUID.randomUUID();
        UUID secondAlt = UUID.randomUUID();
        InetAddress first = address("192.168.6.1");
        InetAddress second = address("192.168.6.2");

        repository.saveLogin(owner, first);
        repository.saveLogin(owner, second);
        repository.saveLogin(firstAlt, first);
        repository.saveLogin(firstAlt, second);
        repository.saveLogin(secondAlt, first);
        repository.saveLogin(secondAlt, second);

        // Distinct must collapse the duplicate rows without dropping a genuinely different
        // account, so both alts are expected exactly once. Sorted because the query does not
        // guarantee an order.
        List<UUID> alts = repository.getPotentialAlts(owner).stream().sorted().toList();
        assertEquals(Stream.of(firstAlt, secondAlt).sorted().toList(), alts);
    }

    @Test
    void playerWithNoSharedAddressHasNoPotentialAlts() throws UnknownHostException {
        UUID player = UUID.randomUUID();
        repository.saveLogin(player, address("203.0.113.5"));

        assertTrue(repository.getPotentialAlts(player).isEmpty());
    }

    @Test
    void storedAddressesAreEncryptedRatherThanPlaintext() throws UnknownHostException {
        // Regression guard for the encryption invariant this repository depends on:
        // lookups must go through IpEncryption rather than storing/matching plaintext.
        UUID player = UUID.randomUUID();
        InetAddress ip = address("198.51.100.7");

        repository.saveLogin(player, ip);

        String expectedCiphertext = ipEncryption.encrypt(ip.getHostAddress());
        assertEquals(expectedCiphertext, ipEncryption.encrypt(ip.getHostAddress()),
                "encryption must be deterministic for the assertion below to be meaningful");

        AccountAddressInfo info = repository.getAccountInfo(player);
        assertEquals(List.of(ip), info.getAddresses(), "repository must decrypt stored addresses back to the original IP");
    }
}
