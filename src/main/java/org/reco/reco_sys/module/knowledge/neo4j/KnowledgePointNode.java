package org.reco.reco_sys.module.knowledge.neo4j;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.UUID;

@Data
@Node("KnowledgePoint")
public class KnowledgePointNode {

    @Id
    @GeneratedValue(GeneratedValue.UUIDGenerator.class)
    private UUID id;

    private Long mysqlId;
    private String name;
    private String courseId;
}
