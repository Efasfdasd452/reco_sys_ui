package org.reco.reco_sys.module.recommendation.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.config.AppProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Python KG4Ex 推荐服务 HTTP 客户端。
 *
 * <p>两阶段调用流程：
 * <pre>
 * Step1: POST /api/v1/knowledge-state
 *   请求：{ uid, answers: [{exercise_id, is_correct, answered_at}], t_half_days }
 *   响应：{ uid, mlkc: {kc0:0.71,...}, pkc: {kc0:1.0,...}, exfr: {ex0:0.43,...} }
 *
 * Step2: POST /api/v1/recommend
 *   请求：{ uid, mlkc, pkc, exfr, top_n }
 *   响应：{ uid, top_n, recommendations: [{exercise_id, exercise_name, score, knowledge_concepts}] }
 * </pre>
 */
@Slf4j
@Component
public class PythonRecommendClient {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public PythonRecommendClient(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

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
            RecommendRequestBody body = new RecommendRequestBody();
            body.setUid(uid);
            body.setMlkc(mlkc != null ? mlkc : Map.of());
            body.setPkc(pkc != null ? pkc : Map.of());
            body.setExfr(exfr != null ? exfr : Map.of());
            body.setTopN(topN);

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl("/api/v1/recommend")))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", appProperties.getRecommendServiceApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            RecommendResponse response = objectMapper.readValue(resp.body(), RecommendResponse.class);
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
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl("/api/v1/knowledge-concepts")))
                    .header("X-API-Key", appProperties.getRecommendServiceApiKey())
                    .GET()
                    .build();
            String json = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            if (json == null) return List.of();
            Map<String, Object> resp = objectMapper.readValue(json, Map.class);
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
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl("/api/v1/exercises")))
                    .header("X-API-Key", appProperties.getRecommendServiceApiKey())
                    .GET()
                    .build();
            String json = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            if (json == null) return List.of();
            Map<String, Object> resp = objectMapper.readValue(json, Map.class);
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

    /**
     * 调用知识状态计算接口（DKT/LSTM + 艾宾浩斯遗忘曲线）。
     *
     * @param uid        用户标识（仅用于日志）
     * @param answers    按时间顺序排列的答题记录
     * @param tHalfDays  艾宾浩斯记忆半衰期（天），默认 1.0
     * @return { mlkc, pkc, exfr } 三个知识状态 Map
     */
    public KnowledgeStateResponse knowledgeState(String uid,
                                                  List<AnswerItem> answers,
                                                  double tHalfDays) {
        try {
            KnowledgeStateRequest body = new KnowledgeStateRequest();
            body.setUid(uid);
            body.setAnswers(answers);
            body.setTHalfDays(tHalfDays);

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl("/api/v1/knowledge-state")))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", appProperties.getRecommendServiceApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.error("knowledge-state 调用失败: status={}, body={}", resp.statusCode(), resp.body());
                throw new BusinessException(ResultCode.RECOMMEND_SERVICE_ERROR);
            }
            KnowledgeStateResponse result = objectMapper.readValue(resp.body(), KnowledgeStateResponse.class);
            return result != null ? result : new KnowledgeStateResponse();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("knowledge-state 服务调用失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.RECOMMEND_SERVICE_ERROR);
        }
    }

    // -------------------------------------------------------------------------
    // 内部类型定义
    // -------------------------------------------------------------------------

    /** 知识状态接口请求体 */
    @Data
    public static class KnowledgeStateRequest {
        private String uid;
        private List<AnswerItem> answers;
        @JsonProperty("t_half_days")
        private double tHalfDays = 1.0;
    }

    /** 单条答题记录（传给 knowledge-state 接口） */
    @Data
    public static class AnswerItem {
        @JsonProperty("exercise_id")
        private int exerciseId;
        /** true=答对，false=答错 */
        @JsonProperty("is_correct")
        private boolean correct;
        /** ISO 8601 时间戳，可为 null（不传则该题 exfr=0.0） */
        @JsonProperty("answered_at")
        private String answeredAt;
    }

    /** 知识状态接口响应体 */
    @Data
    public static class KnowledgeStateResponse {
        private String uid;
        /** KC 掌握度，key="kc{整数}"，value=0~1（DKT 推算） */
        private Map<String, Double> mlkc;
        /** KC 出现概率，key="kc{整数}"，value=0~1（频率统计） */
        private Map<String, Double> pkc;
        /** 习题遗忘率，key="ex{整数}"，value=0~1（艾宾浩斯曲线） */
        private Map<String, Double> exfr;
    }

    /** 推荐接口请求体 */
    @Data
    public static class RecommendRequestBody {
        private String uid;
        private Map<String, Double> mlkc;
        private Map<String, Double> pkc;
        private Map<String, Double> exfr;
        @JsonProperty("top_n")
        private int topN;
    }

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

    private String apiUrl(String path) {
        return appProperties.getRecommendServiceUrl() + path;
    }
}
