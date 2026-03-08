package org.reco.reco_sys.module.learning.service.impl;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.module.exercise.entity.Exercise;
import org.reco.reco_sys.module.exercise.entity.ExerciseKpRel;
import org.reco.reco_sys.module.exercise.repository.ExerciseKpRelRepository;
import org.reco.reco_sys.module.exercise.repository.ExerciseRepository;
import org.reco.reco_sys.module.learning.dto.AnswerRecordDto;
import org.reco.reco_sys.module.learning.dto.SubmitAnswerRequest;
import org.reco.reco_sys.module.learning.entity.AnswerRecord;
import org.reco.reco_sys.module.learning.entity.UserKcState;
import org.reco.reco_sys.module.learning.repository.AnswerRecordRepository;
import org.reco.reco_sys.module.learning.repository.UserKcStateRepository;
import org.reco.reco_sys.module.learning.service.LearningService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningServiceImpl implements LearningService {

    private final AnswerRecordRepository answerRecordRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseKpRelRepository kpRelRepository;
    private final UserKcStateRepository kcStateRepository;

    @Override
    @Transactional
    public AnswerRecordDto submitAnswer(Long userId, SubmitAnswerRequest request) {
        Exercise ex = exerciseRepository.findById(request.getExerciseId())
                .orElseThrow(() -> new BusinessException(ResultCode.EXERCISE_NOT_FOUND));

        AnswerRecord record = new AnswerRecord();
        record.setUserId(userId);
        record.setExerciseId(ex.getId());
        record.setAnswer(request.getAnswer());
        record.setTimeSpent(request.getTimeSpent());

        // Auto-grade for objective questions；主观题仅记录，待教师批改后更新 correctCount
        Boolean correctOrNull = null;
        if (ex.getType() == Exercise.Type.SINGLE_CHOICE || ex.getType() == Exercise.Type.MULTIPLE_CHOICE
                || ex.getType() == Exercise.Type.FILL_BLANK || ex.getType() == Exercise.Type.TRUE_FALSE) {
            if (ex.getAnswerKey() != null) {
                boolean correct = ex.getAnswerKey().trim().equalsIgnoreCase(request.getAnswer().trim());
                record.setScore(correct ? 100 : 0);
                record.setStatus(AnswerRecord.Status.AUTO_GRADED);
                correctOrNull = correct;
            }
        } else {
            record.setStatus(AnswerRecord.Status.SUBMITTED);
        }
        // 任意题型提交都更新 KC 的 totalCount（保证 pkc 分母/分子一致）；correct 已知时同时更新 correctCount
        updateKcState(userId, ex.getId(), correctOrNull);

        return toDto(answerRecordRepository.save(record));
    }

    @Override
    public Page<AnswerRecordDto> myHistory(Long userId, Pageable pageable) {
        return answerRecordRepository.findByUserIdOrderBySubmittedAtDesc(userId, pageable).map(this::toDto);
    }

    @Override
    public List<Long> getAnsweredExerciseIds(Long userId) {
        return answerRecordRepository.findDistinctExerciseIdsByUserId(userId);
    }

    @Override
    @Transactional
    public void clearHistory(Long userId) {
        answerRecordRepository.deleteByUserId(userId);
        kcStateRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void updateKcStateFromGrading(Long userId, Long exerciseId, boolean correct) {
        List<ExerciseKpRel> rels = kpRelRepository.findByExerciseId(exerciseId);
        for (ExerciseKpRel rel : rels) {
            UserKcState state = kcStateRepository.findByUserIdAndKpId(userId, rel.getKpId())
                    .orElseGet(() -> {
                        UserKcState s = new UserKcState();
                        s.setUserId(userId);
                        s.setKpId(rel.getKpId());
                        s.setCorrectCount(0);
                        s.setTotalCount(0);
                        s.setMasteryLevel(0.0);
                        return kcStateRepository.save(s);
                    });
            if (state.getTotalCount() == 0) {
                state.setTotalCount(1);
                state.setCorrectCount(correct ? 1 : 0);
            } else {
                if (correct) {
                    state.setCorrectCount(state.getCorrectCount() + 1);
                }
            }
            state.setMasteryLevel((double) state.getCorrectCount() / state.getTotalCount());
            kcStateRepository.save(state);
        }
    }

    /**
     * 按文档公式实时更新 KC 状态：
     *   mlkc(kc_i) = correctCount / totalCount
     *   pkc(kc_i) 的分子也是 totalCount（除以总答题数在推荐侧计算）
     * @param correct 客观题已知对错时传入 true/false；主观题（问答题）传入 null，仅增加 totalCount，批改时再更新 correctCount
     */
    private void updateKcState(Long userId, Long exerciseId, Boolean correct) {
        List<ExerciseKpRel> rels = kpRelRepository.findByExerciseId(exerciseId);
        for (ExerciseKpRel rel : rels) {
            UserKcState state = kcStateRepository.findByUserIdAndKpId(userId, rel.getKpId())
                    .orElseGet(() -> {
                        UserKcState s = new UserKcState();
                        s.setUserId(userId);
                        s.setKpId(rel.getKpId());
                        s.setCorrectCount(0);
                        s.setTotalCount(0);
                        s.setMasteryLevel(0.0);
                        return s;
                    });
            state.setTotalCount(state.getTotalCount() + 1);
            if (Boolean.TRUE.equals(correct)) {
                state.setCorrectCount(state.getCorrectCount() + 1);
            }
            state.setMasteryLevel((double) state.getCorrectCount() / state.getTotalCount());
            kcStateRepository.save(state);
        }
    }

    private AnswerRecordDto toDto(AnswerRecord record) {
        AnswerRecordDto dto = new AnswerRecordDto();
        dto.setId(record.getId());
        dto.setUserId(record.getUserId());
        dto.setExerciseId(record.getExerciseId());
        dto.setAnswer(record.getAnswer());
        dto.setStatus(record.getStatus().name());
        dto.setScore(record.getScore());
        dto.setTeacherComment(record.getTeacherComment());
        dto.setSubmittedAt(record.getSubmittedAt());
        dto.setGradedAt(record.getGradedAt());
        exerciseRepository.findById(record.getExerciseId()).ifPresent(ex -> {
            dto.setExerciseType(ex.getType().name());
            dto.setExerciseDifficulty(ex.getDifficulty().name());
        });
        return dto;
    }
}
