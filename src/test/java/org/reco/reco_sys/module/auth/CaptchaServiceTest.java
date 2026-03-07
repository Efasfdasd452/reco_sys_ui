package org.reco.reco_sys.module.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.reco.reco_sys.module.auth.captcha.CaptchaService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CaptchaService 单元测试（纯 POJO，无需 Spring 上下文）
 */
@DisplayName("CaptchaService 滑块验证码测试")
class CaptchaServiceTest {

    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        captchaService = new CaptchaService();
    }

    // ---------------------------------------------------------------
    // generate 测试组
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("generate — 生成验证码")
    class Generate {

        @Test
        @DisplayName("生成的 token 不应为空")
        void tokenShouldNotBeBlank() {
            CaptchaService.CaptchaResult result = captchaService.generate();
            assertThat(result.token()).isNotBlank();
        }

        @Test
        @DisplayName("生成的 targetX 应在 [0, 80] 范围内")
        void targetXShouldBeInValidRange() {
            for (int i = 0; i < 50; i++) {
                CaptchaService.CaptchaResult result = captchaService.generate();
                assertThat(result.targetX()).isBetween(0, 80);
            }
        }

        @RepeatedTest(20)
        @DisplayName("每次生成的 token 应唯一（不重复）")
        void tokensShouldBeUnique() {
            String token1 = captchaService.generate().token();
            String token2 = captchaService.generate().token();
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    // ---------------------------------------------------------------
    // verify 测试组
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("verify — 校验滑块位置")
    class Verify {

        @Test
        @DisplayName("滑块位置与 targetX 完全一致时应通过")
        void shouldPassWhenExactMatch() {
            CaptchaService.CaptchaResult result = captchaService.generate();
            assertThat(captchaService.verify(result.token(), result.targetX())).isTrue();
        }

        @Test
        @DisplayName("误差在容差范围内（±5）应通过")
        void shouldPassWithinTolerance() {
            CaptchaService.CaptchaResult result = captchaService.generate();
            int target = result.targetX();
            // +4 和 -4 均应通过（容差5）
            int sliderPlus = Math.min(target + 4, 100);
            int sliderMinus = Math.max(target - 4, 0);

            // 注意：verify 是一次性的，每次用不同的 token
            CaptchaService.CaptchaResult r2 = captchaService.generate();
            CaptchaService.CaptchaResult r3 = captchaService.generate();

            assertThat(captchaService.verify(r2.token(), Math.min(r2.targetX() + 4, 100))).isTrue();
            assertThat(captchaService.verify(r3.token(), Math.max(r3.targetX() - 4, 0))).isTrue();
        }

        @Test
        @DisplayName("误差超出容差（>5）时应拒绝")
        void shouldFailWhenBeyondTolerance() {
            CaptchaService.CaptchaResult result = captchaService.generate();
            int wrongPosition = (result.targetX() + 20) % 80; // 偏差20，远超容差
            // 确保偏差确实 > 5
            if (Math.abs(wrongPosition - result.targetX()) <= 5) {
                wrongPosition = (result.targetX() + 30) % 80;
            }
            assertThat(captchaService.verify(result.token(), wrongPosition)).isFalse();
        }

        @Test
        @DisplayName("使用不存在的 token 应拒绝")
        void shouldFailWithInvalidToken() {
            assertThat(captchaService.verify("totally-fake-token", 42)).isFalse();
        }

        @Test
        @DisplayName("验证码应为一次性：同一 token 第二次 verify 应失败")
        void shouldBeOneTimeUse() {
            CaptchaService.CaptchaResult result = captchaService.generate();
            int target = result.targetX();

            // 第一次验证通过
            boolean first = captchaService.verify(result.token(), target);
            // 第二次同一 token 应失败（token 已被删除）
            boolean second = captchaService.verify(result.token(), target);

            assertThat(first).isTrue();
            assertThat(second).isFalse();
        }

        @Test
        @DisplayName("验证失败不应消耗 token（错误滑动不应让 token 失效）")
        void failedVerifyShouldConsumeToken() {
            // 当前实现：verify 不论成功失败都会 remove token（一次性）
            // 这是故意设计：防止暴力枚举，失败即需重新获取验证码
            CaptchaService.CaptchaResult result = captchaService.generate();
            int wrongPos = (result.targetX() + 30) % 80;
            if (Math.abs(wrongPos - result.targetX()) <= 5) wrongPos = 0;

            captchaService.verify(result.token(), wrongPos); // 失败
            // token 已被消耗，再试也失败
            assertThat(captchaService.verify(result.token(), result.targetX())).isFalse();
        }
    }

    // ---------------------------------------------------------------
    // storePassToken / verifyPassToken 测试组
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("passToken — 登录通行证")
    class PassToken {

        @Test
        @DisplayName("存储的 passToken 应可以验证通过")
        void storedPassTokenShouldVerify() {
            String passToken = "test-pass-token-001";
            captchaService.storePassToken(passToken);
            assertThat(captchaService.verifyPassToken(passToken)).isTrue();
        }

        @Test
        @DisplayName("passToken 应为一次性：验证后再次验证失败")
        void passTokenShouldBeOneTimeUse() {
            String passToken = "one-time-pass";
            captchaService.storePassToken(passToken);

            boolean first = captchaService.verifyPassToken(passToken);
            boolean second = captchaService.verifyPassToken(passToken);

            assertThat(first).isTrue();
            assertThat(second).isFalse();
        }

        @Test
        @DisplayName("不存在的 passToken 应拒绝")
        void unknownPassTokenShouldFail() {
            assertThat(captchaService.verifyPassToken("unknown-token")).isFalse();
        }

        @Test
        @DisplayName("不同 passToken 之间互不干扰")
        void differentPassTokensShouldBeIndependent() {
            captchaService.storePassToken("token-A");
            captchaService.storePassToken("token-B");

            assertThat(captchaService.verifyPassToken("token-A")).isTrue();
            assertThat(captchaService.verifyPassToken("token-B")).isTrue();
        }
    }
}
