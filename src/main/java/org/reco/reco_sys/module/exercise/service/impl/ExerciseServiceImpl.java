package org.reco.reco_sys.module.exercise.service.impl;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.module.exercise.dto.ExerciseCreateRequest;
import org.reco.reco_sys.module.exercise.dto.ExerciseDto;
import org.reco.reco_sys.module.exercise.entity.Exercise;
import org.reco.reco_sys.module.exercise.entity.ExerciseKpRel;
import org.reco.reco_sys.module.exercise.neo4j.ExerciseNeo4jRepository;
import org.reco.reco_sys.module.exercise.neo4j.ExerciseNode;
import org.reco.reco_sys.module.exercise.repository.ExerciseKpRelRepository;
import org.reco.reco_sys.module.exercise.repository.ExerciseRepository;
import org.reco.reco_sys.module.exercise.service.ExerciseService;
import org.reco.reco_sys.module.knowledge.neo4j.KnowledgePointNeo4jRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseKpRelRepository kpRelRepository;
    private final ExerciseNeo4jRepository exerciseNeo4jRepository;
    private final KnowledgePointNeo4jRepository kpNeo4jRepository;

    @Override
    public Page<ExerciseDto> listByCourse(Long courseId, Pageable pageable) {
        return exerciseRepository.findByCourseId(courseId, pageable).map(this::toDto);
    }

    @Override
    public ExerciseDto getById(Long id) {
        return toDto(getExercise(id));
    }

    @Override
    @Transactional
    public ExerciseDto create(ExerciseCreateRequest request, Long creatorId) {
        Exercise ex = new Exercise();
        ex.setCourseId(request.getCourseId());
        ex.setType(Exercise.Type.valueOf(request.getType()));
        ex.setContent(request.getContent());
        ex.setAnswerKey(request.getAnswerKey());
        ex.setDifficulty(Exercise.Difficulty.valueOf(request.getDifficulty()));
        ex.setCreatorId(creatorId);
        Exercise saved = exerciseRepository.save(ex);

        List<Long> kpIds = saveKpRelations(saved.getId(), request.getKnowledgePointIds());
        syncToNeo4j(saved, kpIds);

        return toDto(saved);
    }

    @Override
    @Transactional
    public ExerciseDto update(Long id, ExerciseCreateRequest request) {
        Exercise ex = getExercise(id);
        ex.setContent(request.getContent());
        ex.setAnswerKey(request.getAnswerKey());
        ex.setDifficulty(Exercise.Difficulty.valueOf(request.getDifficulty()));
        exerciseRepository.save(ex);

        kpRelRepository.deleteByExerciseId(id);
        List<Long> kpIds = saveKpRelations(id, request.getKnowledgePointIds());
        syncToNeo4j(ex, kpIds);

        return toDto(ex);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        exerciseRepository.deleteById(id);
        kpRelRepository.deleteByExerciseId(id);
        exerciseNeo4jRepository.findByMysqlId(id).ifPresent(exerciseNeo4jRepository::delete);
    }

    private List<Long> saveKpRelations(Long exerciseId, List<Long> kpIds) {
        if (kpIds == null) return List.of();
        kpIds.forEach(kpId -> {
            ExerciseKpRel rel = new ExerciseKpRel();
            rel.setExerciseId(exerciseId);
            rel.setKpId(kpId);
            kpRelRepository.save(rel);
        });
        return kpIds;
    }

    private void syncToNeo4j(Exercise ex, List<Long> kpIds) {
        ExerciseNode node = exerciseNeo4jRepository.findByMysqlId(ex.getId())
                .orElse(new ExerciseNode());
        node.setMysqlId(ex.getId());
        node.setType(ex.getType().name());
        node.setDifficulty(ex.getDifficulty().name());
        if (kpIds != null && !kpIds.isEmpty()) {
            node.setKnowledgePoints(kpIds.stream()
                    .map(kpId -> kpNeo4jRepository.findByMysqlId(kpId).orElse(null))
                    .filter(n -> n != null)
                    .collect(Collectors.toList()));
        }
        exerciseNeo4jRepository.save(node);
    }

    private Exercise getExercise(Long id) {
        return exerciseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.EXERCISE_NOT_FOUND));
    }

    private ExerciseDto toDto(Exercise ex) {
        ExerciseDto dto = new ExerciseDto();
        dto.setId(ex.getId());
        dto.setCourseId(ex.getCourseId());
        dto.setType(ex.getType().name());
        dto.setContent(ex.getContent());
        dto.setDifficulty(ex.getDifficulty().name());
        dto.setKnowledgePointIds(kpRelRepository.findByExerciseId(ex.getId()).stream()
                .map(ExerciseKpRel::getKpId).collect(Collectors.toList()));
        return dto;
    }
}
