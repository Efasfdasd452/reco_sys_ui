package org.reco.reco_sys.module.classroom.dto;

import lombok.Data;

import java.util.List;

@Data
public class EnrollStudentsRequest {
    /** 要操作的学生ID列表，为空时对班级内所有学生操作 */
    private List<Long> studentIds;
}
