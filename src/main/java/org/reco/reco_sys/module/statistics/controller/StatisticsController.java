package org.reco.reco_sys.module.statistics.controller;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.module.statistics.dto.StatisticsDto;
import org.reco.reco_sys.module.statistics.service.StatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<StatisticsDto> getOverview() {
        return Result.success(statisticsService.getOverview());
    }
}
