package com.dansplugins.detectionsystem.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpEncryptionTest {

    @TempDir
    File dataFolder;

    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger(IpEncryptionTest.class.getName());
    }

    @Test
    void roundTripsIpv4() {
        IpEncryption enc = new IpEncryption(logger, dataFolder);
        String plaintext = "192.168.1.1";

        String ciphertext = enc.encrypt(plaintext);

        assertNotNull(ciphertext);
        assertNotEquals(plaintext, ciphertext, "ciphertext must not equal plaintext");
        assertEquals(plaintext, enc.decrypt(ciphertext));
    }

    @Test
    void roundTripsIpv6() {
        IpEncryption enc = new IpEncryption(logger, dataFolder);
        String plaintext = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";

        assertEquals(plaintext, enc.decrypt(enc.encrypt(plaintext)));
    }

    @Test
    void encryptionIsDeterministic() {
        // Determinism is the whole point of the ECB choice — same plaintext must
        // always produce the same ciphertext so equality lookups in the DB work.
        IpEncryption enc = new IpEncryption(logger, dataFolder);
        String plaintext = "10.0.0.42";

        assertEquals(enc.encrypt(plaintext), enc.encrypt(plaintext));
    }

    @Test
    void differentPlaintextsProduceDifferentCiphertexts() {
        IpEncryption enc = new IpEncryption(logger, dataFolder);

        assertNotEquals(enc.encrypt("10.0.0.1"), enc.encrypt("10.0.0.2"));
    }

    @Test
    void persistsKeyAcrossInstances() {
        // Two IpEncryption instances pointed at the same data folder must use the
        // same key — otherwise existing ciphertexts in the DB would not survive a
        // restart.
        IpEncryption first = new IpEncryption(logger, dataFolder);
        String ciphertext = first.encrypt("172.16.0.1");

        IpEncryption second = new IpEncryption(logger, dataFolder);

        assertEquals("172.16.0.1", second.decrypt(ciphertext));
    }

    @Test
    void writesKeyFileWithRestrictivePermissionsWhenSupported() throws Exception {
        new IpEncryption(logger, dataFolder);

        Path keyFile = dataFolder.toPath().resolve("ip-encryption.key");
        assertTrue(Files.exists(keyFile), "key file should be created on first use");

        // POSIX permissions aren't available on every filesystem (e.g. Windows or
        // some CI containers). Skip the assertion there rather than fail.
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(keyFile);
            assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), perms);
        } catch (UnsupportedOperationException ignored) {
            // POSIX not supported — nothing to assert.
        }
    }

    @Test
    void failsToInitializeWithCorruptedKeyFile() throws Exception {
        // A truncated key file is treated as data-loss territory: the constructor
        // must refuse rather than silently regenerate and orphan existing ciphertexts.
        Files.write(new File(dataFolder, "ip-encryption.key").toPath(), new byte[]{1, 2, 3});

        assertThrows(RuntimeException.class, () -> new IpEncryption(logger, dataFolder));
    }

    @Test
    void isEncryptedRejectsPlaintext() {
        IpEncryption enc = new IpEncryption(logger, dataFolder);

        assertFalse(enc.isEncrypted("192.168.1.1"));
        assertFalse(enc.isEncrypted("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
        assertFalse(enc.isEncrypted(null));
        assertFalse(enc.isEncrypted(""));
        assertFalse(enc.isEncrypted("   "));
    }

    @Test
    void isEncryptedAcceptsCiphertext() {
        IpEncryption enc = new IpEncryption(logger, dataFolder);

        assertTrue(enc.isEncrypted(enc.encrypt("192.168.1.1")));
    }

    @Test
    void encryptRejectsNullAndEmpty() {
        IpEncryption enc = new IpEncryption(logger, dataFolder);

        assertThrows(IllegalArgumentException.class, () -> enc.encrypt(null));
        assertThrows(IllegalArgumentException.class, () -> enc.encrypt(""));
        assertThrows(IllegalArgumentException.class, () -> enc.encrypt("   "));
    }

    @Test
    void decryptRejectsNullAndEmpty() {
        IpEncryption enc = new IpEncryption(logger, dataFolder);

        assertThrows(IllegalArgumentException.class, () -> enc.decrypt(null));
        assertThrows(IllegalArgumentException.class, () -> enc.decrypt(""));
        assertThrows(IllegalArgumentException.class, () -> enc.decrypt("   "));
    }
}
