package com.sep490.backendclubmanagement.config;

import com.sep490.backendclubmanagement.security.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

/**
 * JPA AttributeConverter for automatic encryption/decryption of sensitive string fields.
 * Encrypts data before persisting to database and decrypts when reading from database.
 * Handles migration from plaintext to encrypted data gracefully.
 *
 * Usage on entity field:
 * @Convert(converter = EncryptedStringConverter.class)
 * private String sensitiveField;
 */
@Slf4j
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private EncryptionService encryptionService() {
        EncryptionService svc = EncryptionService.getInstance();
        if (svc == null) {
            throw new IllegalStateException("EncryptionService not initialized. Ensure Spring context has created EncryptionService bean.");
        }
        return svc;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        // Always encrypt when saving
        return encryptionService().encryptToBase64(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        // Try to detect if data is already encrypted (Base64 format) or plaintext
        if (isLikelyEncrypted(dbData)) {
            try {
                return encryptionService().decryptFromBase64(dbData);
            } catch (Exception e) {
                log.warn("Failed to decrypt data, treating as plaintext for migration. Will be encrypted on next save.", e);
                return dbData; // Return plaintext, will be encrypted on next save
            }
        } else {
            // Data is plaintext (legacy data before encryption was implemented)
            log.debug("Detected plaintext data (not encrypted). Will be encrypted on next save.");
            return dbData; // Return as-is, will be encrypted on next save
        }
    }

    /**
     * Heuristic to determine if data is likely encrypted (Base64 encoded).
     * Checks if the string is valid Base64 and has characteristics of encrypted data.
     */
    private boolean isLikelyEncrypted(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        // Encrypted data should be longer (IV + ciphertext + authentication tag)
        // Minimum: 12 bytes IV + 16 bytes tag + some data = ~40 Base64 chars
        if (data.length() < 40) {
            return false;
        }

        // Check if it's valid Base64
        try {
            Base64.getDecoder().decode(data);
            // Valid Base64 - likely encrypted
            return true;
        } catch (IllegalArgumentException e) {
            // Not valid Base64 - definitely plaintext
            return false;
        }
    }
}

