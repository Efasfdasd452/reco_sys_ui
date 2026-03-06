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

        // Auto-grade for objective questions
        if (ex.getType() == Exercise.Type.SINGLE_CHOICE || ex.getType() == Exercise.Type.MULTIPLE_CHOICE
                || ex.getType() == Exercise.Type.FILL_BLANK) {
            if (ex.getAnswerKey() != null) {
                boolean correct = ex.getAnswerKey().trim().equalsIgnoreCase(request.getAnswer().trim());
                record.setScore(correct ? 100 : 0);
                record.setStatus(AnswerRecord.Status.AUTO_GRADED);
                if (correct) updateMastery(userId, ex.getId(), 0.1);
            }
        } else {
            record.setStatus(AnswerRecord.Status.SUBMITTED);
        }

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

    private void updateMastery(Long userId, Long exerciseId, double delta) {
        List<ExerciseKpRel> rels = kpRelRepository.findByExerciseId(exerciseId);
        for (ExerciseKpRel rel : rels) {
            UserKcState state = kcStateRepository.findByUserIdAndKpId(userId, rel.getKpId())
                    .orElseGet(() -> {
                        UserKcState s = new UserKcState();
                        s.setUserId(userId);
                        s.setKpId(rel.getKpId());
                        s.setMasteryLevel(0.0);
                        return s;
                    });
            state.setMasteryLevel(Math.min(1.0, state.getMasteryLevel() + delta));
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
