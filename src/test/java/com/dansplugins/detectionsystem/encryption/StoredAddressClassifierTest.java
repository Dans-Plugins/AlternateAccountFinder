package com.dansplugins.detectionsystem.encryption;

import com.dansplugins.detectionsystem.encryption.StoredAddressClassifier.Classification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoredAddressClassifierTest {

    @TempDir
    File dataFolder;

    @TempDir
    File otherDataFolder;

    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger(StoredAddressClassifierTest.class.getName());
    }

    private StoredAddressClassifier classifierFor(File folder) {
        return new StoredAddressClassifier(new IpEncryption(logger, folder));
    }

    @Test
    void classifiesCiphertextFromCurrentKeyAsEncrypted() {
        IpEncryption enc = new IpEncryption(logger, dataFolder);
        StoredAddressClassifier classifier = new StoredAddressClassifier(enc);

        assertEquals(Classification.ENCRYPTED, classifier.classify(enc.encrypt("192.168.1.1")));
    }

    @Test
    void classifiesPlaintextIpAsPlaintext() {
        StoredAddressClassifier classifier = classifierFor(dataFolder);

        assertEquals(Classification.PLAINTEXT_IP, classifier.classify("192.168.1.1"));
        assertEquals(Classification.PLAINTEXT_IP, classifier.classify("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
    }

    @Test
    void classifiesBlankValuesAsBlank() {
        StoredAddressClassifier classifier = classifierFor(dataFolder);

        assertEquals(Classification.BLANK, classifier.classify(null));
        assertEquals(Classification.BLANK, classifier.classify(""));
        assertEquals(Classification.BLANK, classifier.classify("   "));
    }

    @Test
    void neverClassifiesCiphertextFromAnotherKeyAsPlaintext() {
        // The regression this class exists for: with the original key gone, a fresh key is
        // generated, the old ciphertext no longer decrypts, and treating "does not decrypt" as
        // "is plaintext" would re-encrypt it into an unrecoverable double-encrypted value.
        //
        // The assertion is deliberately "not PLAINTEXT_IP" rather than "UNRECOGNIZED": a
        // wrong-key decrypt can occasionally satisfy PKCS5 padding by chance and yield mojibake,
        // in which case the value reads as ENCRYPTED. Either outcome leaves the row untouched;
        // PLAINTEXT_IP is the only one that would rewrite it.
        IpEncryption originalKey = new IpEncryption(logger, dataFolder);
        String ciphertextUnderLostKey = originalKey.encrypt("203.0.113.7");

        StoredAddressClassifier classifierWithNewKey = classifierFor(otherDataFolder);

        assertNotEquals(Classification.PLAINTEXT_IP, classifierWithNewKey.classify(ciphertextUnderLostKey));
    }

    @Test
    void classifiesNonIpTextAsUnrecognized() {
        StoredAddressClassifier classifier = classifierFor(dataFolder);

        assertEquals(Classification.UNRECOGNIZED, classifier.classify("example.com"));
        assertEquals(Classification.UNRECOGNIZED, classifier.classify("not an address"));
    }

    @Test
    void acceptsIpv4Literals() {
        assertTrue(StoredAddressClassifier.isIpLiteral("0.0.0.0"));
        assertTrue(StoredAddressClassifier.isIpLiteral("127.0.0.1"));
        assertTrue(StoredAddressClassifier.isIpLiteral("255.255.255.255"));
        assertTrue(StoredAddressClassifier.isIpLiteral("203.0.113.7"));
    }

    @Test
    void rejectsMalformedIpv4Literals() {
        assertFalse(StoredAddressClassifier.isIpLiteral("256.0.0.1"));
        assertFalse(StoredAddressClassifier.isIpLiteral("1.2.3"));
        assertFalse(StoredAddressClassifier.isIpLiteral("1.2.3.4.5"));
        assertFalse(StoredAddressClassifier.isIpLiteral("1.2.3."));
        assertFalse(StoredAddressClassifier.isIpLiteral("01.2.3.4"));
        assertFalse(StoredAddressClassifier.isIpLiteral("1.2.3.a"));
    }

    @Test
    void acceptsIpv6Literals() {
        assertTrue(StoredAddressClassifier.isIpLiteral("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
        assertTrue(StoredAddressClassifier.isIpLiteral("2001:db8:85a3::8a2e:370:7334"));
        assertTrue(StoredAddressClassifier.isIpLiteral("::1"));
        assertTrue(StoredAddressClassifier.isIpLiteral("::"));
        assertTrue(StoredAddressClassifier.isIpLiteral("fe80::1%eth0"));
        assertTrue(StoredAddressClassifier.isIpLiteral("::ffff:192.168.1.1"));
    }

    @Test
    void rejectsMalformedIpv6Literals() {
        assertFalse(StoredAddressClassifier.isIpLiteral("2001:db8::85a3::1"));
        assertFalse(StoredAddressClassifier.isIpLiteral("2001:0db8:85a3:0000:0000:8a2e:0370"));
        assertFalse(StoredAddressClassifier.isIpLiteral("2001:0db8:85a3:0000:0000:8a2e:0370:7334:1"));
        assertFalse(StoredAddressClassifier.isIpLiteral(":1:2:3:4:5:6:7"));
        assertFalse(StoredAddressClassifier.isIpLiteral("2001:db8:85a3:0:0:8a2e:370:zzzz"));
        assertFalse(StoredAddressClassifier.isIpLiteral("fe80::1%"));
        assertFalse(StoredAddressClassifier.isIpLiteral("::ffff:192.168.1.256"));
    }

    @Test
    void rejectsCiphertextShapedValuesAsIpLiterals() {
        // Base64 ciphertext must never be mistaken for a plaintext address.
        IpEncryption enc = new IpEncryption(logger, dataFolder);

        assertFalse(StoredAddressClassifier.isIpLiteral(enc.encrypt("192.168.1.1")));
        assertFalse(StoredAddressClassifier.isIpLiteral(enc.encrypt("2001:db8::1")));
    }

    @Test
    void rejectsNullAndBlankAsIpLiterals() {
        assertFalse(StoredAddressClassifier.isIpLiteral(null));
        assertFalse(StoredAddressClassifier.isIpLiteral(""));
        assertFalse(StoredAddressClassifier.isIpLiteral("   "));
    }

    @Test
    void rejectsPaddedIpLiterals() {
        // Matching exactly keeps the migration from storing the ciphertext of " 1.2.3.4 ",
        // which no later equality lookup would ever match.
        assertFalse(StoredAddressClassifier.isIpLiteral(" 192.168.1.1"));
        assertFalse(StoredAddressClassifier.isIpLiteral("192.168.1.1 "));
        assertEquals(Classification.UNRECOGNIZED, classifierFor(dataFolder).classify(" 192.168.1.1 "));
    }
}
