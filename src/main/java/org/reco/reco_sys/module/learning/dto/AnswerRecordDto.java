package org.reco.reco_sys.module.learning.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnswerRecordDto {
    private Long id;
    private Long exerciseId;
    private String answer;
    private String status;
    private Integer score;
    private String teacherComment;
    private LocalDateTime submittedAt;
    private LocalDateTime gradedAt;
}
