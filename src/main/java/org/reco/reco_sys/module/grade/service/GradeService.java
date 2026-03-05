package org.reco.reco_sys.module.grade.service;

import org.reco.reco_sys.module.grade.dto.GradeRequest;
import org.reco.reco_sys.module.learning.dto.AnswerRecordDto;

import java.util.List;

public interface GradeService {
    List<AnswerRecordDto> listPendingByCourse(Long courseId);
    AnswerRecordDto grade(Long recordId, GradeRequest request, Long teacherId);
}
