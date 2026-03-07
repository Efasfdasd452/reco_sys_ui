package org.reco.reco_sys.module.classroom.service.impl;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.module.classroom.dto.ClassroomCreateRequest;
import org.reco.reco_sys.module.classroom.dto.ClassroomDto;
import org.reco.reco_sys.module.classroom.entity.Classroom;
import org.reco.reco_sys.module.classroom.entity.ClassroomCourse;
import org.reco.reco_sys.module.classroom.entity.ClassroomStudent;
import org.reco.reco_sys.module.classroom.repository.ClassroomCourseRepository;
import org.reco.reco_sys.module.classroom.repository.ClassroomRepository;
import org.reco.reco_sys.module.classroom.repository.ClassroomStudentRepository;
import org.reco.reco_sys.module.classroom.service.ClassroomService;
import org.reco.reco_sys.module.course.entity.UserCourse;
import org.reco.reco_sys.module.course.repository.CourseRepository;
import org.reco.reco_sys.module.course.repository.UserCourseRepository;
import org.reco.reco_sys.module.user.entity.SysUser;
import org.reco.reco_sys.module.user.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final ClassroomCourseRepository classroomCourseRepository;
    private final SysUserRepository userRepository;
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;

    @Override
    @Transactional
    public ClassroomDto create(ClassroomCreateRequest request, Long teacherId) {
        Classroom classroom = new Classroom();
        classroom.setName(request.getName());
        classroom.setDescription(request.getDescription());
        classroom.setTeacherId(teacherId);
        classroom.setInviteCode(generateInviteCode());
        return toDto(classroomRepository.save(classroom), false);
    }

    @Override
    public List<ClassroomDto> myClassrooms(Long teacherId) {
        return classroomRepository.findByTeacherIdAndIsActiveTrue(teacherId).stream()
                .map(c -> {
                    ClassroomDto dto = toDto(c, false);
                    dto.setStudentCount(classroomStudentRepository.findByClassroomId(c.getId()).size());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ClassroomDto> allClassrooms() {
        return classroomRepository.findAll().stream()
                .map(c -> {
                    ClassroomDto dto = toDto(c, false);
                    dto.setStudentCount(classroomStudentRepository.findByClassroomId(c.getId()).size());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public ClassroomDto getDetail(Long classroomId, Long currentUserId) {
        Classroom classroom = getClassroom(classroomId);
        // 只有班级教师、管理员、或班级成员才能查看详情
        SysUser currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "用户不存在"));
        boolean isAdmin = currentUser.getRole() == SysUser.Role.ADMIN;
        boolean isTeacher = classroom.getTeacherId().equals(currentUserId);
        boolean isMember = classroomStudentRepository.existsByClassroomIdAndUserId(classroomId, currentUserId);
        if (!isAdmin && !isTeacher && !isMember) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该班级详情");
        }
        ClassroomDto dto = toDto(classroom, true);
        dto.setStudentCount(dto.getStudents() != null ? dto.getStudents().size() : 0);
        // include courses
        dto.setCourses(buildCourseItems(classroomId));
        return dto;
    }

    @Override
    @Transactional
    public ClassroomDto joinByCode(String inviteCode, Long studentId) {
        Classroom classroom = classroomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "邀请码无效"));
        if (!classroom.getIsActive()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该班级已解散");
        }
        if (classroomStudentRepository.existsByClassroomIdAndUserId(classroom.getId(), studentId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "您已在该班级中");
        }
        ClassroomStudent cs = new ClassroomStudent();
        cs.setClassroomId(classroom.getId());
        cs.setUserId(studentId);
        classroomStudentRepository.save(cs);
        return toDto(classroom, false);
    }

    @Override
    @Transactional
    public void leave(Long classroomId, Long studentId) {
        ClassroomStudent cs = classroomStudentRepository.findByClassroomIdAndUserId(classroomId, studentId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "您不在该班级中"));
        classroomStudentRepository.delete(cs);
    }

    @Override
    @Transactional
    public void removeStudent(Long classroomId, Long studentId, Long currentUserId) {
        checkOwnership(classroomId, currentUserId);
        classroomStudentRepository.findByClassroomIdAndUserId(classroomId, studentId)
                .ifPresent(classroomStudentRepository::delete);
    }

    @Override
    @Transactional
    public void dissolve(Long classroomId, Long currentUserId) {
        Classroom classroom = checkOwnership(classroomId, currentUserId);
        classroom.setIsActive(false);
        classroomRepository.save(classroom);
    }

    @Override
    public List<ClassroomDto> myJoinedClassrooms(Long studentId) {
        List<Long> classroomIds = classroomStudentRepository.findClassroomIdsByUserId(studentId);
        return classroomRepository.findAllById(classroomIds).stream()
                .filter(Classroom::getIsActive)
                .map(c -> {
                    ClassroomDto dto = toDto(c, false);
                    dto.setStudentCount(classroomStudentRepository.findByClassroomId(c.getId()).size());
                    dto.setCourses(buildCourseItems(c.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addCourseToClassroom(Long classroomId, Long courseId, Long currentUserId) {
        checkOwnership(classroomId, currentUserId);
        courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "课程不存在"));
        if (classroomCourseRepository.existsByClassroomIdAndCourseId(classroomId, courseId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该课程已在班级中");
        }
        ClassroomCourse cc = new ClassroomCourse();
        cc.setClassroomId(classroomId);
        cc.setCourseId(courseId);
        classroomCourseRepository.save(cc);
    }

    @Override
    @Transactional
    public void removeCourseFromClassroom(Long classroomId, Long courseId, Long currentUserId) {
        checkOwnership(classroomId, currentUserId);
        classroomCourseRepository.deleteByClassroomIdAndCourseId(classroomId, courseId);
    }

    @Override
    @Transactional
    public void enrollStudentsInCourse(Long classroomId, Long courseId, List<Long> studentIds, Long currentUserId) {
        checkOwnership(classroomId, currentUserId);
        courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "课程不存在"));
        List<Long> toEnroll = resolveStudents(classroomId, studentIds);
        for (Long studentId : toEnroll) {
            if (!userCourseRepository.existsByUserIdAndCourseId(studentId, courseId)) {
                UserCourse uc = new UserCourse();
                uc.setUserId(studentId);
                uc.setCourseId(courseId);
                userCourseRepository.save(uc);
            }
        }
    }

    @Override
    @Transactional
    public void unenrollStudentsFromCourse(Long classroomId, Long courseId, List<Long> studentIds, Long currentUserId) {
        checkOwnership(classroomId, currentUserId);
        List<Long> toUnenroll = resolveStudents(classroomId, studentIds);
        for (Long studentId : toUnenroll) {
            userCourseRepository.findByUserIdAndCourseId(studentId, courseId)
                    .ifPresent(userCourseRepository::delete);
        }
    }

    // ---- helpers ----

    private Classroom checkOwnership(Long classroomId, Long currentUserId) {
        Classroom classroom = getClassroom(classroomId);
        SysUser user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "用户不存在"));
        if (user.getRole() != SysUser.Role.ADMIN && !classroom.getTeacherId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此班级");
        }
        return classroom;
    }

    private List<Long> resolveStudents(Long classroomId, List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return classroomStudentRepository.findByClassroomId(classroomId).stream()
                    .map(ClassroomStudent::getUserId)
                    .collect(Collectors.toList());
        }
        return studentIds;
    }

    private List<ClassroomDto.CourseItem> buildCourseItems(Long classroomId) {
        return classroomCourseRepository.findByClassroomId(classroomId).stream()
                .map(cc -> {
                    ClassroomDto.CourseItem item = new ClassroomDto.CourseItem();
                    item.setCourseId(cc.getCourseId());
                    item.setAddedAt(cc.getAddedAt());
                    courseRepository.findById(cc.getCourseId()).ifPresent(c -> {
                        item.setCourseName(c.getName());
                        item.setDescription(c.getDescription());
                        item.setInviteCode(c.getInviteCode());
                    });
                    return item;
                })
                .collect(Collectors.toList());
    }

    private Classroom getClassroom(Long id) {
        return classroomRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "班级不存在"));
    }

    private ClassroomDto toDto(Classroom classroom, boolean includeStudents) {
        ClassroomDto dto = new ClassroomDto();
        dto.setId(classroom.getId());
        dto.setName(classroom.getName());
        dto.setDescription(classroom.getDescription());
        dto.setTeacherId(classroom.getTeacherId());
        dto.setInviteCode(classroom.getInviteCode());
        dto.setIsActive(classroom.getIsActive());
        dto.setCreatedAt(classroom.getCreatedAt());
        userRepository.findById(classroom.getTeacherId()).ifPresent(t ->
            dto.setTeacherName(t.getNickname() != null ? t.getNickname() : t.getUsername())
        );
        if (includeStudents) {
            List<ClassroomStudent> members = classroomStudentRepository.findByClassroomId(classroom.getId());
            List<ClassroomDto.StudentItem> students = members.stream().map(cs -> {
                ClassroomDto.StudentItem item = new ClassroomDto.StudentItem();
                item.setUserId(cs.getUserId());
                item.setJoinedAt(cs.getJoinedAt());
                userRepository.findById(cs.getUserId()).ifPresent(u -> {
                    item.setUsername(u.getUsername());
                    item.setNickname(u.getNickname());
                });
                return item;
            }).collect(Collectors.toList());
            dto.setStudents(students);
        }
        return dto;
    }

    private String generateInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (classroomRepository.findByInviteCode(code).isPresent());
        return code;
    }
}
