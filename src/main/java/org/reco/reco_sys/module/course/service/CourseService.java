package org.reco.reco_sys.module.course.service;

import org.reco.reco_sys.module.course.dto.CourseCreateRequest;
import org.reco.reco_sys.module.course.dto.CourseDto;

import java.util.List;

public interface CourseService {
    List<CourseDto> listAll(Long currentUserId);
    CourseDto getById(Long id, Long currentUserId);
    CourseDto create(CourseCreateRequest request, Long teacherId);
    void enroll(Long courseId, Long userId);
    void unenroll(Long courseId, Long userId);
    List<CourseDto> myEnrolledCourses(Long userId);
}
