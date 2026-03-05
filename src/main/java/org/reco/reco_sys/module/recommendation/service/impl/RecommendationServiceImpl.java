package org.reco.reco_sys.module.recommendation.service.impl;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.module.exercise.entity.Exercise;
import org.reco.reco_sys.module.exercise.repository.ExerciseRepository;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRecordRepository recRecordRepository;
    private final RecExerciseItemRepository recItemRepository;
    private final UserKcStateRepository kcStateRepository;
    private final ExerciseRepository exerciseRepository;
    private final PythonRecommendClient pythonClient;

    @Override
    @Transactional
    public RecommendResponse recommend(Long userId, Long courseId) {
        List<UserKcState> states = kcStateRepository.findByUserId(userId);
        List<Map<String, Object>> kcStates = states.stream()
                .map(s -> Map.<String, Object>of("kp_id", s.getKpId(), "mastery", s.getMasteryLevel()))
                .collect(Collectors.toList());

        PythonRecommendClient.RecommendResult result = pythonClient.recommend(userId, courseId, kcStates);

        RecommendationRecord record = new RecommendationRecord();
        record.setUserId(userId);
        record.setCourseId(courseId);
        record.setTriggeredBy("MANUAL");
        record.setReason(result.reason());
        RecommendationRecord saved = recRecordRepository.save(record);

        List<RecExerciseItem> items = result.exercises().stream().map(rec -> {
            RecExerciseItem item = new RecExerciseItem();
            item.setRecId(saved.getId());
            item.setExerciseId(rec.exercise_id());
            item.setRankOrder(rec.rank());
            item.setScore(rec.score());
            item.setReason(rec.reason());
            return recItemRepository.save(item);
        }).collect(Collectors.toList());

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
