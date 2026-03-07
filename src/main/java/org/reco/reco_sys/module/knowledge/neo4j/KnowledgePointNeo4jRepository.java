package org.reco.reco_sys.module.knowledge.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;

public interface KnowledgePointNeo4jRepository extends Neo4jRepository<KnowledgePointNode, Long> {

    /**
     * 显式 Cypher 查询，避免 Spring Data Neo4j 8.x 派生查询在空库时的 NPE bug。
     * OPTIONAL MATCH 保证即使关系不存在也能正常返回节点。
     */
    @Query("MATCH (n:KnowledgePoint) WHERE n.mysqlId = $mysqlId " +
           "OPTIONAL MATCH (n)-[:PREREQUISITE_OF]->(pre:KnowledgePoint) " +
           "OPTIONAL MATCH (n)-[:RELATED_TO]->(rel:KnowledgePoint) " +
           "RETURN n, collect(pre), collect(rel)")
    Optional<KnowledgePointNode> findByMysqlId(Long mysqlId);

    @Query("MATCH (n:KnowledgePoint) WHERE n.courseId = $courseId " +
           "OPTIONAL MATCH (n)-[:PREREQUISITE_OF]->(pre:KnowledgePoint) " +
           "OPTIONAL MATCH (n)-[:RELATED_TO]->(rel:KnowledgePoint) " +
           "RETURN n, collect(pre), collect(rel)")
    List<KnowledgePointNode> findAllByCourseId(String courseId);
}
