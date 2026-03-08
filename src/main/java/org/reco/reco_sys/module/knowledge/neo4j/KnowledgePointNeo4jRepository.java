package org.reco.reco_sys.module.knowledge.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgePointNeo4jRepository extends Neo4jRepository<KnowledgePointNode, UUID> {

    /**
     * 按 mysqlId 查找单个节点（仅返回节点本身，关系通过 Neo4jClient 单独查询）。
     */
    @Query("MATCH (n:KnowledgePoint) WHERE n.mysqlId = $mysqlId RETURN n")
    Optional<KnowledgePointNode> findByMysqlId(Long mysqlId);

    /**
     * 按 courseId 查找所有节点（仅返回节点本身，关系通过 Neo4jClient 单独查询）。
     */
    @Query("MATCH (n:KnowledgePoint) WHERE n.courseId = $courseId RETURN n")
    List<KnowledgePointNode> findAllByCourseId(String courseId);
}
