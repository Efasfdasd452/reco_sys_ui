package org.reco.reco_sys.module.auth.captcha;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 图片滑块验证码图片生成器。
 *
 * <p>流程：
 * 1. 用 Graphics2D 生成随机彩色背景（渐变 + 几何图形 + 干扰文字）。
 * 2. 在背景上随机位置切出 50×50 的拼图块，保存原始像素。
 * 3. 在背景上将拼图区域替换为暗色凹槽（缺口提示）。
 * 4. 将背景横向切成 10 条（每条 30px），Fisher-Yates 打乱顺序。
 * 5. 返回：打乱后的条带列表（base64）、原始位置映射、拼图块图片（base64）、拼图Y坐标、targetX（服务端保密）。
 */
@Component
public class CaptchaImageGenerator {

    static final int BG_W = 300;
    static final int BG_H = 150;
    static final int PIECE_W = 50;
    static final int PIECE_H = 50;
    static final int STRIP_COUNT = 10;      // 每条 30px
    static final int STRIP_W = BG_W / STRIP_COUNT;

    // targetX 范围：让拼图不贴边，留出至少 1 条的宽度
    private static final int TARGET_X_MIN = STRIP_W + 5;
    private static final int TARGET_X_MAX = BG_W - PIECE_W - STRIP_W - 5;

    private static final SecureRandom RNG = new SecureRandom();

    // -----------------------------------------------------------------------

    /**
     * 生成一组验证码图片数据。
     */
    public ImageData generate() {
        try {
            // 1. 生成背景
            BufferedImage bg = createBackground();

            // 2. 随机缺口位置
            int targetX = TARGET_X_MIN + RNG.nextInt(TARGET_X_MAX - TARGET_X_MIN + 1);
            int pieceY  = 20 + RNG.nextInt(BG_H - PIECE_H - 20);

            // 3. 提取拼图块（原始像素，未被涂改）
            BufferedImage pieceSrc = deepCopy(bg.getSubimage(targetX, pieceY, PIECE_W, PIECE_H));
            // 为拼图块加边框高亮，增强可视效果
            addPieceBorder(pieceSrc);
            String pieceBase64 = toBase64(pieceSrc);

            // 4. 在背景上绘制缺口
            drawNotch(bg, targetX, pieceY);

            // 5. 切条 + 打乱
            int[] shuffleOrder = createShuffleOrder(STRIP_COUNT);
            List<String> bgStrips = new ArrayList<>();
            for (int i = 0; i < STRIP_COUNT; i++) {
                // shuffleOrder[i] = 这条发给前端的 strip 在原背景中的列号
                BufferedImage strip = deepCopy(bg.getSubimage(shuffleOrder[i] * STRIP_W, 0, STRIP_W, BG_H));
                bgStrips.add(toBase64(strip));
            }

            return new ImageData(bgStrips, shuffleOrder, pieceBase64, pieceY, targetX);

        } catch (Exception e) {
            throw new RuntimeException("验证码图片生成失败", e);
        }
    }

    // -----------------------------------------------------------------------
    // 背景生成
    // -----------------------------------------------------------------------

    private BufferedImage createBackground() {
        BufferedImage img = new BufferedImage(BG_W, BG_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 随机渐变背景
        Color c1 = randomBrightColor();
        Color c2 = randomBrightColor();
        GradientPaint gp = new GradientPaint(0, 0, c1, BG_W, BG_H, c2);
        g.setPaint(gp);
        g.fillRect(0, 0, BG_W, BG_H);

        // 半透明几何图形（干扰）
        for (int i = 0; i < 18; i++) {
            g.setColor(randomColor(50 + RNG.nextInt(80)));
            int x = RNG.nextInt(BG_W - 30);
            int y = RNG.nextInt(BG_H - 30);
            int size = 15 + RNG.nextInt(45);
            int shape = RNG.nextInt(3);
            if (shape == 0)      g.fillOval(x, y, size, size);
            else if (shape == 1) g.fillRect(x, y, size, size / 2);
            else                 g.fillRoundRect(x, y, size, size, 12, 12);
        }

        // 干扰线
        g.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < 8; i++) {
            g.setColor(randomColor(60 + RNG.nextInt(60)));
            g.drawLine(RNG.nextInt(BG_W), RNG.nextInt(BG_H),
                       RNG.nextInt(BG_W), RNG.nextInt(BG_H));
        }

        // 干扰文字（旋转散布）
        g.setFont(new Font("Arial", Font.BOLD, 16 + RNG.nextInt(10)));
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz0123456789";
        for (int i = 0; i < 10; i++) {
            g.setColor(randomColor(100 + RNG.nextInt(80)));
            String ch = String.valueOf(chars.charAt(RNG.nextInt(chars.length())));
            AffineTransform old = g.getTransform();
            int tx = 10 + RNG.nextInt(BG_W - 20);
            int ty = 20 + RNG.nextInt(BG_H - 20);
            g.translate(tx, ty);
            g.rotate(Math.toRadians(RNG.nextInt(60) - 30));
            g.drawString(ch, 0, 0);
            g.setTransform(old);
        }

        g.dispose();
        return img;
    }

    // -----------------------------------------------------------------------
    // 缺口绘制
    // -----------------------------------------------------------------------

    private void drawNotch(BufferedImage bg, int x, int y) {
        Graphics2D g = bg.createGraphics();
        // 暗色遮罩
        g.setColor(new Color(0, 0, 0, 130));
        g.fillRect(x, y, PIECE_W, PIECE_H);
        // 高亮边框
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(x, y, PIECE_W - 1, PIECE_H - 1);
        // 内部光泽
        g.setColor(new Color(255, 255, 255, 40));
        g.fillRect(x + 2, y + 2, PIECE_W - 4, (PIECE_H - 4) / 2);
        g.dispose();
    }

    // -----------------------------------------------------------------------
    // 拼图块增强
    // -----------------------------------------------------------------------

    private void addPieceBorder(BufferedImage piece) {
        Graphics2D g = piece.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 白色边框
        g.setColor(new Color(255, 255, 255, 220));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(0, 0, PIECE_W - 1, PIECE_H - 1);
        // 顶部高光
        g.setColor(new Color(255, 255, 255, 100));
        g.fillRect(1, 1, PIECE_W - 2, 6);
        // 左侧阴影
        g.setColor(new Color(0, 0, 0, 60));
        g.fillRect(0, 0, 3, PIECE_H);
        g.dispose();
    }

    // -----------------------------------------------------------------------
    // 工具方法
    // -----------------------------------------------------------------------

    private int[] createShuffleOrder(int n) {
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = RNG.nextInt(i + 1);
            int tmp = order[i]; order[i] = order[j]; order[j] = tmp;
        }
        return order;
    }

    private BufferedImage deepCopy(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(),
                                               BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    private String toBase64(BufferedImage img) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private Color randomBrightColor() {
        // 生成饱和度较高的颜色
        float hue = RNG.nextFloat();
        float sat = 0.4f + RNG.nextFloat() * 0.4f;
        float bri = 0.6f + RNG.nextFloat() * 0.3f;
        return Color.getHSBColor(hue, sat, bri);
    }

    private Color randomColor(int alpha) {
        return new Color(RNG.nextInt(256), RNG.nextInt(256), RNG.nextInt(256), alpha);
    }

    // -----------------------------------------------------------------------
    // 数据类
    // -----------------------------------------------------------------------

    /**
     * @param bgStrips    打乱后的条带列表（base64 PNG），共 STRIP_COUNT 条
     * @param stripOrder  stripOrder[i] = 第 i 条发送的 strip 在原背景中的列号
     * @param pieceImage  拼图块图片（base64 PNG, 50×50）
     * @param pieceY      拼图块在背景图中的 Y 坐标
     * @param targetX     缺口在背景图中的 X 坐标（服务端保密，不发给前端）
     */
    public record ImageData(
            List<String> bgStrips,
            int[] stripOrder,
            String pieceImage,
            int pieceY,
            int targetX
    ) {}
}
