package org.reco.reco_sys.module.auth.service.impl;

import org.reco.reco_sys.module.auth.service.TotpService;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Service
public class TotpServiceImpl implements TotpService {

    private static final String ISSUER = "reco_sys";
    private static final long TIME_STEP = 30L;
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    @Override
    public String generateSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return base32Encode(bytes);
    }

    @Override
    public String buildQrUri(String secret, String username) {
        String label = URLEncoder.encode(ISSUER + ":" + username, StandardCharsets.UTF_8);
        String issuer = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + issuer
                + "&algorithm=SHA1&digits=6&period=30";
    }

    @Override
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != 6) return false;
        try {
            long time = System.currentTimeMillis() / 1000L / TIME_STEP;
            // 允许 ±1 个时间窗口，容忍客户端时钟偏差
            for (int i = -1; i <= 1; i++) {
                if (totp(secret, time + i).equals(code)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private String totp(String base32Secret, long counter) throws Exception {
        byte[] key = base32Decode(base32Secret);
        byte[] msg = ByteBuffer.allocate(8).putLong(counter).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(msg);
        int offset = hash[hash.length - 1] & 0x0f;
        int otp = ((hash[offset]     & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                |  (hash[offset + 3] & 0xff);
        return String.format("%06d", otp % 1_000_000);
    }

    private String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(BASE32_CHARS.charAt((buffer >> (bitsLeft - 5)) & 0x1f));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1f));
        }
        return sb.toString();
    }

    private byte[] base32Decode(String base32) {
        base32 = base32.toUpperCase().replaceAll("[=\\s]", "");
        byte[] result = new byte[base32.length() * 5 / 8];
        int buffer = 0, bitsLeft = 0, idx = 0;
        for (char c : base32.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) throw new IllegalArgumentException("非法 Base32 字符: " + c);
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[idx++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return result;
    }
}
