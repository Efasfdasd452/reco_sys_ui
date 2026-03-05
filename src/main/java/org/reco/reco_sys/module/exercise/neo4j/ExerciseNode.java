package org.reco.reco_sys.module.exercise.neo4j;

import lombok.Data;
import org.reco.reco_sys.module.knowledge.neo4j.KnowledgePointNode;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Data
@Node("Exercise")
public class ExerciseNode {

    @Id
    @GeneratedValue
    private Long id;

    private Long mysqlId;

    private String type;

    private String difficulty;

    @Relationship(type = "TESTS", direction = Relationship.Direction.OUTGOING)
    private List<KnowledgePointNode> knowledgePoints;
}
