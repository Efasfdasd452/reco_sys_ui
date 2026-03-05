package org.reco.reco_sys.module.course.service.impl;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.module.course.dto.CourseCreateRequest;
import org.reco.reco_sys.module.course.dto.CourseDto;
import org.reco.reco_sys.module.course.entity.Course;
import org.reco.reco_sys.module.course.entity.UserCourse;
import org.reco.reco_sys.module.course.repository.CourseRepository;
import org.reco.reco_sys.module.course.repository.UserCourseRepository;
import org.reco.reco_sys.module.course.service.CourseService;
import org.reco.reco_sys.module.user.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final SysUserRepository userRepository;

    @Override
    public List<CourseDto> listAll(Long currentUserId) {
        return courseRepository.findByIsActiveTrue().stream()
                .map(c -> toDto(c, currentUserId))
                .collect(Collectors.toList());
    }

    @Override
    public CourseDto getById(Long id, Long currentUserId) {
        Course course = getCourse(id);
        return toDto(course, currentUserId);
    }

    @Override
    @Transactional
    public CourseDto create(CourseCreateRequest request, Long teacherId) {
        Course course = new Course();
        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setTeacherId(teacherId);
        return toDto(courseRepository.save(course), teacherId);
    }

    @Override
    @Transactional
    public void enroll(Long courseId, Long userId) {
        getCourse(courseId);
        if (userCourseRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new BusinessException(ResultCode.COURSE_ALREADY_ENROLLED);
        }
        UserCourse uc = new UserCourse();
        uc.setUserId(userId);
        uc.setCourseId(courseId);
        userCourseRepository.save(uc);
    }

    @Override
    @Transactional
    public void unenroll(Long courseId, Long userId) {
        UserCourse uc = userCourseRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new BusinessException(ResultCode.COURSE_NOT_ENROLLED));
        userCourseRepository.delete(uc);
    }

    @Override
    public List<CourseDto> myEnrolledCourses(Long userId) {
        return userCourseRepository.findByUserId(userId).stream()
                .map(uc -> courseRepository.findById(uc.getCourseId()).orElse(null))
                .filter(c -> c != null)
                .map(c -> toDto(c, userId))
                .collect(Collectors.toList());
    }

    private Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.COURSE_NOT_FOUND));
    }

    private CourseDto toDto(Course course, Long currentUserId) {
        CourseDto dto = new CourseDto();
        dto.setId(course.getId());
        dto.setName(course.getName());
        dto.setDescription(course.getDescription());
        dto.setTeacherId(course.getTeacherId());
        dto.setCreatedAt(course.getCreatedAt());
        if (currentUserId != null) {
            dto.setIsEnrolled(userCourseRepository.existsByUserIdAndCourseId(currentUserId, course.getId()));
        }
        userRepository.findById(course.getTeacherId())
                .ifPresent(t -> dto.setTeacherName(t.getNickname()));
        return dto;
    }
}
