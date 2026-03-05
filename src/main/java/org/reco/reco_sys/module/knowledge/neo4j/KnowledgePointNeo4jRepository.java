package org.reco.reco_sys.module.knowledge.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface KnowledgePointNeo4jRepository extends Neo4jRepository<KnowledgePointNode, Long> {
    Optional<KnowledgePointNode> findByMysqlId(Long mysqlId);
}
