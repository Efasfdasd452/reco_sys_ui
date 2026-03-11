package org.reco.reco_sys.module.auth.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM 加解密工具，用于保护数据库中的 TOTP 密钥。
 * 即使数据库泄露，攻击者没有 app.totp.encryption-key 也无法还原密钥。
 */
@Component
public class TotpEncryptionUtil {

    private static final int IV_LEN = 12;
    private static final int TAG_BIT = 128;

    private final SecretKeySpec keySpec;

    public TotpEncryptionUtil(@Value("${app.totp.encryption-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    /** 加密明文 TOTP 密钥 → 存入数据库的密文（Base64） */
    public String encrypt(String plainSecret) {
        try {
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BIT, iv));
            byte[] ciphertext = cipher.doFinal(plainSecret.getBytes(StandardCharsets.UTF_8));
            // 存储格式：Base64( IV(12B) || ciphertext+tag )
            byte[] combined = new byte[IV_LEN + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LEN);
            System.arraycopy(ciphertext, 0, combined, IV_LEN, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("TOTP 密钥加密失败", e);
        }
    }

    /** 解密数据库中的密文 → 明文 TOTP 密钥 */
    public String decrypt(String encodedSecret) {
        try {
            byte[] combined = Base64.getDecoder().decode(encodedSecret);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LEN);
            byte[] ciphertext = Arrays.copyOfRange(combined, IV_LEN, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BIT, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("TOTP 密钥解密失败", e);
        }
    }
}
