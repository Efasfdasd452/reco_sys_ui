package org.reco.reco_sys.module.recommendation.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reco.reco_sys.module.exercise.entity.Exercise;
import org.reco.reco_sys.module.exercise.repository.ExerciseRepository;
import org.reco.reco_sys.module.knowledge.entity.KnowledgePoint;
import org.reco.reco_sys.module.knowledge.repository.KnowledgePointRepository;
import org.reco.reco_sys.module.learning.entity.AnswerRecord;
import org.reco.reco_sys.module.learning.entity.UserKcState;
import org.reco.reco_sys.module.learning.repository.AnswerRecordRepository;
import org.reco.reco_sys.module.learning.repository.UserKcStateRepository;
import org.reco.reco_sys.module.recommendation.client.PythonRecommendClient;
import org.reco.reco_sys.module.recommendation.dto.RecommendResponse;
import org.reco.reco_sys.module.recommendation.entity.RecExerciseItem;
import org.reco.reco_sys.module.recommendation.entity.RecommendationRecord;
import org.reco.reco_sys.module.recommendation.repository.RecExerciseItemRepository;
import org.reco.reco_sys.module.recommendation.repository.RecommendationRecordRepository;
import org.reco.reco_sys.module.recommendation.service.RecommendationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RecommendationRecordRepository recRecordRepository;
    private final RecExerciseItemRepository recItemRepository;
    private final UserKcStateRepository kcStateRepository;
    private final KnowledgePointRepository kpRepository;
    private final ExerciseRepository exerciseRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final PythonRecommendClient pythonClient;

    @Override
    @Transactional
    public RecommendResponse recommend(Long userId, Long courseId) {
        List<KnowledgePoint> courseKps = kpRepository.findByCourseId(courseId);

        // 1. 将答题记录转换为 Python knowledge-state 接口格式
        //    只传已有明确对错结果（AUTO_GRADED / GRADED）的记录，按时间顺序
        List<AnswerRecord> records = answerRecordRepository.findByUserId(userId);
        records.sort((a, b) -> {
            if (a.getSubmittedAt() == null) return -1;
            if (b.getSubmittedAt() == null) return 1;
            return a.getSubmittedAt().compareTo(b.getSubmittedAt());
        });

        List<PythonRecommendClient.AnswerItem> answerItems = new ArrayList<>();
        for (AnswerRecord ar : records) {
            if (ar.getStatus() != AnswerRecord.Status.AUTO_GRADED
                    && ar.getStatus() != AnswerRecord.Status.GRADED) continue;
            Exercise ex = exerciseRepository.findById(ar.getExerciseId()).orElse(null);
            if (ex == null || ex.getPyExIndex() == null) continue;
            PythonRecommendClient.AnswerItem item = new PythonRecommendClient.AnswerItem();
            item.setExerciseId(ex.getPyExIndex());
            item.setCorrect(ar.getScore() != null && ar.getScore() > 0);
            if (ar.getSubmittedAt() != null) {
                item.setAnsweredAt(ar.getSubmittedAt().toString());
            }
            answerItems.add(item);
        }

        Map<String, Double> mlkc;
        Map<String, Double> pkc;
        Map<String, Double> exfr;

        if (answerItems.isEmpty()) {
            // 冷启动：尚无有效答题记录
            log.info("userId={} 无有效答题记录，冷启动（空知识状态）", userId);
            mlkc = Map.of();
            pkc  = Map.of();
            exfr = Map.of();
        } else {
            // 2. 调用 Python /api/v1/knowledge-state，用 DKT/LSTM 计算 mlkc/pkc/exfr
            PythonRecommendClient.KnowledgeStateResponse ksResp =
                    pythonClient.knowledgeState("user_" + userId, answerItems, 1.0);
            mlkc = ksResp.getMlkc() != null ? ksResp.getMlkc() : Map.of();
            pkc  = ksResp.getPkc()  != null ? ksResp.getPkc()  : Map.of();
            exfr = ksResp.getExfr() != null ? ksResp.getExfr() : Map.of();

            // 3. 将 Python 计算的 mlkc 同步回 user_kc_state，供学习状态页展示
            syncMlkcToDb(userId, mlkc, courseKps);
        }

        log.info("调用推荐服务：userId={}, courseId={}, mlkc数量={}, exfr数量={}",
                userId, courseId, mlkc.size(), exfr.size());

        // 4. 调用 Python /api/v1/recommend，获取 Top-N 推荐
        List<PythonRecommendClient.RecommendationItem> pyResults =
                pythonClient.recommend("user_" + userId, mlkc, pkc, exfr, 10);

        // 5. 保存推荐记录
        RecommendationRecord record = new RecommendationRecord();
        record.setUserId(userId);
        record.setCourseId(courseId);
        record.setTriggeredBy("MANUAL");
        record.setReason("基于KG4Ex知识图谱推荐（DKT），共返回 " + pyResults.size() + " 条结果");
        RecommendationRecord saved = recRecordRepository.save(record);

        // 6. 将 Python exercise_id（pyExIndex）映射到 MySQL exercise，保存推荐明细
        List<RecExerciseItem> items = new ArrayList<>();
        int rank = 1;
        for (PythonRecommendClient.RecommendationItem rec : pyResults) {
            Exercise ex = exerciseRepository.findByPyExIndex(rec.getExerciseId()).orElse(null);
            if (ex == null) {
                log.warn("Python 推荐的 exercise_id={} 未在 MySQL 中找到对应习题，已跳过", rec.getExerciseId());
                continue;
            }
            RecExerciseItem item = new RecExerciseItem();
            item.setRecId(saved.getId());
            item.setExerciseId(ex.getId());
            item.setRankOrder(rank++);
            item.setScore(rec.getScore());
            item.setReason(buildReason(rec.getKnowledgeConcepts(), courseKps, mlkc));
            item.setKcIndicesJson(toJson(rec.getKnowledgeConcepts()));
            items.add(recItemRepository.save(item));
        }

        return buildResponse(saved, items, courseKps, mlkc, pkc, exfr);
    }

    @Override
    public RecommendResponse getLatest(Long userId, Long courseId) {
        return recRecordRepository.findTopByUserIdAndCourseIdOrderByCreatedAtDesc(userId, courseId)
                .map(rec -> {
                    List<RecExerciseItem> items = recItemRepository.findByRecIdOrderByRankOrder(rec.getId());
                    List<KnowledgePoint> courseKps = kpRepository.findByCourseId(courseId);
                    // 从 DB 读取上次推荐后同步的 mlkc / pkc
                    Map<String, Double> mlkc = buildMlkcFromDb(userId, courseKps);
                    Map<String, Double> pkc  = buildPkcFromDb(userId, courseKps);
                    // exfr 使用 Java 侧艾宾浩斯公式近似（与 Python 公式一致）
                    Map<String, Double> exfr = buildExfrEbbinghaus(userId);
                    return buildResponse(rec, items, courseKps, mlkc, pkc, exfr);
                })
                .orElse(null);
    }

    // -------------------------------------------------------------------------
    // 私有工具
    // -------------------------------------------------------------------------

    /**
     * 将 Python 返回的 mlkc 同步回 user_kc_state.masteryLevel，
     * 使学习状态页能展示 DKT 计算的掌握度。
     */
    private void syncMlkcToDb(Long userId, Map<String, Double> mlkc, List<KnowledgePoint> courseKps) {
        Map<Integer, KnowledgePoint> kpByPyIndex = courseKps.stream()
                .filter(kp -> kp.getPyKcIndex() != null)
                .collect(Collectors.toMap(KnowledgePoint::getPyKcIndex, kp -> kp));

        for (Map.Entry<String, Double> e : mlkc.entrySet()) {
            if (!e.getKey().startsWith("kc")) continue;
            try {
                int pyIdx = Integer.parseInt(e.getKey().substring(2));
                KnowledgePoint kp = kpByPyIndex.get(pyIdx);
                if (kp == null) continue;
                UserKcState state = kcStateRepository.findByUserIdAndKpId(userId, kp.getId())
                        .orElseGet(() -> {
                            UserKcState s = new UserKcState();
                            s.setUserId(userId);
                            s.setKpId(kp.getId());
                            s.setCorrectCount(0);
                            s.setTotalCount(0);
                            return s;
                        });
                state.setMasteryLevel(e.getValue());
                kcStateRepository.save(state);
            } catch (NumberFormatException ignored) {}
        }
    }

    /** 从 user_kc_state 重建 mlkc map（getLatest 路径使用，值为上次推荐时同步的 DKT 结果） */
    private Map<String, Double> buildMlkcFromDb(Long userId, List<KnowledgePoint> courseKps) {
        List<UserKcState> states = kcStateRepository.findByUserId(userId);
        Map<Long, Integer> kpIdToPyIdx = courseKps.stream()
                .filter(kp -> kp.getPyKcIndex() != null)
                .collect(Collectors.toMap(KnowledgePoint::getId, KnowledgePoint::getPyKcIndex));
        Map<String, Double> mlkc = new HashMap<>();
        for (UserKcState s : states) {
            Integer pyIdx = kpIdToPyIdx.get(s.getKpId());
            if (pyIdx == null) continue;
            mlkc.put("kc" + pyIdx, s.getMasteryLevel());
        }
        return mlkc;
    }

    /** 从 user_kc_state 重建 pkc：pkc(kc_i) = totalCount / 总答题数 */
    private Map<String, Double> buildPkcFromDb(Long userId, List<KnowledgePoint> courseKps) {
        long totalDone = answerRecordRepository.countDistinctExerciseIdsByUserId(userId);
        if (totalDone == 0) return Map.of();
        List<UserKcState> states = kcStateRepository.findByUserId(userId);
        Map<Long, Integer> kpIdToPyIdx = courseKps.stream()
                .filter(kp -> kp.getPyKcIndex() != null)
                .collect(Collectors.toMap(KnowledgePoint::getId, KnowledgePoint::getPyKcIndex));
        Map<String, Double> pkc = new HashMap<>();
        for (UserKcState s : states) {
            Integer pyIdx = kpIdToPyIdx.get(s.getKpId());
            if (pyIdx == null) continue;
            pkc.put("kc" + pyIdx, Math.min(1.0, (double) s.getTotalCount() / totalDone));
        }
        return pkc;
    }

    /**
     * Java 侧艾宾浩斯遗忘曲线：exfr = 1 - 0.5^(t / T_half)
     * 与 Python 侧 knowledge-state 接口的 exfr 公式完全一致。
     * 用于 getLatest() 路径（无需再次调用 Python）。
     */
    private Map<String, Double> buildExfrEbbinghaus(Long userId) {
        final double T_HALF = 1.0; // 记忆半衰期（天）
        Map<String, Double> exfr = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        List<AnswerRecord> allRecords = answerRecordRepository.findByUserId(userId);
        // 每道题取最近一次作答时间
        Map<Long, AnswerRecord> latestByExId = new HashMap<>();
        for (AnswerRecord ar : allRecords) {
            if (ar.getSubmittedAt() == null) continue;
            latestByExId.merge(ar.getExerciseId(), ar,
                    (a, b) -> a.getSubmittedAt().isAfter(b.getSubmittedAt()) ? a : b);
        }
        for (Map.Entry<Long, AnswerRecord> e : latestByExId.entrySet()) {
            Exercise ex = exerciseRepository.findById(e.getKey()).orElse(null);
            if (ex == null || ex.getPyExIndex() == null) continue;
            double tDays = ChronoUnit.SECONDS.between(e.getValue().getSubmittedAt(), now) / 86400.0;
            double val = 1.0 - Math.pow(0.5, tDays / T_HALF);
            val = Math.round(Math.max(0.0, Math.min(1.0, val)) * 100.0) / 100.0;
            exfr.put("ex" + ex.getPyExIndex(), val);
        }
        return exfr;
    }

    /** 构造推荐理由说明（含知识点名称和 DKT 掌握度） */
    private String buildReason(List<Integer> kcIndices, List<KnowledgePoint> courseKps,
                               Map<String, Double> mlkc) {
        if (kcIndices == null || kcIndices.isEmpty()) return "KG4Ex推荐";
        Map<Integer, String> idxToName = courseKps.stream()
                .filter(kp -> kp.getPyKcIndex() != null)
                .collect(Collectors.toMap(KnowledgePoint::getPyKcIndex, KnowledgePoint::getName));
        return "涉及知识点：" + kcIndices.stream()
                .map(idx -> {
                    String name = idxToName.getOrDefault(idx, "kc" + idx);
                    double mastery = mlkc.getOrDefault("kc" + idx, 0.0);
                    return name + String.format("(掌握度%.0f%%)", mastery * 100);
                })
                .collect(Collectors.joining("、"));
    }

    private RecommendResponse buildResponse(RecommendationRecord rec, List<RecExerciseItem> items,
                                            List<KnowledgePoint> courseKps,
                                            Map<String, Double> mlkc,
                                            Map<String, Double> pkc,
                                            Map<String, Double> exfr) {
        Map<Integer, KnowledgePoint> kpByPyIndex = courseKps.stream()
                .filter(kp -> kp.getPyKcIndex() != null)
                .collect(Collectors.toMap(KnowledgePoint::getPyKcIndex, kp -> kp));

        RecommendResponse response = new RecommendResponse();
        response.setRecId(rec.getId());
        response.setOverallReason(rec.getReason());
        response.setCreatedAt(rec.getCreatedAt());
        response.setItems(items.stream().map(item -> {
            RecommendResponse.RecommendItem ri = new RecommendResponse.RecommendItem();
            ri.setExerciseId(item.getExerciseId());
            ri.setScore(item.getScore());
            ri.setReason(item.getReason());
            ri.setRankOrder(item.getRankOrder());
            exerciseRepository.findById(item.getExerciseId()).ifPresent(ex -> {
                ri.setContent(ex.getContent());
                ri.setType(ex.getType().name());
                ri.setDifficulty(ex.getDifficulty().name());
                if (ex.getPyExIndex() != null) {
                    ri.setExerciseExfr(exfr.getOrDefault("ex" + ex.getPyExIndex(), 0.0));
                }
            });
            List<Integer> kcIndices = parseJson(item.getKcIndicesJson());
            if (!kcIndices.isEmpty()) {
                List<RecommendResponse.KcDetail> kcDetails = kcIndices.stream()
                        .map(idx -> {
                            KnowledgePoint kp = kpByPyIndex.get(idx);
                            RecommendResponse.KcDetail detail = new RecommendResponse.KcDetail();
                            detail.setKcName(kp != null ? kp.getName() : "kc" + idx);
                            detail.setMastery(mlkc.getOrDefault("kc" + idx, 0.0));
                            detail.setPkc(pkc.getOrDefault("kc" + idx, 0.0));
                            return detail;
                        })
                        .collect(Collectors.toList());
                ri.setKcDetails(kcDetails);
            }
            return ri;
        }).collect(Collectors.toList()));
        return response;
    }

    private String toJson(List<Integer> list) {
        try {
            return list == null ? "[]" : objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<Integer> parseJson(String json) {
        try {
            if (json == null || json.isBlank()) return List.of();
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
