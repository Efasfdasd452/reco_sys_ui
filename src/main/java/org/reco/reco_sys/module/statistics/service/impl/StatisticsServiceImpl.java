package org.reco.reco_sys.module.statistics.service.impl;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.module.learning.entity.AnswerRecord;
import org.reco.reco_sys.module.learning.repository.AnswerRecordRepository;
import org.reco.reco_sys.module.recommendation.repository.RecommendationRecordRepository;
import org.reco.reco_sys.module.statistics.dto.StatisticsDto;
import org.reco.reco_sys.module.statistics.service.StatisticsService;
import org.reco.reco_sys.module.course.repository.CourseRepository;
import org.reco.reco_sys.module.exercise.repository.ExerciseRepository;
import org.reco.reco_sys.module.user.entity.SysUser;
import org.reco.reco_sys.module.user.repository.SysUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SysUserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ExerciseRepository exerciseRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final RecommendationRecordRepository recRecordRepository;

    @Override
    public StatisticsDto getOverview() {
        StatisticsDto dto = new StatisticsDto();
        dto.setTotalUsers(userRepository.count());
        dto.setTotalStudents(userRepository.findAll().stream()
                .filter(u -> u.getRole() == SysUser.Role.STUDENT).count());
        dto.setTotalTeachers(userRepository.findAll().stream()
                .filter(u -> u.getRole() == SysUser.Role.TEACHER).count());
        dto.setTotalCourses(courseRepository.count());
        dto.setTotalExercises(exerciseRepository.count());
        dto.setTotalAnswerRecords(answerRecordRepository.count());
        dto.setTotalRecommendations(recRecordRepository.count());
        dto.setPendingGrading((long) answerRecordRepository
                .findByStatusIn(List.of(AnswerRecord.Status.SUBMITTED)).size());
        return dto;
    }
}
