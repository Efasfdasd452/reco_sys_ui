package org.reco.reco_sys.module.auth.captcha;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 图片滑块验证码服务（无需 Redis，纯内存存储）。
 *
 * <p>验证逻辑：
 * - generate()：调用图片生成器产生带缺口的背景条带 + 拼图块，targetX（缺口像素位置）仅存服务端。
 * - verify()：前端提交 sliderX（像素，0~250），|sliderX - targetX| ≤ TOLERANCE 则通过，一次性消费。
 * - 通行证（passToken）：验证通过后生成，60s 内登录/注册时携带，由 AuthController 校验。
 */
@Service
public class CaptchaService {

    /** 验证码有效期（秒） */
    private static final int EXPIRE_SECONDS = 120;
    /** 像素容差：允许 ±8px 误差 */
    private static final int TOLERANCE = 8;

    private final CaptchaImageGenerator imageGenerator;
    private final SecureRandom random = new SecureRandom();

    private final Map<String, CaptchaEntry> store     = new ConcurrentHashMap<>();
    private final Map<String, Long>         passStore  = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    public CaptchaService(CaptchaImageGenerator imageGenerator) {
        this.imageGenerator = imageGenerator;
        cleaner.scheduleAtFixedRate(this::cleanup, 30, 30, TimeUnit.SECONDS);
    }

    // -----------------------------------------------------------------------
    // 生成验证码
    // -----------------------------------------------------------------------

    /**
     * 生成图片验证码数据，targetX 仅存服务端、不返回给前端。
     */
    public CaptchaResult generate() {
        CaptchaImageGenerator.ImageData data = imageGenerator.generate();
        String token = UUID.randomUUID().toString().replace("-", "");
        store.put(token, new CaptchaEntry(data.targetX(), System.currentTimeMillis()));
        return new CaptchaResult(
                token,
                data.bgStrips(),
                data.stripOrder(),
                data.pieceImage(),
                data.pieceY(),
                CaptchaImageGenerator.BG_W,
                CaptchaImageGenerator.BG_H
        );
    }

    // -----------------------------------------------------------------------
    // 验证滑块位置
    // -----------------------------------------------------------------------

    /**
     * 校验前端提交的滑块像素位置（一次性消费）。
     *
     * @param token   generate 返回的 token
     * @param sliderX 前端拖动后拼图块左边缘的像素位置（0 ~ BG_W - PIECE_W = 250）
     * @return true = 通过
     */
    public boolean verify(String token, int sliderX) {
        CaptchaEntry entry = store.remove(token);
        if (entry == null) return false;
        if (System.currentTimeMillis() - entry.createdAt > EXPIRE_SECONDS * 1000L) return false;
        return Math.abs(sliderX - entry.targetX) <= TOLERANCE;
    }

    // -----------------------------------------------------------------------
    // 通行证（passToken）
    // -----------------------------------------------------------------------

    public void storePassToken(String passToken) {
        passStore.put(passToken, System.currentTimeMillis());
    }

    public boolean verifyPassToken(String passToken) {
        Long ts = passStore.remove(passToken);
        if (ts == null) return false;
        return System.currentTimeMillis() - ts <= 60_000L;
    }

    // -----------------------------------------------------------------------
    // 清理过期数据
    // -----------------------------------------------------------------------

    private void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now - e.getValue().createdAt > EXPIRE_SECONDS * 1000L);
        passStore.entrySet().removeIf(e -> now - e.getValue() > 60_000L);
    }

    // -----------------------------------------------------------------------
    // 数据类
    // -----------------------------------------------------------------------

    /**
     * 返回给前端的验证码数据（不含 targetX）。
     */
    public record CaptchaResult(
            String token,
            List<String> bgStrips,
            int[] stripOrder,
            String pieceImage,
            int pieceY,
            int imageWidth,
            int imageHeight
    ) {}

    private record CaptchaEntry(int targetX, long createdAt) {}
}
