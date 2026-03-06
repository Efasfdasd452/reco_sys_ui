package org.reco.reco_sys.module.recommendation.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.config.AppProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Python KG4Ex 推荐服务 HTTP 客户端。
 *
 * <p>接口格式（POST /api/v1/recommend）：
 * <pre>
 * 请求：{ uid, mlkc: {kc0: 0.5, ...}, pkc: {kc0: 0.3, ...}, exfr: {ex0: 0.1, ...}, top_n: 10 }
 * 响应：{ uid, top_n, recommendations: [{exercise_id, exercise_name, score, knowledge_concepts}] }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PythonRecommendClient {

    private final AppProperties appProperties;

    /**
     * 调用推荐接口。
     *
     * @param uid    用户标识（仅用于日志）
     * @param mlkc   知识点掌握度，key 形如 "kc0"，value 为 0~1
     * @param pkc    知识点出现概率，key 形如 "kc0"，value 为 0~1
     * @param exfr   习题遗忘率，key 形如 "ex0"，value 为 0~1（可为空）
     * @param topN   推荐数量
     * @return 推荐结果列表
     */
    public List<RecommendationItem> recommend(String uid,
                                              Map<String, Double> mlkc,
                                              Map<String, Double> pkc,
                                              Map<String, Double> exfr,
                                              int topN) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("uid", uid);
            body.put("mlkc", mlkc);
            body.put("pkc", pkc);
            body.put("exfr", exfr != null ? exfr : Map.of());
            body.put("top_n", topN);

            RecommendResponse response = buildClient()
                    .post()
                    .uri(apiUrl("/api/v1/recommend"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(RecommendResponse.class);

            return response != null && response.getRecommendations() != null
                    ? response.getRecommendations()
                    : List.of();
        } catch (Exception e) {
            log.error("推荐服务调用失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.RECOMMEND_SERVICE_ERROR);
        }
    }

    /**
     * 获取 Python 模型中所有知识点列表（用于数据初始化）。
     */
    @SuppressWarnings("unchecked")
    public List<KcItem> listKnowledgeConcepts() {
        try {
            Map<?, ?> resp = buildClient()
                    .get()
                    .uri(apiUrl("/api/v1/knowledge-concepts"))
                    .retrieve()
                    .body(Map.class);
            if (resp == null) return List.of();
            List<Map<String, Object>> raw = (List<Map<String, Object>>) resp.get("knowledge_concepts");
            if (raw == null) return List.of();
            return raw.stream()
                    .map(m -> new KcItem(
                            ((Number) m.get("kc_id")).intValue(),
                            (String) m.get("kc_name")))
                    .toList();
        } catch (Exception e) {
            log.error("获取知识点列表失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.RECOMMEND_SERVICE_ERROR);
        }
    }

    /**
     * 获取 Python 模型中所有习题列表（用于数据初始化）。
     */
    @SuppressWarnings("unchecked")
    public List<ExItem> listExercises() {
        try {
            Map<?, ?> resp = buildClient()
                    .get()
                    .uri(apiUrl("/api/v1/exercises"))
                    .retrieve()
                    .body(Map.class);
            if (resp == null) return List.of();
            List<Map<String, Object>> raw = (List<Map<String, Object>>) resp.get("exercises");
            if (raw == null) return List.of();
            return raw.stream()
                    .map(m -> new ExItem(
                            ((Number) m.get("exercise_id")).intValue(),
                            (String) m.get("exercise_name"),
                            ((List<Number>) m.get("knowledge_concepts")).stream()
                                    .map(Number::intValue).toList()))
                    .toList();
        } catch (Exception e) {
            log.error("获取习题列表失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.RECOMMEND_SERVICE_ERROR);
        }
    }

    // -------------------------------------------------------------------------
    // 内部类型定义
    // -------------------------------------------------------------------------

    /** 推荐接口响应体 */
    @Data
    public static class RecommendResponse {
        private String uid;
        @JsonProperty("top_n")
        private Integer topN;
        private List<RecommendationItem> recommendations;
    }

    /** 单条推荐结果 */
    @Data
    public static class RecommendationItem {
        @JsonProperty("exercise_id")
        private Integer exerciseId;
        @JsonProperty("exercise_name")
        private String exerciseName;
        private Double score;
        @JsonProperty("knowledge_concepts")
        private List<Integer> knowledgeConcepts;
    }

    /** 知识点元数据 */
    public record KcItem(Integer kcId, String kcName) {}

    /** 习题元数据 */
    public record ExItem(Integer exId, String exName, List<Integer> kcIds) {}

    // -------------------------------------------------------------------------
    // 私有工具
    // -------------------------------------------------------------------------

    private RestClient buildClient() {
        return RestClient.builder()
                .defaultHeader("X-API-Key", appProperties.getRecommendServiceApiKey())
                .build();
    }

    private String apiUrl(String path) {
        return appProperties.getRecommendServiceUrl() + path;
    }
}
