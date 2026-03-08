package org.reco.reco_sys.module.exercise.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExerciseNeo4jRepository extends Neo4jRepository<ExerciseNode, UUID> {
    Optional<ExerciseNode> findByMysqlId(Long mysqlId);
}
