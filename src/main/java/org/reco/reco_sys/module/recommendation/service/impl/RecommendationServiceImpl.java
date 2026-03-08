package org.reco.reco_sys.module.recommendation.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reco.reco_sys.module.exercise.entity.Exercise;
import org.reco.reco_sys.module.exercise.repository.ExerciseRepository;
import org.reco.reco_sys.module.knowledge.entity.KnowledgePoint;
import org.reco.reco_sys.module.knowledge.repository.KnowledgePointRepository;
import org.reco.reco_sys.module.learning.entity.UserKcState;
import org.reco.reco_sys.module.learning.repository.UserKcStateRepository;
import org.reco.reco_sys.module.learning.entity.AnswerRecord;
import org.reco.reco_sys.module.learning.repository.AnswerRecordRepository;
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
        // 1. 构造 mlkc / pkc
        //    mlkc[kc{i}] = 掌握度（0~1）
        //    pkc[kc{i}]  = 1 - 掌握度（近似表示"该知识点还需练习的概率"）
        List<KnowledgePoint> courseKps = kpRepository.findByCourseId(courseId);
        Map<Long, KnowledgePoint> kpById = courseKps.stream()
                .collect(Collectors.toMap(KnowledgePoint::getId, kp -> kp));

        List<UserKcState> states = kcStateRepository.findByUserId(userId);

        // 文档公式：pkc(kc_i) = kc_i出现次数 / 总答题数
        long totalExercisesDone = answerRecordRepository.countDistinctExerciseIdsByUserId(userId);

        Map<String, Double> mlkc = new HashMap<>();
        Map<String, Double> pkc = new HashMap<>();
        for (UserKcState state : states) {
            KnowledgePoint kp = kpById.get(state.getKpId());
            if (kp == null || kp.getPyKcIndex() == null) continue;
            String key = "kc" + kp.getPyKcIndex();
            // mlkc = correctCount / totalCount
            mlkc.put(key, state.getMasteryLevel());
            // pkc = kc出现次数(totalCount) / 总答题数
            double pkcVal = totalExercisesDone > 0
                    ? Math.min(1.0, (double) state.getTotalCount() / totalExercisesDone)
                    : 0.0;
            pkc.put(key, pkcVal);
        }

        // 2. 构造 exfr：根据答题记录推算遗忘率
        Map<String, Double> exfr = buildExfrMap(userId);

        log.info("调用推荐服务：userId={}, courseId={}, mlkc数量={}, exfr数量={}", userId, courseId, mlkc.size(), exfr.size());

        // 3. 调用 Python 推荐服务（传入真实 exfr）
        List<PythonRecommendClient.RecommendationItem> pyResults =
                pythonClient.recommend("user_" + userId, mlkc, pkc, exfr, 10);

        // 4. 保存推荐记录
        RecommendationRecord record = new RecommendationRecord();
        record.setUserId(userId);
        record.setCourseId(courseId);
        record.setTriggeredBy("MANUAL");
        record.setReason("基于KG4Ex知识图谱推荐，共返回 " + pyResults.size() + " 条结果");
        RecommendationRecord saved = recRecordRepository.save(record);

        // 5. 将 Python exercise_id（pyExIndex）映射到 MySQL exercise，保存推荐项
        List<RecExerciseItem> items = new ArrayList<>();
        int rank = 1;
        for (PythonRecommendClient.RecommendationItem rec : pyResults) {
            Exercise ex = exerciseRepository.findByPyExIndex(rec.getExerciseId()).orElse(null);
            if (ex == null) {
                log.warn("Python 推荐的 exercise_id={} 未在 MySQL 中找到对应习题，已跳过", rec.getExerciseId());
                continue;
            }
            String reason = buildReason(rec.getKnowledgeConcepts(), courseKps, mlkc);
            String kcIndicesJson = toJson(rec.getKnowledgeConcepts());

            RecExerciseItem item = new RecExerciseItem();
            item.setRecId(saved.getId());
            item.setExerciseId(ex.getId());
            item.setRankOrder(rank++);
            item.setScore(rec.getScore());
            item.setReason(reason);
            item.setKcIndicesJson(kcIndicesJson);
            items.add(recItemRepository.save(item));
        }

        return buildResponse(saved, items, userId, courseKps, exfr);
    }

    @Override
    public RecommendResponse getLatest(Long userId, Long courseId) {
        return recRecordRepository.findTopByUserIdAndCourseIdOrderByCreatedAtDesc(userId, courseId)
                .map(rec -> {
                    List<RecExerciseItem> items = recItemRepository.findByRecIdOrderByRankOrder(rec.getId());
                    List<KnowledgePoint> courseKps = kpRepository.findByCourseId(courseId);
                    // getLatest 时重新计算当前 exfr（掌握度可能已变化）
                    Map<String, Double> exfr = buildExfrMap(userId);
                    return buildResponse(rec, items, userId, courseKps, exfr);
                })
                .orElse(null);
    }

    // -------------------------------------------------------------------------
    // 私有工具
    // -------------------------------------------------------------------------

    /** 根据知识点 pyKcIndex 列表拼装推荐理由说明（含掌握度信息） */
    private String buildReason(List<Integer> kcIndices, List<KnowledgePoint> courseKps,
                               Map<String, Double> mlkc) {
        if (kcIndices == null || kcIndices.isEmpty()) return "KG4Ex推荐";
        Map<Integer, String> idxToName = courseKps.stream()
                .filter(kp -> kp.getPyKcIndex() != null)
                .collect(Collectors.toMap(KnowledgePoint::getPyKcIndex, KnowledgePoint::getName));
        String kpNames = kcIndices.stream()
                .map(idx -> {
                    String name = idxToName.getOrDefault(idx, "kc" + idx);
                    double mastery = mlkc.getOrDefault("kc" + idx, 0.0);
                    return name + String.format("(掌握度%.0f%%)", mastery * 100);
                })
                .collect(Collectors.joining("、"));
        return "涉及知识点：" + kpNames;
    }

    private RecommendResponse buildResponse(RecommendationRecord rec, List<RecExerciseItem> items,
                                            Long userId, List<KnowledgePoint> courseKps,
                                            Map<String, Double> exfr) {
        // 获取用户当前掌握度
        List<UserKcState> states = kcStateRepository.findByUserId(userId);
        Map<Long, Double> masteryByKpId = states.stream()
                .collect(Collectors.toMap(UserKcState::getKpId, UserKcState::getMasteryLevel));
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
            // 构建每个知识点的 mlkc/pkc 明细
            List<Integer> kcIndices = parseJson(item.getKcIndicesJson());
            if (!kcIndices.isEmpty()) {
                List<RecommendResponse.KcDetail> kcDetails = kcIndices.stream()
                        .map(idx -> {
                            KnowledgePoint kp = kpByPyIndex.get(idx);
                            RecommendResponse.KcDetail detail = new RecommendResponse.KcDetail();
                            detail.setKcName(kp != null ? kp.getName() : "kc" + idx);
                            double mastery = kp != null
                                    ? masteryByKpId.getOrDefault(kp.getId(), 0.0)
                                    : 0.0;
                            detail.setMastery(mastery);
                            detail.setPkc(Math.max(0.0, 1.0 - mastery));
                            return detail;
                        })
                        .collect(Collectors.toList());
                ri.setKcDetails(kcDetails);
            }
            return ri;
        }).collect(Collectors.toList()));
        return response;
    }

    private Map<String, Double> buildExfrMap(Long userId) {
        Map<String, Double> exfr = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        List<AnswerRecord> allRecords = answerRecordRepository.findByUserId(userId);
        Map<Long, AnswerRecord> latestByExId = new HashMap<>();
        for (AnswerRecord ar : allRecords) {
            latestByExId.merge(ar.getExerciseId(), ar,
                    (a, b) -> a.getSubmittedAt().isAfter(b.getSubmittedAt()) ? a : b);
        }
        for (Map.Entry<Long, AnswerRecord> e : latestByExId.entrySet()) {
            Exercise ex = exerciseRepository.findById(e.getKey()).orElse(null);
            if (ex == null || ex.getPyExIndex() == null) continue;
            AnswerRecord ar = e.getValue();
            double exfrVal;
            if (ar.getScore() == null || ar.getScore() == 0) {
                exfrVal = 0.7;
            } else {
                long hours = ChronoUnit.HOURS.between(ar.getSubmittedAt(), now);
                exfrVal = Math.min(1.0, hours / (24.0 * 30));
            }
            exfr.put("ex" + ex.getPyExIndex(), exfrVal);
        }
        return exfr;
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
