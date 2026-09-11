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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRepositoryTest {

    @TempDir
    File dataFolder;

    private LoginRepository repository;
    private IpEncryption ipEncryption;
    private Connection connection;
    private DSLContext dsl;

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
        dsl = DSL.using(connection, SQLDialect.H2);

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

    /**
     * Writes a login record with a chosen {@code last_login}. {@link LoginRepository#saveLogin}
     * always stamps the current instant, and several calls in a row land close enough together to
     * tie, which would leave the ordering assertions below decided by the tiebreak rather than by
     * the timestamp they mean to exercise. The insert mirrors {@code saveLogin} otherwise, address
     * encryption included.
     */
    private void saveLoginAt(UUID minecraftUuid, InetAddress ip, LocalDateTime lastLogin) {
        dsl.insertInto(AAF_LOGIN_RECORD)
                .set(AAF_LOGIN_RECORD.MINECRAFT_UUID, minecraftUuid.toString())
                .set(AAF_LOGIN_RECORD.ADDRESS, ipEncryption.encrypt(ip.getHostAddress()))
                .set(AAF_LOGIN_RECORD.LOGINS, 1)
                .set(AAF_LOGIN_RECORD.FIRST_LOGIN, lastLogin)
                .set(AAF_LOGIN_RECORD.LAST_LOGIN, lastLogin)
                .execute();
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
    void accountsForAnAddressAreListedMostRecentlySeenFirst() throws UnknownHostException {
        // The UUIDs are fixed rather than random so that the order the previous implementation
        // produced — HashMap iteration order, which depends only on the UUIDs — is reproducible,
        // and so this test reliably fails without the ordering (see issue #104).
        UUID first = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
        UUID second = UUID.fromString("00000000-0000-4000-8000-0000000000a2");
        UUID third = UUID.fromString("00000000-0000-4000-8000-0000000000a3");
        InetAddress ip = address("192.0.2.10");

        saveLoginAt(first, ip, LocalDateTime.of(2026, 1, 1, 0, 0));
        saveLoginAt(second, ip, LocalDateTime.of(2026, 3, 1, 0, 0));
        saveLoginAt(third, ip, LocalDateTime.of(2026, 2, 1, 0, 0));

        assertEquals(List.of(second, third, first), repository.getAddressInfo(ip).getAccounts());
    }

    @Test
    void accountsSeenAtTheSameMomentAreOrderedByUuid() throws UnknownHostException {
        // Fixed UUIDs again, and this pair specifically: the previous implementation iterated a
        // HashMap, and these two hash into buckets that put the higher UUID first, so the
        // assertion below is not one the old code could satisfy by luck.
        UUID lower = UUID.fromString("00000000-0000-4000-8000-00000000b001");
        UUID higher = UUID.fromString("00000000-0000-4000-8000-00000000b010");
        InetAddress ip = address("192.0.2.11");
        LocalDateTime sameMoment = LocalDateTime.of(2026, 5, 1, 12, 0);

        saveLoginAt(higher, ip, sameMoment);
        saveLoginAt(lower, ip, sameMoment);

        // Without the tiebreak the two rows could come back either way round, so the assertion is
        // that a tie resolves to the ascending UUID rather than to whatever the database returns.
        assertEquals(List.of(lower, higher), repository.getAddressInfo(ip).getAccounts());
    }

    @Test
    void potentialAltsAreListedMostRecentlySeenFirst() throws UnknownHostException {
        UUID owner = UUID.fromString("00000000-0000-4000-8000-0000000000c0");
        UUID staleAlt = UUID.fromString("00000000-0000-4000-8000-0000000000c1");
        UUID recentAlt = UUID.fromString("00000000-0000-4000-8000-0000000000c2");
        InetAddress sharedIp = address("192.0.2.12");

        saveLoginAt(owner, sharedIp, LocalDateTime.of(2026, 1, 1, 0, 0));
        saveLoginAt(staleAlt, sharedIp, LocalDateTime.of(2026, 1, 2, 0, 0));
        saveLoginAt(recentAlt, sharedIp, LocalDateTime.of(2026, 6, 2, 0, 0));

        assertEquals(List.of(recentAlt, staleAlt), repository.getPotentialAlts(owner));
    }

    @Test
    void potentialAltIsOrderedByItsNewestSharedLogin() throws UnknownHostException {
        // An alt sharing several addresses collapses to one entry, and the timestamp that decides
        // where that entry sits is the newest of the rows that matched — not an arbitrary one.
        // The alt that must come first is given the higher UUID and is written last, so neither
        // insertion order nor UUID order produces the expected result by accident.
        UUID owner = UUID.fromString("00000000-0000-4000-8000-0000000000d0");
        UUID altWithOneMiddlingRow = UUID.fromString("00000000-0000-4000-8000-0000000000d1");
        UUID altWithOldAndNewRows = UUID.fromString("00000000-0000-4000-8000-0000000000d2");
        InetAddress first = address("192.0.2.13");
        InetAddress second = address("192.0.2.14");

        saveLoginAt(owner, first, LocalDateTime.of(2026, 1, 1, 0, 0));
        saveLoginAt(owner, second, LocalDateTime.of(2026, 1, 1, 0, 0));
        saveLoginAt(altWithOneMiddlingRow, first, LocalDateTime.of(2026, 4, 1, 0, 0));
        saveLoginAt(altWithOldAndNewRows, first, LocalDateTime.of(2025, 1, 1, 0, 0));
        saveLoginAt(altWithOldAndNewRows, second, LocalDateTime.of(2026, 9, 1, 0, 0));

        assertEquals(List.of(altWithOldAndNewRows, altWithOneMiddlingRow),
                repository.getPotentialAlts(owner));
    }

    @Test
    void storedAddressesAreEncryptedRatherThanPlaintext() throws UnknownHostException {
        // Regression guard for the encryption invariant this repository depends on:
        // lookups must go through IpEncryption rather than storing/matching plaintext.
        // The stored column is read back directly, since nothing in the repository
        // decrypts an address any more (see issue #110).
        UUID player = UUID.randomUUID();
        InetAddress ip = address("198.51.100.7");

        repository.saveLogin(player, ip);

        String expectedCiphertext = ipEncryption.encrypt(ip.getHostAddress());
        assertEquals(expectedCiphertext, ipEncryption.encrypt(ip.getHostAddress()),
                "encryption must be deterministic for the assertion below to be meaningful");

        List<String> storedAddresses = dsl.select(AAF_LOGIN_RECORD.ADDRESS)
                .from(AAF_LOGIN_RECORD)
                .where(AAF_LOGIN_RECORD.MINECRAFT_UUID.eq(player.toString()))
                .fetch(AAF_LOGIN_RECORD.ADDRESS);
        assertEquals(List.of(expectedCiphertext), storedAddresses,
                "repository must store the ciphertext of the address, never the plaintext");
        assertEquals(ip.getHostAddress(), ipEncryption.decrypt(expectedCiphertext),
                "the stored ciphertext must still decrypt to the original IP");
    }
}
