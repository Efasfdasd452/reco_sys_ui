package org.reco.reco_sys.module.course.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.common.util.JwtUtil;
import org.reco.reco_sys.module.course.dto.CourseCreateRequest;
import org.reco.reco_sys.module.course.dto.CourseDto;
import org.reco.reco_sys.module.course.service.CourseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public Result<List<CourseDto>> list(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(courseService.listAll(userId));
    }

    @GetMapping("/{id}")
    public Result<CourseDto> getById(@PathVariable Long id,
                                     @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(courseService.getById(id, userId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<CourseDto> create(@Valid @RequestBody CourseCreateRequest request,
                                    @RequestHeader("Authorization") String token) {
        Long teacherId = jwtUtil.getUserId(extractToken(token));
        return Result.success(courseService.create(request, teacherId));
    }

    @PostMapping("/{id}/enroll")
    public Result<Void> enroll(@PathVariable Long id,
                               @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        courseService.enroll(id, userId);
        return Result.success(null);
    }

    @DeleteMapping("/{id}/enroll")
    public Result<Void> unenroll(@PathVariable Long id,
                                 @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        courseService.unenroll(id, userId);
        return Result.success(null);
    }

    @GetMapping("/my")
    public Result<List<CourseDto>> myCourses(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(courseService.myEnrolledCourses(userId));
    }

    private String extractToken(String bearer) {
        return bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
    }
}
