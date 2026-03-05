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
        private String type;
        private Double masteryLevel;
    }

    @Data
    public static class GraphEdge {
        private String source;
        private String target;
        private String label;
    }
}
