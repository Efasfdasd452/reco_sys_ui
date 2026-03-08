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
        /** 该习题的遗忘率 exfr（0~1，来自答题历史推算） */
        private Double exerciseExfr;
        /** 每个关联知识点的三维度明细 */
        private java.util.List<KcDetail> kcDetails;
    }

    @Data
    public static class KcDetail {
        private String kcName;
        /** 掌握度 mlkc，0~1 */
        private Double mastery;
        /** 知识点出现概率 pkc，0~1 */
        private Double pkc;
    }
}
