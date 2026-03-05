package org.reco.reco_sys.module.exercise.service;

import org.reco.reco_sys.module.exercise.dto.ExerciseCreateRequest;
import org.reco.reco_sys.module.exercise.dto.ExerciseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExerciseService {
    Page<ExerciseDto> listByCourse(Long courseId, Pageable pageable);
    ExerciseDto getById(Long id);
    ExerciseDto create(ExerciseCreateRequest request, Long creatorId);
    ExerciseDto update(Long id, ExerciseCreateRequest request);
    void delete(Long id);
}
