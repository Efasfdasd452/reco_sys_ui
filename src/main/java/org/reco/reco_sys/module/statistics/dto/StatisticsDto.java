package org.reco.reco_sys.module.statistics.dto;

import lombok.Data;

@Data
public class StatisticsDto {
    private Long totalUsers;
    private Long totalStudents;
    private Long totalTeachers;
    private Long totalCourses;
    private Long totalExercises;
    private Long totalAnswerRecords;
    private Long totalRecommendations;
    private Long pendingGrading;
}
