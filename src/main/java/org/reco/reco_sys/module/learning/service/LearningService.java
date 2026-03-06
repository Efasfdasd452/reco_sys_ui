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
}
