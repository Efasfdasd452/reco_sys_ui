package org.reco.reco_sys.module.knowledge.neo4j;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Data
@Node("KnowledgePoint")
public class KnowledgePointNode {

    @Id
    @GeneratedValue
    private Long id;

    private Long mysqlId;

    private String name;

    private String courseId;

    @Relationship(type = "PREREQUISITE_OF", direction = Relationship.Direction.OUTGOING)
    private List<KnowledgePointNode> prerequisites;

    @Relationship(type = "RELATED_TO", direction = Relationship.Direction.OUTGOING)
    private List<KnowledgePointNode> relatedPoints;
}
