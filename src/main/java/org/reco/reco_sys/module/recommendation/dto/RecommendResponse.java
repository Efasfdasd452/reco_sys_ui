package org.reco.reco_sys.module.recommendation.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecommendResponse {
    private Long recId;
    private String overallReason;
    private List<RecommendItem> items;
    private LocalDateTime createdAt;

    @Data
    public static class RecommendItem {
        private Long exerciseId;
        private String content;
        private String type;
        private String difficulty;
        private Double score;
        private String reason;
        private Integer rankOrder;
    }
}
