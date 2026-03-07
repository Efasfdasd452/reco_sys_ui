package org.reco.reco_sys.module.classroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.common.util.JwtUtil;
import org.reco.reco_sys.module.classroom.dto.ClassroomCourseRequest;
import org.reco.reco_sys.module.classroom.dto.ClassroomCreateRequest;
import org.reco.reco_sys.module.classroom.dto.ClassroomDto;
import org.reco.reco_sys.module.classroom.dto.EnrollStudentsRequest;
import org.reco.reco_sys.module.classroom.service.ClassroomService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/classrooms")
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomService classroomService;
    private final JwtUtil jwtUtil;

    /** 教师：创建班级 */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<ClassroomDto> create(@Valid @RequestBody ClassroomCreateRequest request,
                                       @RequestHeader("Authorization") String token) {
        return Result.success(classroomService.create(request, userId(token)));
    }

    /** 教师：查看自己的班级列表 */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<List<ClassroomDto>> myClassrooms(@RequestHeader("Authorization") String token) {
        return Result.success(classroomService.myClassrooms(userId(token)));
    }

    /** 管理员：查看所有班级 */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<ClassroomDto>> allClassrooms() {
        return Result.success(classroomService.allClassrooms());
    }

    /** 班级详情（含学生和课程） */
    @GetMapping("/{id}")
    public Result<ClassroomDto> detail(@PathVariable Long id,
                                       @RequestHeader("Authorization") String token) {
        return Result.success(classroomService.getDetail(id, userId(token)));
    }

    /** 教师/管理员：移除学生 */
    @DeleteMapping("/{id}/students/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<Void> removeStudent(@PathVariable Long id,
                                      @PathVariable Long studentId,
                                      @RequestHeader("Authorization") String token) {
        classroomService.removeStudent(id, studentId, userId(token));
        return Result.success(null);
    }

    /** 教师/管理员：解散班级 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<Void> dissolve(@PathVariable Long id,
                                  @RequestHeader("Authorization") String token) {
        classroomService.dissolve(id, userId(token));
        return Result.success(null);
    }

    /** 学生：通过邀请码加入班级 */
    @PostMapping("/join")
    public Result<ClassroomDto> join(@RequestParam String inviteCode,
                                     @RequestHeader("Authorization") String token) {
        return Result.success(classroomService.joinByCode(inviteCode, userId(token)));
    }

    /** 学生：退出班级 */
    @DeleteMapping("/{id}/leave")
    public Result<Void> leave(@PathVariable Long id,
                               @RequestHeader("Authorization") String token) {
        classroomService.leave(id, userId(token));
        return Result.success(null);
    }

    /** 学生：查看自己加入的班级（含每班级的课程列表） */
    @GetMapping("/joined")
    public Result<List<ClassroomDto>> joinedClassrooms(@RequestHeader("Authorization") String token) {
        return Result.success(classroomService.myJoinedClassrooms(userId(token)));
    }

    /** 教师/管理员：将课程加入班级 */
    @PostMapping("/{id}/courses")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<Void> addCourse(@PathVariable Long id,
                                   @Valid @RequestBody ClassroomCourseRequest request,
                                   @RequestHeader("Authorization") String token) {
        classroomService.addCourseToClassroom(id, request.getCourseId(), userId(token));
        return Result.success(null);
    }

    /** 教师/管理员：从班级移除课程 */
    @DeleteMapping("/{id}/courses/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<Void> removeCourse(@PathVariable Long id,
                                      @PathVariable Long courseId,
                                      @RequestHeader("Authorization") String token) {
        classroomService.removeCourseFromClassroom(id, courseId, userId(token));
        return Result.success(null);
    }

    /** 教师/管理员：批量将班级学生加入课程（body为空则全班加入） */
    @PostMapping("/{id}/courses/{courseId}/enroll")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<Void> enrollStudents(@PathVariable Long id,
                                        @PathVariable Long courseId,
                                        @RequestBody(required = false) EnrollStudentsRequest request,
                                        @RequestHeader("Authorization") String token) {
        List<Long> ids = request != null ? request.getStudentIds() : Collections.emptyList();
        classroomService.enrollStudentsInCourse(id, courseId, ids, userId(token));
        return Result.success(null);
    }

    /** 教师/管理员：批量将班级学生移出课程（body为空则全班移出） */
    @DeleteMapping("/{id}/courses/{courseId}/enroll")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<Void> unenrollStudents(@PathVariable Long id,
                                          @PathVariable Long courseId,
                                          @RequestBody(required = false) EnrollStudentsRequest request,
                                          @RequestHeader("Authorization") String token) {
        List<Long> ids = request != null ? request.getStudentIds() : Collections.emptyList();
        classroomService.unenrollStudentsFromCourse(id, courseId, ids, userId(token));
        return Result.success(null);
    }

    private Long userId(String bearer) {
        String token = bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
        return jwtUtil.getUserId(token);
    }
}
