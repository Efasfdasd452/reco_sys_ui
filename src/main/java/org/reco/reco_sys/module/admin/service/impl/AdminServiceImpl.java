package org.reco.reco_sys.module.admin.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.module.admin.service.AdminService;
import org.reco.reco_sys.module.course.entity.Course;
import org.reco.reco_sys.module.course.repository.CourseRepository;
import org.reco.reco_sys.module.exercise.entity.Exercise;
import org.reco.reco_sys.module.exercise.entity.ExerciseKpRel;
import org.reco.reco_sys.module.exercise.repository.ExerciseKpRelRepository;
import org.reco.reco_sys.module.exercise.repository.ExerciseRepository;
import org.reco.reco_sys.module.knowledge.entity.KnowledgePoint;
import org.reco.reco_sys.module.knowledge.neo4j.KnowledgePointNeo4jRepository;
import org.reco.reco_sys.module.knowledge.neo4j.KnowledgePointNode;
import org.reco.reco_sys.module.knowledge.repository.KnowledgePointRepository;
import org.reco.reco_sys.module.recommendation.client.PythonRecommendClient;
import org.reco.reco_sys.module.user.dto.UserProfileDto;
import org.reco.reco_sys.module.user.entity.SysUser;
import org.reco.reco_sys.module.user.repository.SysUserRepository;
import org.reco.reco_sys.module.user.service.impl.UserServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final SysUserRepository userRepository;
    private final UserServiceImpl userServiceImpl;
    private final CourseRepository courseRepository;
    private final KnowledgePointRepository kpRepository;
    private final KnowledgePointNeo4jRepository kpNeo4jRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseKpRelRepository kpRelRepository;
    private final PythonRecommendClient pythonClient;

    @Override
    public List<UserProfileDto> listUsers() {
        return userRepository.findAll().stream()
                .map(userServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setUserRole(Long userId, String role) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        user.setRole(SysUser.Role.valueOf(role));
        userRepository.save(user);
    }

    /**
     * 从 Python 推荐服务导入 KG4Ex 数据（112个知识点 + 1084道习题）。
     * 幂等操作：若检测到已有 pyExIndex 数据则直接返回。
     */
    @Override
    @Transactional
    public Map<String, Object> initPythonData(Long adminUserId) {
        if (exerciseRepository.existsByPyExIndexIsNotNull()) {
            log.info("Python数据已初始化，跳过");
            return Map.of("status", "already_initialized",
                    "message", "数据已经初始化过，无需重复操作");
        }

        log.info("开始从Python推荐服务导入KG4Ex数据...");

        // 1. 创建默认课程
        Course defaultCourse = new Course();
        defaultCourse.setName("Algebra 2005 数学练习");
        defaultCourse.setDescription("KG4Ex推荐模型（CIKM'23）基于Algebra 2005数据集训练，本课程包含该数据集全部1084道习题与112个知识点。");
        defaultCourse.setTeacherId(adminUserId);
        defaultCourse = courseRepository.save(defaultCourse);
        log.info("创建默认课程 id={}", defaultCourse.getId());

        // 2. 导入112个知识点
        List<PythonRecommendClient.KcItem> kcItems = pythonClient.listKnowledgeConcepts();
        Map<Integer, Long> kcIndexToMysqlId = new HashMap<>();

        for (PythonRecommendClient.KcItem kc : kcItems) {
            KnowledgePoint kp = new KnowledgePoint();
            kp.setCourseId(defaultCourse.getId());
            kp.setName(kc.kcName());
            kp.setDescription("Algebra 2005 知识点（" + kc.kcName() + "）");
            kp.setPyKcIndex(kc.kcId());
            KnowledgePoint saved = kpRepository.save(kp);
            kcIndexToMysqlId.put(kc.kcId(), saved.getId());

            // Sync to Neo4j
            KnowledgePointNode node = new KnowledgePointNode();
            node.setMysqlId(saved.getId());
            node.setName(kc.kcName());
            node.setCourseId(String.valueOf(defaultCourse.getId()));
            kpNeo4jRepository.save(node);
        }
        log.info("导入知识点 {} 个", kcItems.size());

        // 3. 导入1084道习题（含KP关联）
        List<PythonRecommendClient.ExItem> exItems = pythonClient.listExercises();
        int exCount = 0;
        for (PythonRecommendClient.ExItem ex : exItems) {
            Exercise exercise = new Exercise();
            exercise.setCourseId(defaultCourse.getId());
            exercise.setType(Exercise.Type.SHORT_ANSWER);
            exercise.setDifficulty(Exercise.Difficulty.MEDIUM);
            exercise.setContent("**[" + ex.exName() + "]** Algebra 2005 数学习题");
            exercise.setCreatorId(adminUserId);
            exercise.setPyExIndex(ex.exId());
            Exercise savedEx = exerciseRepository.save(exercise);

            for (Integer kcIdx : ex.kcIds()) {
                Long kpMysqlId = kcIndexToMysqlId.get(kcIdx);
                if (kpMysqlId != null) {
                    ExerciseKpRel rel = new ExerciseKpRel();
                    rel.setExerciseId(savedEx.getId());
                    rel.setKpId(kpMysqlId);
                    kpRelRepository.save(rel);
                }
            }
            exCount++;
        }
        log.info("导入习题 {} 道", exCount);

        return Map.of(
                "status", "success",
                "courseId", defaultCourse.getId(),
                "kcs", kcItems.size(),
                "exercises", exCount
        );
    }
}
