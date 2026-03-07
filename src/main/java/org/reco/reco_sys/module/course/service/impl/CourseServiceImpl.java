package org.reco.reco_sys.module.course.service.impl;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.module.course.dto.CourseCreateRequest;
import org.reco.reco_sys.module.course.dto.CourseDto;
import org.reco.reco_sys.module.course.dto.CourseStudentDto;
import org.reco.reco_sys.module.course.entity.Course;
import org.reco.reco_sys.module.course.entity.UserCourse;
import org.reco.reco_sys.module.course.repository.CourseRepository;
import org.reco.reco_sys.module.course.repository.UserCourseRepository;
import org.reco.reco_sys.module.course.service.CourseService;
import org.reco.reco_sys.module.user.entity.SysUser;
import org.reco.reco_sys.module.user.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
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
        course.setInviteCode(generateInviteCode());
        return toDto(courseRepository.save(course), teacherId);
    }

    @Override
    @Transactional
    public void joinByInviteCode(String inviteCode, Long userId) {
        Course course = courseRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "课程邀请码无效"));
        if (!Boolean.TRUE.equals(course.getIsActive())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该课程已关闭");
        }
        if (userCourseRepository.existsByUserIdAndCourseId(userId, course.getId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "您已加入该课程");
        }
        UserCourse uc = new UserCourse();
        uc.setUserId(userId);
        uc.setCourseId(course.getId());
        userCourseRepository.save(uc);
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
        SysUser user = userRepository.findById(userId).orElse(null);
        if (user != null && (user.getRole() == SysUser.Role.TEACHER || user.getRole() == SysUser.Role.ADMIN)) {
            return courseRepository.findByTeacherId(userId).stream()
                    .map(c -> toDto(c, userId))
                    .collect(Collectors.toList());
        }
        return userCourseRepository.findByUserId(userId).stream()
                .map(uc -> courseRepository.findById(uc.getCourseId()).orElse(null))
                .filter(c -> c != null)
                .map(c -> toDto(c, userId))
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseStudentDto> listEnrolledStudents(Long courseId) {
        return userCourseRepository.findUserIdsByCourseId(courseId).stream()
                .map(uid -> userRepository.findById(uid).map(u ->
                        new CourseStudentDto(u.getId(), u.getUsername(), u.getNickname()))
                        .orElse(null))
                .filter(dto -> dto != null)
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
        dto.setInviteCode(course.getInviteCode());
        dto.setCreatedAt(course.getCreatedAt());
        if (currentUserId != null) {
            dto.setIsEnrolled(userCourseRepository.existsByUserIdAndCourseId(currentUserId, course.getId()));
        }
        userRepository.findById(course.getTeacherId())
                .ifPresent(t -> dto.setTeacherName(t.getNickname()));
        return dto;
    }

    private String generateInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        } while (courseRepository.findByInviteCode(code).isPresent());
        return code;
    }
}
