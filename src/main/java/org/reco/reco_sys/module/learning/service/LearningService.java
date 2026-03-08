package org.reco.reco_sys.module.learning.service;

import org.reco.reco_sys.module.learning.dto.AnswerRecordDto;
import org.reco.reco_sys.module.learning.dto.SubmitAnswerRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LearningService {
    AnswerRecordDto submitAnswer(Long userId, SubmitAnswerRequest request);
    Page<AnswerRecordDto> myHistory(Long userId, Pageable pageable);
    List<Long> getAnsweredExerciseIds(Long userId);
    void clearHistory(Long userId);

    /**
     * 教师批改主观题后，根据对错更新该题涉及知识点的 correctCount，用于修正 mlkc。
     * 若该题提交时未写过 UserKcState（旧数据），会先补一条 totalCount=1 的尝试。
     */
    void updateKcStateFromGrading(Long userId, Long exerciseId, boolean correct);
}
