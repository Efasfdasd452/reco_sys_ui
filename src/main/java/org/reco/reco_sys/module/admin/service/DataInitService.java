package org.reco.reco_sys.module.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reco.reco_sys.module.exercise.entity.Exercise;
import org.reco.reco_sys.module.exercise.entity.ExerciseKpRel;
import org.reco.reco_sys.module.exercise.repository.ExerciseKpRelRepository;
import org.reco.reco_sys.module.exercise.repository.ExerciseRepository;
import org.reco.reco_sys.module.knowledge.entity.KnowledgePoint;
import org.reco.reco_sys.module.knowledge.repository.KnowledgePointRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * 将 algebra2005 数据集（Q矩阵）导入 MySQL 的服务。
 * 调用一次后数据库中就有 112 个知识点 + 1084 道习题。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataInitService {

    private final KnowledgePointRepository kpRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseKpRelRepository exerciseKpRelRepository;

    @Value("${recommend.data.path:C:/Users/ASUS/important_files/py_local/recommend_system/data/algebra2005}")
    private String dataPath;

    /**
     * 导入 algebra2005 数据集到指定课程。
     * 幂等：若 py_kc_index 已存在则跳过。
     *
     * @param courseId 目标课程 ID（由管理员/教师指定）
     * @param teacherId 创建者
     * @return 导入摘要
     */
    @Transactional
    public Map<String, Object> importAlgebra2005(Long courseId, Long teacherId) throws IOException {
        List<List<Integer>> qMatrix = loadQMatrix();
        int numKcs = qMatrix.isEmpty() ? 0 : qMatrix.get(0).size();   // 112
        int numExercises = qMatrix.size();                              // 1084

        // 1. 导入知识点（kc0 ~ kc{numKcs-1}）
        Map<Integer, Long> kcIndexToKpId = new HashMap<>();
        int kcCreated = 0;
        for (int i = 0; i < numKcs; i++) {
            var existing = kpRepository.findByPyKcIndexAndCourseId(i, courseId);
            if (existing.isPresent()) {
                kcIndexToKpId.put(i, existing.get().getId());
            } else {
                KnowledgePoint kp = new KnowledgePoint();
                kp.setCourseId(courseId);
                kp.setName("kc" + i);
                kp.setDescription("代数知识点 #" + i + "（ASSISTments algebra2005）");
                kp.setPyKcIndex(i);
                kp = kpRepository.save(kp);
                kcIndexToKpId.put(i, kp.getId());
                kcCreated++;
            }
        }
        log.info("KC 导入完成，新增 {} 个", kcCreated);

        // 2. 导入习题（ex0 ~ ex{numExercises-1}）
        int exCreated = 0;
        for (int i = 0; i < numExercises; i++) {
            List<Integer> row = qMatrix.get(i);
            // 找该题涉及的知识点
            List<Integer> involvedKcs = new ArrayList<>();
            for (int j = 0; j < row.size(); j++) {
                if (row.get(j) == 1) involvedKcs.add(j);
            }

            // 判断是否已存在
            if (exerciseRepository.existsByPyExIndexAndCourseId(i, courseId)) continue;

            // 生成有意义的占位符题目内容
            String content = buildExerciseContent(i, involvedKcs);

            Exercise ex = new Exercise();
            ex.setCourseId(courseId);
            ex.setType(Exercise.Type.SHORT_ANSWER);
            ex.setDifficulty(determineDifficulty(involvedKcs.size()));
            ex.setContent(content);
            ex.setAnswerKey("参考答案：请结合知识点 " + involvedKcs.toString() + " 作答。");
            ex.setPyExIndex(i);
            ex.setCreatorId(teacherId);
            ex = exerciseRepository.save(ex);

            // 创建习题-知识点关联
            for (int kcIdx : involvedKcs) {
                Long kpId = kcIndexToKpId.get(kcIdx);
                if (kpId != null) {
                    ExerciseKpRel rel = new ExerciseKpRel();
                    rel.setExerciseId(ex.getId());
                    rel.setKpId(kpId);
                    exerciseKpRelRepository.save(rel);
                }
            }
            exCreated++;
        }
        log.info("Exercise 导入完成，新增 {} 道", exCreated);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseId", courseId);
        result.put("totalKcs", numKcs);
        result.put("kcCreated", kcCreated);
        result.put("totalExercises", numExercises);
        result.put("exerciseCreated", exCreated);
        return result;
    }

    private List<List<Integer>> loadQMatrix() throws IOException {
        List<List<Integer>> matrix = new ArrayList<>();
        String qPath = dataPath + "/Q.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(qPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                List<Integer> row = new ArrayList<>();
                for (String p : parts) row.add(Integer.parseInt(p.trim()));
                matrix.add(row);
            }
        }
        return matrix;
    }

    private String buildExerciseContent(int exIdx, List<Integer> kcList) {
        if (kcList.isEmpty()) {
            return String.format("【代数练习 #%d】本题为综合代数练习题，请根据所学知识作答。", exIdx);
        }
        String kcStr = kcList.stream()
                .map(k -> "kc" + k)
                .reduce((a, b) -> a + "、" + b).orElse("");
        return String.format(
                "【代数练习 #%d】本题考查知识点：%s。\n" +
                "请根据以下代数问题进行分析和求解（题目编号 ex%d，来自 ASSISTments algebra2005 数据集）。",
                exIdx, kcStr, exIdx
        );
    }

    private Exercise.Difficulty determineDifficulty(int kcCount) {
        if (kcCount <= 1) return Exercise.Difficulty.EASY;
        if (kcCount <= 3) return Exercise.Difficulty.MEDIUM;
        return Exercise.Difficulty.HARD;
    }
}
