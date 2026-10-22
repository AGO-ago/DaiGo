package org.com.first.zhao_waimai.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码工具：PBKDF2WithHmacSHA256 + 每行独立随机盐（JDK 内置，不引入额外依赖）
 */
public final class PasswordUtil {

    /** 迭代次数（登录哈希与种子数据保持一致） */
    private static final int ITERATIONS = 100000;
    /** 盐长度（字节） */
    private static final int SALT_LENGTH = 16;
    /** 派生密钥长度（bit） */
    private static final int KEY_LENGTH = 256;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    private PasswordUtil() {
    }

    /**
     * 生成随机盐（Base64）
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return ENCODER.encodeToString(salt);
    }

    /**
     * 计算密码哈希（Base64）
     */
    public static String hash(String rawPassword, String saltBase64) {
        try {
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(),
                    DECODER.decode(saltBase64), ITERATIONS, KEY_LENGTH);
            byte[] encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            return ENCODER.encodeToString(encoded);
        } catch (Exception e) {
            throw new IllegalStateException("密码哈希计算失败", e);
        }
    }

    /**
     * 校验密码（恒定时间比较，避免时序攻击）
     */
    public static boolean matches(String rawPassword, String saltBase64, String expectedHash) {
        if (rawPassword == null || saltBase64 == null || expectedHash == null) {
            return false;
        }
        byte[] actual = DECODER.decode(hash(rawPassword, saltBase64));
        byte[] expected = DECODER.decode(expectedHash);
        return MessageDigest.isEqual(actual, expected);
    }
}
