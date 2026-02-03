package com.dansplugins.detectionsystem.encryption;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Utility class for encrypting and decrypting IP addresses.
 * <p>
 * This class uses AES in {@code AES/ECB/PKCS5Padding} mode with a single, consistent key
 * stored on disk. Using a fixed key and ECB mode makes the encryption <em>deterministic</em>:
 * the same IP address will always produce the same ciphertext. This determinism is required
 * so that encrypted IPs can be used for equality comparisons and database lookups (for example,
 * to detect alternate accounts that share an IP).
 * <p>
 * <strong>Security trade-offs:</strong>
 * <ul>
 *   <li>ECB mode does <em>not</em> hide patterns in the data. If the same IP address is
 *       encrypted multiple times, the resulting ciphertext will be identical each time.</li>
 *   <li>This design is intentionally weaker than using a randomized mode (such as GCM or CBC
 *       with a random IV), but it is chosen here to support deterministic lookups.</li>
 *   <li>Do not reuse this class for encrypting highly sensitive data where pattern leakage is
 *       unacceptable; it is intended only for this specific IP-detection use case.</li>
 * </ul>
 * <p>
 * <strong>Key management:</strong>
 * <ul>
 *   <li>The key file <em>must</em> be backed up. If the key is lost or overwritten, any IP
 *       addresses encrypted with the old key can no longer be decrypted, and all historical
 *       database lookups based on those encrypted IPs will fail.</li>
 *   <li>If the key changes, previously stored ciphertexts become permanently unusable; there is
 *       no way to recover the data without the original key.</li>
 *   <li>On startup, a prominent warning is logged about the importance of backing up the key file.</li>
 * </ul>
 */
public final class IpEncryption {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String KEY_FILENAME = "ip-encryption.key";
    
    private final SecretKey secretKey;
    private final Logger logger;
    private final File dataFolder;
    
    public IpEncryption(Logger logger, File dataFolder) {
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.secretKey = getOrCreateKey();
        logKeyBackupWarning();
    }
    
    /**
     * Logs a prominent warning about the importance of backing up the encryption key.
     */
    private void logKeyBackupWarning() {
        logger.warning("=================================================================");
        logger.warning("IMPORTANT: IP Encryption Key Information");
        logger.warning("=================================================================");
        logger.warning("IP addresses are encrypted using a key file stored at:");
        logger.warning("  " + getKeyFile().getAbsolutePath());
        logger.warning("");
        logger.warning("*** CRITICAL: BACK UP THIS KEY FILE ***");
        logger.warning("");
        logger.warning("If this key file is lost, corrupted, or deleted:");
        logger.warning("  - All encrypted IP addresses in the database will be unrecoverable");
        logger.warning("  - Alternate account detection will fail for historical data");
        logger.warning("  - You will lose access to all stored IP information");
        logger.warning("");
        logger.warning("Back up this file regularly along with your database backups.");
        logger.warning("=================================================================");
    }
    
    /**
     * Encrypts an IP address string.
     * 
     * @param ipAddress The IP address to encrypt
     * @return Base64 encoded encrypted IP address
     * @throws IllegalArgumentException if ipAddress is null or empty
     * @throws RuntimeException if encryption fails
     */
    public String encrypt(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address to encrypt must not be null or empty");
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(ipAddress.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            logger.severe("Failed to encrypt IP address");
            throw new RuntimeException("IP encryption failed", e);
        }
    }
    
    /**
     * Decrypts an encrypted IP address string.
     * 
     * @param encryptedIp Base64 encoded encrypted IP address
     * @return Decrypted IP address string
     * @throws IllegalArgumentException if encryptedIp is null or empty
     * @throws RuntimeException if decryption fails
     */
    public String decrypt(String encryptedIp) {
        if (encryptedIp == null || encryptedIp.trim().isEmpty()) {
            throw new IllegalArgumentException("Encrypted IP address must not be null or empty");
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(encryptedIp);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.severe("Failed to decrypt IP address");
            throw new RuntimeException("IP decryption failed", e);
        }
    }
    
    /**
     * Checks if a given string appears to be an encrypted IP address by attempting to decrypt it.
     * 
     * @param address The address to check
     * @return true if the address can be successfully decrypted, false otherwise
     */
    public boolean isEncrypted(String address) {
        if (address == null || address.trim().isEmpty()) {
            return false;
        }
        
        try {
            decrypt(address);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets the key file path.
     */
    private File getKeyFile() {
        return new File(dataFolder, KEY_FILENAME);
    }
    
    /**
     * Gets the existing encryption key or creates a new one if it doesn't exist.
     * 
     * @throws RuntimeException if the key cannot be loaded or created
     */
    private SecretKey getOrCreateKey() {
        File keyFile = getKeyFile();
        Path keyPath = keyFile.toPath();
        
        if (keyFile.exists()) {
            try {
                byte[] keyBytes = Files.readAllBytes(keyPath);
                if (keyBytes.length != 32) { // AES-256 key is 32 bytes
                    logger.severe("Encryption key file is corrupted (invalid size). Expected 32 bytes, got " + keyBytes.length);
                    logger.severe("*** ALL EXISTING ENCRYPTED DATA WILL BE UNRECOVERABLE ***");
                    throw new RuntimeException("Corrupted encryption key file. Cannot proceed without data loss.");
                }
                logger.info("Loaded existing IP encryption key from " + keyFile.getAbsolutePath());
                return new SecretKeySpec(keyBytes, ALGORITHM);
            } catch (IOException e) {
                logger.severe("Failed to read encryption key file: " + e.getMessage());
                logger.severe("*** ALL EXISTING ENCRYPTED DATA WILL BE UNRECOVERABLE IF KEY IS REGENERATED ***");
                throw new RuntimeException("Cannot read encryption key file. Check file permissions and integrity.", e);
            } catch (SecurityException e) {
                logger.severe("Security manager prevented reading encryption key file: " + e.getMessage());
                throw new RuntimeException("Security policy prevents reading encryption key.", e);
            }
        }
        
        return createNewKey(keyPath);
    }
    
    /**
     * Creates a new encryption key and saves it to disk with restricted permissions.
     * 
     * @throws RuntimeException if key generation or saving fails
     */
    private SecretKey createNewKey(Path keyPath) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256, new java.security.SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            
            // Ensure directory exists
            Files.createDirectories(keyPath.getParent());
            
            // Save key to file
            Files.write(keyPath, secretKey.getEncoded());
            
            // Set restrictive file permissions (Unix-like systems only)
            setRestrictivePermissions(keyPath);
            
            logger.info("Generated new IP encryption key at " + keyPath.toAbsolutePath());
            logger.warning("NEW ENCRYPTION KEY CREATED - Back up this file immediately!");
            
            return secretKey;
        } catch (Exception e) {
            logger.severe("Failed to create encryption key: " + e.getMessage());
            throw new RuntimeException("Key generation failed", e);
        }
    }
    
    /**
     * Sets restrictive file permissions on the key file (owner read/write only).
     * This method is best-effort and will silently fail on non-POSIX systems.
     */
    private void setRestrictivePermissions(Path keyPath) {
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(keyPath, perms);
            logger.info("Set restrictive permissions (600) on encryption key file");
        } catch (UnsupportedOperationException e) {
            // POSIX file permissions not supported on this system (e.g., Windows)
            logger.info("File permission restrictions not available on this system");
        } catch (Exception e) {
            logger.warning("Could not set restrictive permissions on key file: " + e.getMessage());
        }
    }
}