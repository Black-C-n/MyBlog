package com.example.myblog.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 兼容 Django 的 pbkdf2_sha256 密码格式。
 * 格式：pbkdf2_sha256$迭代次数$salt$Base64编码后的哈希。
 */
public final class DjangoPasswordUtil {

    private static final int DEFAULT_ITERATIONS = 390_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final String SALT_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private DjangoPasswordUtil() {
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        String[] parts = encodedPassword.split("\\$", -1);
        if (parts.length != 4 || !"pbkdf2_sha256".equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            String calculated = encode(rawPassword, parts[2], iterations);
            return MessageDigest.isEqual(
                    calculated.getBytes(StandardCharsets.UTF_8),
                    encodedPassword.getBytes(StandardCharsets.UTF_8));
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public static String encode(String rawPassword) {
        return encode(rawPassword, randomSalt(22), DEFAULT_ITERATIONS);
    }

    private static String encode(String rawPassword, String salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(),
                    salt.getBytes(StandardCharsets.UTF_8), iterations, KEY_LENGTH_BITS);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            return "pbkdf2_sha256$" + iterations + "$" + salt + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("无法加密密码", exception);
        }
    }

    private static String randomSalt(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder salt = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            salt.append(SALT_CHARS.charAt(random.nextInt(SALT_CHARS.length())));
        }
        return salt.toString();
    }
}
