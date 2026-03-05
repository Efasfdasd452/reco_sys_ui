package org.reco.reco_sys.module.grade.service.impl;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.module.grade.dto.GradeRequest;
import org.reco.reco_sys.module.grade.service.GradeService;
import org.reco.reco_sys.module.learning.dto.AnswerRecordDto;
import org.reco.reco_sys.module.learning.entity.AnswerRecord;
import org.reco.reco_sys.module.learning.repository.AnswerRecordRepository;
import org.reco.reco_sys.module.notification.entity.Notification;
import org.reco.reco_sys.module.notification.service.NotificationService;
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

    @Override
    public List<AnswerRecordDto> listPendingByCourse(Long courseId) {
        return answerRecordRepository
                .findByStatusIn(List.of(AnswerRecord.Status.SUBMITTED, AnswerRecord.Status.GRADING))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AnswerRecordDto grade(Long recordId, GradeRequest request, Long teacherId) {
        AnswerRecord record = answerRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "答题记录不存在"));
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
        dto.setExerciseId(record.getExerciseId());
        dto.setAnswer(record.getAnswer());
        dto.setStatus(record.getStatus().name());
        dto.setScore(record.getScore());
        dto.setTeacherComment(record.getTeacherComment());
        dto.setSubmittedAt(record.getSubmittedAt());
        dto.setGradedAt(record.getGradedAt());
        return dto;
    }
}
