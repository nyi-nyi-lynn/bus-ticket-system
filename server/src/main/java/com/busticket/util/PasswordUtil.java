package com.busticket.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

public class PasswordUtil {
    private static final String PREFIX = "v1";
    private static final int SALT_LENGTH = 16;

    private PasswordUtil() {
    }

    public static String hash(String plain) {
        Objects.requireNonNull(plain, "plain password must not be null");
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] digest = digest(salt, plain);

        return PREFIX + "$"
                + Base64.getEncoder().encodeToString(salt)
                + "$"
                + Base64.getEncoder().encodeToString(digest);
    }

    public static boolean verify(String plain, String hashed) {
        if (plain == null || hashed == null || hashed.isBlank()) {
            return false;
        }

        String[] parts = hashed.split("\\$");
        if (parts.length == 3 && PREFIX.equals(parts[0])) {
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = digest(salt, plain);
            return MessageDigest.isEqual(actual, expected);
        }

        // Backward compatibility for existing SHA-256-only hashes in legacy data.
        return hashPasswordLegacy(plain).equals(hashed);
    }

    private static byte[] digest(byte[] salt, String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(plain.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String hashPasswordLegacy(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
