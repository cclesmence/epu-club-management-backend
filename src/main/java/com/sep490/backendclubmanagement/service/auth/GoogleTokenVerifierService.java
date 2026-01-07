package com.sep490.backendclubmanagement.service.auth;

import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for verifying Google ID tokens
 */
@Service
@Slf4j
public class GoogleTokenVerifierService {

    @Value("${google.client-id}")
    private String googleClientId;

    /**
     * Verify Google ID token and extract payload
     *
     * @param idTokenString The Google ID token string to verify
     * @return GoogleIdToken.Payload containing user information
     * @throws Exception if token verification fails
     */
    public GoogleIdToken.Payload verifyIdToken(String idTokenString) throws Exception {
        log.info("Verifying token starting with: {}", idTokenString.substring(0, 20) + "...");
        if (idTokenString == null || idTokenString.trim().isEmpty()) {
            log.error("ID token is null or empty");
            throw new IllegalArgumentException("ID token cannot be null or empty");
        }

        try {
            var transport = GoogleNetHttpTransport.newTrustedTransport();
            var jsonFactory = JacksonFactory.getDefaultInstance();
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                log.error("Invalid Google ID token - verification failed");
                throw new RuntimeException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            log.info("Successfully verified Google ID token for email: {}", payload.getEmail());
            return payload;

        } catch (GeneralSecurityException e) {
            log.error("Security exception during Google token verification", e);
            throw new RuntimeException("Failed to verify Google ID token due to security error", e);
        } catch (Exception e) {
            log.error("Failed to verify Google ID token", e);
            throw new RuntimeException("Failed to verify Google ID token", e);
        }
    }
}

