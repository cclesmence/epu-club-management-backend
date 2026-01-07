package com.sep490.backendclubmanagement.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class PayOSUtil {
    public static String generateSignature(String checksumKey, long amount, String cancelUrl,
                                           String description, long orderCode, String returnUrl) {
        try {
            String data = String.format(
                    "amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                    amount, cancelUrl, description, orderCode, returnUrl
            );

            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(checksumKey.getBytes("UTF-8"), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] hashBytes = sha256_HMAC.doFinal(data.getBytes("UTF-8"));

            // convert to HEX
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error generating signature", e);
        }
    }
}
