package org.reco.reco_sys.module.recommendation.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reco.reco_sys.module.exercise.entity.Exercise;
import org.reco.reco_sys.module.exercise.repository.ExerciseRepository;
import org.reco.reco_sys.module.knowledge.entity.KnowledgePoint;
import org.reco.reco_sys.module.knowledge.repository.KnowledgePointRepository;
import org.reco.reco_sys.module.learning.entity.UserKcState;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRecordRepository recRecordRepository;
    private final RecExerciseItemRepository recItemRepository;
    private final UserKcStateRepository kcStateRepository;
    private final KnowledgePointRepository kpRepository;
    private final ExerciseRepository exerciseRepository;
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

        Map<String, Double> mlkc = new HashMap<>();
        Map<String, Double> pkc = new HashMap<>();
        for (UserKcState state : states) {
            KnowledgePoint kp = kpById.get(state.getKpId());
            if (kp == null || kp.getPyKcIndex() == null) continue;
            String key = "kc" + kp.getPyKcIndex();
            double mastery = state.getMasteryLevel();
            mlkc.put(key, mastery);
            pkc.put(key, Math.max(0.0, 1.0 - mastery));
        }

        log.info("调用推荐服务：userId={}, courseId={}, mlkc数量={}", userId, courseId, mlkc.size());

        // 2. 调用 Python 推荐服务
        List<PythonRecommendClient.RecommendationItem> pyResults =
                pythonClient.recommend("user_" + userId, mlkc, pkc, Map.of(), 10);

        // 3. 保存推荐记录
        RecommendationRecord record = new RecommendationRecord();
        record.setUserId(userId);
        record.setCourseId(courseId);
        record.setTriggeredBy("MANUAL");
        record.setReason("基于KG4Ex知识图谱推荐，共返回 " + pyResults.size() + " 条结果");
        RecommendationRecord saved = recRecordRepository.save(record);

        // 4. 将 Python exercise_id（pyExIndex）映射到 MySQL exercise，保存推荐项
        List<RecExerciseItem> items = new ArrayList<>();
        int rank = 1;
        for (PythonRecommendClient.RecommendationItem rec : pyResults) {
            Exercise ex = exerciseRepository.findByPyExIndex(rec.exercise_id()).orElse(null);
            if (ex == null) {
                log.warn("Python 推荐的 exercise_id={} 未在 MySQL 中找到对应习题，已跳过", rec.exercise_id());
                continue;
            }
            String reason = buildReason(rec.knowledge_concepts(), courseKps);

            RecExerciseItem item = new RecExerciseItem();
            item.setRecId(saved.getId());
            item.setExerciseId(ex.getId());
            item.setRankOrder(rank++);
            item.setScore(rec.score());
            item.setReason(reason);
            items.add(recItemRepository.save(item));
        }

        return buildResponse(saved, items);
    }

    @Override
    public RecommendResponse getLatest(Long userId, Long courseId) {
        return recRecordRepository.findTopByUserIdAndCourseIdOrderByCreatedAtDesc(userId, courseId)
                .map(rec -> {
                    List<RecExerciseItem> items = recItemRepository.findByRecIdOrderByRankOrder(rec.getId());
                    return buildResponse(rec, items);
                })
                .orElseGet(() -> recommend(userId, courseId));
    }

    // -------------------------------------------------------------------------
    // 私有工具
    // -------------------------------------------------------------------------

    /** 根据知识点 pyKcIndex 列表拼装推荐理由说明 */
    private String buildReason(List<Integer> kcIndices, List<KnowledgePoint> courseKps) {
        if (kcIndices == null || kcIndices.isEmpty()) return "KG4Ex推荐";
        Map<Integer, String> idxToName = courseKps.stream()
                .filter(kp -> kp.getPyKcIndex() != null)
                .collect(Collectors.toMap(KnowledgePoint::getPyKcIndex, KnowledgePoint::getName));
        String kpNames = kcIndices.stream()
                .map(idx -> idxToName.getOrDefault(idx, "kc" + idx))
                .collect(Collectors.joining("、"));
        return "涉及知识点：" + kpNames;
    }

    private RecommendResponse buildResponse(RecommendationRecord rec, List<RecExerciseItem> items) {
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
            });
            return ri;
        }).collect(Collectors.toList()));
        return response;
    }
}
