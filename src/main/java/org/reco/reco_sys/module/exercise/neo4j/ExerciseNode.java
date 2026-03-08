package org.reco.reco_sys.module.exercise.neo4j;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.UUID;

/**
 * Neo4j 习题节点。
 * 与 KnowledgePoint 的 COVERS 关系通过 Neo4jClient 直接 Cypher 维护，
 * 不在实体类中声明（避免 SDN 8.x 自定义查询映射 NPE）。
 */
@Data
@Node("Exercise")
public class ExerciseNode {

    @Id
    @GeneratedValue(GeneratedValue.UUIDGenerator.class)
    private UUID id;

    private Long mysqlId;
    private Integer pyExIndex;
    private String courseId;
}
