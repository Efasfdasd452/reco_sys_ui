package org.reco.reco_sys.module.grade.service.impl;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.module.course.repository.CourseRepository;
import org.reco.reco_sys.module.grade.dto.GradeRequest;
import org.reco.reco_sys.module.grade.service.GradeService;
import org.reco.reco_sys.module.learning.dto.AnswerRecordDto;
import org.reco.reco_sys.module.exercise.repository.ExerciseRepository;
import org.reco.reco_sys.module.learning.entity.AnswerRecord;
import org.reco.reco_sys.module.learning.repository.AnswerRecordRepository;
import org.reco.reco_sys.module.notification.entity.Notification;
import org.reco.reco_sys.module.notification.service.NotificationService;
import org.reco.reco_sys.module.user.entity.SysUser;
import org.reco.reco_sys.module.user.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private final AnswerRecordRepository answerRecordRepository;
    private final NotificationService notificationService;
    private final ExerciseRepository exerciseRepository;
    private final SysUserRepository userRepository;
    private final CourseRepository courseRepository;

    @Override
    public List<AnswerRecordDto> listPendingByCourse(Long courseId, Long requesterId) {
        // ADMIN 可查看所有课程；普通教师只能查看自己的课程
        SysUser requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "用户不存在"));
        if (requester.getRole() != SysUser.Role.ADMIN) {
            courseRepository.findById(courseId).ifPresent(course -> {
                if (!course.getTeacherId().equals(requesterId)) {
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该课程的批改列表");
                }
            });
        }
        return answerRecordRepository
                .findPendingGradingByCourse(courseId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AnswerRecordDto grade(Long recordId, GradeRequest request, Long teacherId) {
        AnswerRecord record = answerRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "答题记录不存在"));
        // 校验教师归属（ADMIN 免校验）
        SysUser teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "用户不存在"));
        if (teacher.getRole() != SysUser.Role.ADMIN) {
            exerciseRepository.findById(record.getExerciseId()).ifPresent(ex ->
                courseRepository.findById(ex.getCourseId()).ifPresent(course -> {
                    if (!course.getTeacherId().equals(teacherId)) {
                        throw new BusinessException(ResultCode.FORBIDDEN, "无权批改该答题记录");
                    }
                })
            );
        }
        record.setScore(request.getScore());
        record.setTeacherComment(request.getTeacherComment());
        record.setStatus(AnswerRecord.Status.GRADED);
        record.setGradedBy(teacherId);
        record.setGradedAt(LocalDateTime.now());
        AnswerRecord saved = answerRecordRepository.save(record);

        notificationService.send(
                record.getUserId(),
                Notification.Type.GRADE_DONE,
                "批改完成",
                "您的答题已批改，得分：" + request.getScore(),
                recordId
        );

        return toDto(saved);
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
        userRepository.findById(record.getUserId()).ifPresent(u ->
            dto.setStudentName(u.getNickname() != null ? u.getNickname() : u.getUsername())
        );
        exerciseRepository.findById(record.getExerciseId()).ifPresent(ex -> {
            dto.setExerciseContent(ex.getContent());
            dto.setExerciseAnswerKey(ex.getAnswerKey());
            dto.setExerciseType(ex.getType().name());
            dto.setExerciseDifficulty(ex.getDifficulty().name());
        });
        return dto;
    }
}
