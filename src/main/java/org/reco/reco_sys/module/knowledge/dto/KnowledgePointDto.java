package org.reco.reco_sys.module.knowledge.dto;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgePointDto {
    private Long id;
    private Long courseId;
    private String name;
    private String description;
    private Long parentId;
    private List<KnowledgePointDto> children;
}
