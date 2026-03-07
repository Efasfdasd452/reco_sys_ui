package org.reco.reco_sys.module.knowledge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reco.reco_sys.module.knowledge.dto.GraphDto;
import org.reco.reco_sys.module.knowledge.entity.KnowledgePoint;
import org.reco.reco_sys.module.knowledge.neo4j.KnowledgePointNeo4jRepository;
import org.reco.reco_sys.module.knowledge.neo4j.KnowledgePointNode;
import org.reco.reco_sys.module.knowledge.repository.KnowledgePointRepository;
import org.reco.reco_sys.module.knowledge.service.impl.KnowledgeServiceImpl;
import org.reco.reco_sys.module.learning.entity.UserKcState;
import org.reco.reco_sys.module.learning.repository.UserKcStateRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * KnowledgeServiceImpl 单元测试
 * 重点验证修复的 Bug：
 *   原 buildGraph 只用 MySQL 父子关系构建边，
 *   修复后额外查询 Neo4j 的 PREREQUISITE_OF("先修") 和 RELATED_TO("相关") 边。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeServiceImpl 单元测试")
class KnowledgeServiceImplTest {

    @Mock private KnowledgePointRepository kpRepository;
    @Mock private KnowledgePointNeo4jRepository kpNeo4jRepository;
    @Mock private UserKcStateRepository kcStateRepository;

    @InjectMocks private KnowledgeServiceImpl knowledgeService;

    // ---------------------------------------------------------------
    // 辅助方法
    // ---------------------------------------------------------------

    private KnowledgePoint makeKp(Long id, Long courseId, String name, Long parentId) {
        KnowledgePoint kp = new KnowledgePoint();
        kp.setId(id);
        kp.setCourseId(courseId);
        kp.setName(name);
        kp.setParentId(parentId);
        return kp;
    }

    private KnowledgePointNode makeNode(Long mysqlId, String name) {
        KnowledgePointNode node = new KnowledgePointNode();
        node.setMysqlId(mysqlId);
        node.setName(name);
        return node;
    }

    private UserKcState makeState(Long userId, Long kpId, double mastery) {
        UserKcState s = new UserKcState();
        s.setUserId(userId);
        s.setKpId(kpId);
        s.setMasteryLevel(mastery);
        return s;
    }

    // ---------------------------------------------------------------
    // getGraphForStudent 测试组
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getGraphForStudent — 学生知识图谱")
    class GetGraphForStudent {

        @Test
        @DisplayName("应将知识点转换为图节点，并携带掌握度")
        void shouldBuildNodesWithMasteryLevel() {
            Long courseId = 1L, userId = 100L;
            List<KnowledgePoint> kps = List.of(
                    makeKp(1L, courseId, "线性方程", null),
                    makeKp(2L, courseId, "二次方程", null)
            );
            List<UserKcState> states = List.of(
                    makeState(userId, 1L, 0.8),
                    makeState(userId, 2L, 0.3)
            );

            when(kpRepository.findByCourseId(courseId)).thenReturn(kps);
            when(kcStateRepository.findByUserId(userId)).thenReturn(states);
            when(kpNeo4jRepository.findAllByCourseId("1")).thenReturn(List.of());

            GraphDto graph = knowledgeService.getGraphForStudent(courseId, userId);

            assertThat(graph.getNodes()).hasSize(2);
            assertThat(graph.getNodes())
                    .anyMatch(n -> n.getId().equals("kp_1") && n.getMasteryLevel() == 0.8)
                    .anyMatch(n -> n.getId().equals("kp_2") && n.getMasteryLevel() == 0.3);
        }

        @Test
        @DisplayName("未学习的知识点掌握度应默认为 0.0")
        void shouldDefaultMasteryToZeroForUnlearnedKp() {
            Long courseId = 1L, userId = 100L;
            when(kpRepository.findByCourseId(courseId)).thenReturn(
                    List.of(makeKp(5L, courseId, "导数", null))
            );
            when(kcStateRepository.findByUserId(userId)).thenReturn(List.of());
            when(kpNeo4jRepository.findAllByCourseId("1")).thenReturn(List.of());

            GraphDto graph = knowledgeService.getGraphForStudent(courseId, userId);

            assertThat(graph.getNodes().get(0).getMasteryLevel()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("【Bug修复】应包含 MySQL 父子边（包含关系）")
        void shouldIncludeParentChildEdges() {
            Long courseId = 1L, userId = 100L;
            KnowledgePoint parent = makeKp(1L, courseId, "代数", null);
            KnowledgePoint child = makeKp(2L, courseId, "线性代数", 1L);

            when(kpRepository.findByCourseId(courseId)).thenReturn(List.of(parent, child));
            when(kcStateRepository.findByUserId(userId)).thenReturn(List.of());
            when(kpNeo4jRepository.findAllByCourseId("1")).thenReturn(List.of());

            GraphDto graph = knowledgeService.getGraphForStudent(courseId, userId);

            assertThat(graph.getEdges()).hasSize(1);
            GraphDto.GraphEdge edge = graph.getEdges().get(0);
            assertThat(edge.getSource()).isEqualTo("kp_1");
            assertThat(edge.getTarget()).isEqualTo("kp_2");
            assertThat(edge.getLabel()).isEqualTo("包含");
        }

        @Test
        @DisplayName("【Bug修复】应包含 Neo4j PREREQUISITE_OF 先修边")
        void shouldIncludeNeo4jPrerequisiteEdges() {
            Long courseId = 1L, userId = 100L;
            KnowledgePoint kp1 = makeKp(1L, courseId, "基础代数", null);
            KnowledgePoint kp2 = makeKp(2L, courseId, "高等代数", null);

            KnowledgePointNode node1 = makeNode(1L, "基础代数");
            KnowledgePointNode node2 = makeNode(2L, "高等代数");
            node1.setPrerequisites(List.of(node2));

            when(kpRepository.findByCourseId(courseId)).thenReturn(List.of(kp1, kp2));
            when(kcStateRepository.findByUserId(userId)).thenReturn(List.of());
            when(kpNeo4jRepository.findAllByCourseId("1")).thenReturn(List.of(node1));

            GraphDto graph = knowledgeService.getGraphForStudent(courseId, userId);

            assertThat(graph.getEdges())
                    .anyMatch(e -> e.getSource().equals("kp_1")
                               && e.getTarget().equals("kp_2")
                               && e.getLabel().equals("先修"));
        }

        @Test
        @DisplayName("【Bug修复】应包含 Neo4j RELATED_TO 相关边")
        void shouldIncludeNeo4jRelatedEdges() {
            Long courseId = 1L, userId = 100L;
            KnowledgePoint kp1 = makeKp(1L, courseId, "概率", null);
            KnowledgePoint kp2 = makeKp(2L, courseId, "统计", null);

            KnowledgePointNode node1 = makeNode(1L, "概率");
            KnowledgePointNode node2 = makeNode(2L, "统计");
            node1.setRelatedPoints(List.of(node2));

            when(kpRepository.findByCourseId(courseId)).thenReturn(List.of(kp1, kp2));
            when(kcStateRepository.findByUserId(userId)).thenReturn(List.of());
            when(kpNeo4jRepository.findAllByCourseId("1")).thenReturn(List.of(node1));

            GraphDto graph = knowledgeService.getGraphForStudent(courseId, userId);

            assertThat(graph.getEdges())
                    .anyMatch(e -> e.getSource().equals("kp_1")
                               && e.getTarget().equals("kp_2")
                               && e.getLabel().equals("相关"));
        }

        @Test
        @DisplayName("无知识点的课程应返回空图")
        void shouldReturnEmptyGraphForEmptyCourse() {
            when(kpRepository.findByCourseId(999L)).thenReturn(List.of());
            when(kcStateRepository.findByUserId(100L)).thenReturn(List.of());
            when(kpNeo4jRepository.findAllByCourseId("999")).thenReturn(List.of());

            GraphDto graph = knowledgeService.getGraphForStudent(999L, 100L);

            assertThat(graph.getNodes()).isEmpty();
            assertThat(graph.getEdges()).isEmpty();
        }

        @Test
        @DisplayName("Neo4j 抛出异常时图谱应降级为纯 MySQL 边，不向上抛异常")
        void shouldGracefullyDegradeWhenNeo4jFails() {
            Long courseId = 1L, userId = 100L;
            KnowledgePoint parent = makeKp(1L, courseId, "代数", null);
            KnowledgePoint child = makeKp(2L, courseId, "线性代数", 1L);

            when(kpRepository.findByCourseId(courseId)).thenReturn(List.of(parent, child));
            when(kcStateRepository.findByUserId(userId)).thenReturn(List.of());
            when(kpNeo4jRepository.findAllByCourseId("1"))
                    .thenThrow(new RuntimeException("Neo4j connection refused"));

            // 不应抛出异常，降级为只有 MySQL 父子边
            GraphDto graph = knowledgeService.getGraphForStudent(courseId, userId);

            assertThat(graph.getNodes()).hasSize(2);
            assertThat(graph.getEdges()).hasSize(1); // 只有 MySQL 的父子边
            assertThat(graph.getEdges().get(0).getLabel()).isEqualTo("包含");
        }
    }

    // ---------------------------------------------------------------
    // getGraphForTeacher 测试组
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getGraphForTeacher — 教师查看指定学生图谱")
    class GetGraphForTeacher {

        @Test
        @DisplayName("教师查看学生图谱时应使用 targetUserId 的掌握度")
        void shouldUseTargetUserMastery() {
            Long courseId = 1L, studentId = 200L;
            List<KnowledgePoint> kps = List.of(makeKp(1L, courseId, "矩阵", null));
            List<UserKcState> studentStates = List.of(makeState(studentId, 1L, 0.65));

            when(kpRepository.findByCourseId(courseId)).thenReturn(kps);
            when(kcStateRepository.findByUserId(studentId)).thenReturn(studentStates);
            when(kpNeo4jRepository.findAllByCourseId("1")).thenReturn(List.of());

            GraphDto graph = knowledgeService.getGraphForTeacher(courseId, studentId);

            assertThat(graph.getNodes()).hasSize(1);
            assertThat(graph.getNodes().get(0).getMasteryLevel()).isEqualTo(0.65);
        }

        @Test
        @DisplayName("Neo4j 为空时应忽略并继续，不抛出异常")
        void shouldHandleMissingNeo4jNodeGracefully() {
            Long courseId = 1L, userId = 100L;
            List<KnowledgePoint> kps = List.of(makeKp(1L, courseId, "函数", null));

            when(kpRepository.findByCourseId(courseId)).thenReturn(kps);
            when(kcStateRepository.findByUserId(userId)).thenReturn(List.of());
            when(kpNeo4jRepository.findAllByCourseId("1")).thenReturn(List.of());

            assertThat(knowledgeService.getGraphForTeacher(courseId, userId)).isNotNull();
        }
    }
}
