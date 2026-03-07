package org.reco.reco_sys.module.auth.captcha;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    /**
     * 生成图片滑块验证码。
     * 返回：token、打乱的背景条带（base64数组）、条带原位置映射、拼图块图片（base64）、拼图Y坐标、图片尺寸。
     * 不返回 targetX（缺口位置保密）。
     */
    @GetMapping("/generate")
    public Result<Map<String, Object>> generate() {
        CaptchaService.CaptchaResult r = captchaService.generate();
        return Result.success(Map.of(
                "token",       r.token(),
                "bgStrips",    r.bgStrips(),
                "stripOrder",  r.stripOrder(),
                "pieceImage",  r.pieceImage(),
                "pieceY",      r.pieceY(),
                "imageWidth",  r.imageWidth(),
                "imageHeight", r.imageHeight()
        ));
    }

    /**
     * 校验滑块位置。
     *
     * @param token   generate 返回的 token
     * @param sliderX 拼图块左边缘的像素位置（0 ~ imageWidth - 50）
     */
    @PostMapping("/verify")
    public Result<Map<String, Object>> verify(@RequestParam String token,
                                              @RequestParam int sliderX) {
        boolean passed = captchaService.verify(token, sliderX);
        if (!passed) {
            return Result.success(Map.of("passed", false));
        }
        String passToken = UUID.randomUUID().toString().replace("-", "");
        captchaService.storePassToken(passToken);
        return Result.success(Map.of("passed", true, "passToken", passToken));
    }
}
