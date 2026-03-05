package org.reco.reco_sys.module.recommendation.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.config.AppProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PythonRecommendClient {

    private final AppProperties appProperties;

    public RecommendResult recommend(Long userId, Long courseId, List<Map<String, Object>> kcStates) {
        try {
            RestClient client = RestClient.create();
            Map<String, Object> body = Map.of(
                    "user_id", userId,
                    "course_id", courseId,
                    "kc_states", kcStates
            );
            return client.post()
                    .uri(appProperties.getRecommendServiceUrl() + "/api/v1/recommend")
                    .header("X-API-Key", appProperties.getRecommendServiceApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(RecommendResult.class);
        } catch (Exception e) {
            log.error("推荐服务调用失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.RECOMMEND_SERVICE_ERROR);
        }
    }

    public void registerExercise(Long exerciseId, List<Long> kpIds) {
        try {
            RestClient client = RestClient.create();
            Map<String, Object> body = Map.of(
                    "exercise_id", exerciseId,
                    "kp_ids", kpIds
            );
            client.post()
                    .uri(appProperties.getRecommendServiceUrl() + "/api/v1/register-exercise")
                    .header("X-API-Key", appProperties.getRecommendServiceApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("注册习题到推荐服务失败 (exerciseId={}): {}", exerciseId, e.getMessage());
        }
    }

    public record RecommendResult(
            List<ExerciseRec> exercises,
            String reason
    ) {}

    public record ExerciseRec(
            Long exercise_id,
            Double score,
            String reason,
            Integer rank
    ) {}
}
