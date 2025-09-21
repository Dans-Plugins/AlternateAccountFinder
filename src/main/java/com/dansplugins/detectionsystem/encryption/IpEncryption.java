package com.dansplugins.detectionsystem.encryption;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Utility class for encrypting and decrypting IP addresses.
 * Uses AES encryption with a consistent key for deterministic encryption.
 */
public final class IpEncryption {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String KEY_FILE = "plugins/AlternateAccountFinder/ip-encryption.key";
    
    private final SecretKey secretKey;
    private final Logger logger;
    
    public IpEncryption(Logger logger) {
        this.logger = logger;
        this.secretKey = getOrCreateKey();
    }
    
    /**
     * Encrypts an IP address string.
     * 
     * @param ipAddress The IP address to encrypt
     * @return Base64 encoded encrypted IP address
     */
    public String encrypt(String ipAddress) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(ipAddress.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            logger.severe("Failed to encrypt IP address: " + e.getMessage());
            throw new RuntimeException("IP encryption failed", e);
        }
    }
    
    /**
     * Decrypts an encrypted IP address string.
     * 
     * @param encryptedIp Base64 encoded encrypted IP address
     * @return Decrypted IP address string
     */
    public String decrypt(String encryptedIp) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(encryptedIp);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.severe("Failed to decrypt IP address: " + e.getMessage());
            throw new RuntimeException("IP decryption failed", e);
        }
    }
    
    /**
     * Gets the existing encryption key or creates a new one if it doesn't exist.
     */
    private SecretKey getOrCreateKey() {
        Path keyPath = Paths.get(KEY_FILE);
        
        if (Files.exists(keyPath)) {
            try {
                byte[] keyBytes = Files.readAllBytes(keyPath);
                return new SecretKeySpec(keyBytes, ALGORITHM);
            } catch (Exception e) {
                logger.warning("Failed to read existing encryption key, generating new one: " + e.getMessage());
            }
        }
        
        return createNewKey(keyPath);
    }
    
    /**
     * Creates a new encryption key and saves it to disk.
     */
    private SecretKey createNewKey(Path keyPath) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            
            // Ensure directory exists
            Files.createDirectories(keyPath.getParent());
            
            // Save key to file
            Files.write(keyPath, secretKey.getEncoded());
            
            logger.info("Generated new IP encryption key");
            return secretKey;
        } catch (Exception e) {
            logger.severe("Failed to create encryption key: " + e.getMessage());
            throw new RuntimeException("Key generation failed", e);
        }
    }
}