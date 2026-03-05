package org.reco.reco_sys.module.exercise.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExerciseDto {
    private Long id;
    private Long courseId;
    private String type;
    private String content;
    private String difficulty;
    private List<Long> knowledgePointIds;
}
