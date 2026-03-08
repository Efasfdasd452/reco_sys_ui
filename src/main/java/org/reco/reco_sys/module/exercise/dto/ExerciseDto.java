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
    private String answerKey;
    private List<Long> knowledgePointIds;
    private List<String> knowledgePointNames;
}
