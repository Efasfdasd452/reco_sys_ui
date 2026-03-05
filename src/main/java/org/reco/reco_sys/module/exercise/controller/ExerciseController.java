package org.reco.reco_sys.module.exercise.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.common.util.JwtUtil;
import org.reco.reco_sys.module.exercise.dto.ExerciseCreateRequest;
import org.reco.reco_sys.module.exercise.dto.ExerciseDto;
import org.reco.reco_sys.module.exercise.service.ExerciseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final JwtUtil jwtUtil;

    @GetMapping("/course/{courseId}")
    public Result<Page<ExerciseDto>> listByCourse(@PathVariable Long courseId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return Result.success(exerciseService.listByCourse(courseId, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public Result<ExerciseDto> getById(@PathVariable Long id) {
        return Result.success(exerciseService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<ExerciseDto> create(@Valid @RequestBody ExerciseCreateRequest request,
                                      @RequestHeader("Authorization") String token) {
        Long creatorId = jwtUtil.getUserId(extractToken(token));
        return Result.success(exerciseService.create(request, creatorId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<ExerciseDto> update(@PathVariable Long id,
                                      @Valid @RequestBody ExerciseCreateRequest request) {
        return Result.success(exerciseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        exerciseService.delete(id);
        return Result.success(null);
    }

    private String extractToken(String bearer) {
        return bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
    }
}
