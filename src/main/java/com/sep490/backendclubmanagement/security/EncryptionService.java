package com.sep490.backendclubmanagement.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive data using AES-GCM.
 * Used for protecting sensitive payment information like API keys, client IDs, and checksum keys.
 */
@Slf4j
@Component
public class EncryptionService {

    @Value("${app.encryption.base64Key}")
    private String base64Key;

    private SecretKeySpec keySpec;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int IV_SIZE = 12; // Recommended for GCM
    private static final int TAG_LENGTH_BITS = 128;

    // Static instance for use in JPA AttributeConverter
    private static EncryptionService INSTANCE;

    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                throw new IllegalArgumentException("Invalid AES key length. Must be 16, 24, or 32 bytes for AES-128, AES-192, or AES-256");
            }
            this.keySpec = new SecretKeySpec(keyBytes, "AES");
            INSTANCE = this;
            log.info("EncryptionService initialized successfully with AES-{} key", keyBytes.length * 8);
        } catch (Exception e) {
            log.error("Failed to initialize EncryptionService", e);
            throw new RuntimeException("Failed to initialize encryption service", e);
        }
    }

    public static EncryptionService getInstance() {
        return INSTANCE;
    }

    /**
     * Encrypts plaintext and returns a Base64-encoded string containing IV + ciphertext.
     * @param plaintext The text to encrypt
     * @return Base64-encoded encrypted data, or null if input is null
     */
    public String encryptToBase64(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a Base64-encoded encrypted string.
     * @param base64 Base64-encoded encrypted data (IV + ciphertext)
     * @return Decrypted plaintext, or null if input is null
     */
    public String decryptFromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[IV_SIZE];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Masks sensitive data for display purposes.
     * Shows only first 4 and last 4 characters, masking the rest.
     * @param value The value to mask
     * @return Masked string, or null if input is null
     */
    public String maskSensitiveData(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}

