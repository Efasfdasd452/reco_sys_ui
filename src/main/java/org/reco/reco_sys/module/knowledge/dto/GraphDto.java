package org.reco.reco_sys.module.knowledge.dto;

import lombok.Data;

import java.util.List;

@Data
public class GraphDto {

    private List<GraphNode> nodes;
    private List<GraphEdge> edges;

    @Data
    public static class GraphNode {
        private String id;
        private String label;
        /** "student" | "kc" | "exercise" */
        private String nodeType;
        /** KC 掌握度 mlkc = correctCount/totalCount（仅 kc 节点） */
        private Double masteryLevel;
        /** KC 出现概率 pkc = totalCount/总答题数（仅 kc 节点） */
        private Double pkc;
        /** 习题遗忘率 exfr（仅 exercise 节点） */
        private Double exfr;
    }

    @Data
    public static class GraphEdge {
        private String source;
        private String target;
        /** 显示标签 */
        private String label;
        /** 边类型："mlkc" | "pkc" | "exfr" | "covers" */
        private String edgeType;
    }
}
