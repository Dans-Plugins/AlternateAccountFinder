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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginServiceTest {

    @TempDir
    File dataFolder;

    private LoginService service;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        // Same real-H2, no-mocks setup as LoginRepositoryTest: LoginService is a thin
        // pass-through, so it is only meaningfully tested against a real repository.
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

        IpEncryption ipEncryption = new IpEncryption(Logger.getLogger(LoginServiceTest.class.getName()), dataFolder);
        LoginRepository repository = new LoginRepository(dsl, ipEncryption);
        service = new LoginService(repository);
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    private InetAddress address(String literal) throws UnknownHostException {
        return InetAddress.getByName(literal);
    }

    @Test
    void saveLoginIsReflectedInAddressInfo() throws UnknownHostException {
        UUID player = UUID.randomUUID();
        InetAddress ip = address("192.168.10.1");

        service.saveLogin(player, ip);

        AddressAccountInfo info = service.getAddressInfo(ip);
        assertEquals(List.of(player), info.getAccounts());
        assertEquals(1, info.getAccountInfo(player).getLogins());
    }

    @Test
    void getLoginCountIncrementsAcrossRepeatedLogins() throws UnknownHostException {
        UUID player = UUID.randomUUID();
        InetAddress ip = address("10.1.0.5");

        service.saveLogin(player, ip);
        service.saveLogin(player, ip);

        assertEquals(2, service.getLoginCount(player, ip));
    }

    @Test
    void getLoginCountIsZeroWhenNoRecordExists() throws UnknownHostException {
        assertEquals(0, service.getLoginCount(UUID.randomUUID(), address("10.1.0.9")));
    }

    @Test
    void getAccountInfoReturnsAddressesUsedByPlayer() throws UnknownHostException {
        UUID player = UUID.randomUUID();
        InetAddress ip = address("172.20.0.1");

        service.saveLogin(player, ip);

        AccountAddressInfo info = service.getAccountInfo(player);
        assertEquals(List.of(ip), info.getAddresses());
    }

    @Test
    void getPotentialAltsReturnsAccountsSharingAnAddress() throws UnknownHostException {
        UUID owner = UUID.randomUUID();
        UUID alt = UUID.randomUUID();
        UUID unrelated = UUID.randomUUID();
        InetAddress sharedIp = address("192.168.50.100");
        InetAddress otherIp = address("192.168.50.200");

        service.saveLogin(owner, sharedIp);
        service.saveLogin(alt, sharedIp);
        service.saveLogin(unrelated, otherIp);

        assertEquals(List.of(alt), service.getPotentialAlts(owner));
    }

    @Test
    void getPotentialAltsIsEmptyWhenNoAddressIsShared() throws UnknownHostException {
        UUID player = UUID.randomUUID();
        service.saveLogin(player, address("203.0.113.9"));

        assertTrue(service.getPotentialAlts(player).isEmpty());
    }
}
