package org.reco.reco_sys.module.course.service;

import org.reco.reco_sys.module.course.dto.CourseCreateRequest;
import org.reco.reco_sys.module.course.dto.CourseDto;
import org.reco.reco_sys.module.course.dto.CourseStudentDto;

import java.util.List;

public interface CourseService {
    List<CourseDto> listAll(Long currentUserId);
    CourseDto getById(Long id, Long currentUserId);
    CourseDto create(CourseCreateRequest request, Long teacherId);
    void enroll(Long courseId, Long userId);
    void unenroll(Long courseId, Long userId);
    List<CourseDto> myEnrolledCourses(Long userId);
    /** 学生通过课程邀请码加入课程 */
    void joinByInviteCode(String inviteCode, Long userId);
    /** 教师/管理员：获取课程已选学生列表 */
    List<CourseStudentDto> listEnrolledStudents(Long courseId);
}
