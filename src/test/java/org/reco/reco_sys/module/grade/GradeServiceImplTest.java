package org.reco.reco_sys.module.grade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.module.exercise.entity.Exercise;
import org.reco.reco_sys.module.exercise.repository.ExerciseRepository;
import org.reco.reco_sys.module.grade.dto.GradeRequest;
import org.reco.reco_sys.module.grade.service.impl.GradeServiceImpl;
import org.reco.reco_sys.module.learning.dto.AnswerRecordDto;
import org.reco.reco_sys.module.learning.entity.AnswerRecord;
import org.reco.reco_sys.module.learning.repository.AnswerRecordRepository;
import org.reco.reco_sys.module.notification.entity.Notification;
import org.reco.reco_sys.module.notification.service.NotificationService;
import org.reco.reco_sys.module.user.entity.SysUser;
import org.reco.reco_sys.module.user.repository.SysUserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GradeServiceImpl 单元测试
 * 重点验证修复的 Bug：
 *   原代码 listPendingByCourse 调用 findByStatusIn() 忽略了 courseId 参数，
 *   修复后改为 findPendingGradingByCourse(courseId)，正确按课程过滤。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GradeServiceImpl 单元测试")
class GradeServiceImplTest {

    @Mock private AnswerRecordRepository answerRecordRepository;
    @Mock private NotificationService notificationService;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private SysUserRepository userRepository;

    @InjectMocks private GradeServiceImpl gradeService;

    // ---------------------------------------------------------------
    // 辅助工厂方法
    // ---------------------------------------------------------------

    private AnswerRecord makeRecord(Long id, Long userId, Long exerciseId, AnswerRecord.Status status) {
        AnswerRecord r = new AnswerRecord();
        r.setId(id);
        r.setUserId(userId);
        r.setExerciseId(exerciseId);
        r.setAnswer("学生答案");
        r.setStatus(status);
        r.setSubmittedAt(LocalDateTime.now());
        return r;
    }

    private Exercise makeExercise(Long id, String content, Exercise.Type type) {
        Exercise e = new Exercise();
        e.setId(id);
        e.setContent(content);
        e.setAnswerKey("参考答案");
        e.setType(type);
        e.setDifficulty(Exercise.Difficulty.MEDIUM);
        return e;
    }

    private SysUser makeUser(Long id, String username, String nickname) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(nickname);
        return u;
    }

    // ---------------------------------------------------------------
    // listPendingByCourse 测试组
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("listPendingByCourse — 按课程查询待批改列表")
    class ListPendingByCourse {

        @Test
        @DisplayName("【核心Bug修复】应调用 findPendingGradingByCourse 并传入正确 courseId")
        void shouldCallCorrectRepositoryMethodWithCourseId() {
            // 修复前：调用 findByStatusIn()，courseId 完全被忽略
            // 修复后：调用 findPendingGradingByCourse(courseId)，按课程过滤
            Long courseId = 10L;
            when(answerRecordRepository.findPendingGradingByCourse(courseId))
                    .thenReturn(List.of());

            gradeService.listPendingByCourse(courseId);

            verify(answerRecordRepository).findPendingGradingByCourse(courseId);
            // 关键断言：确保 findByStatusIn 从未被调用（旧的错误方法）
            verify(answerRecordRepository, never()).findByStatusIn(anyList());
        }

        @Test
        @DisplayName("不同 courseId 应分别查询各自的待批改记录")
        void differentCourseIdsShouldQueryIndependently() {
            // 课程1有2条待批改，课程2有0条
            AnswerRecord r1 = makeRecord(1L, 100L, 1L, AnswerRecord.Status.SUBMITTED);
            AnswerRecord r2 = makeRecord(2L, 101L, 1L, AnswerRecord.Status.GRADING);

            when(answerRecordRepository.findPendingGradingByCourse(1L)).thenReturn(List.of(r1, r2));
            when(answerRecordRepository.findPendingGradingByCourse(2L)).thenReturn(List.of());
            when(exerciseRepository.findById(1L)).thenReturn(Optional.of(makeExercise(1L, "题目内容", Exercise.Type.SHORT_ANSWER)));
            when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

            List<AnswerRecordDto> course1Result = gradeService.listPendingByCourse(1L);
            List<AnswerRecordDto> course2Result = gradeService.listPendingByCourse(2L);

            assertThat(course1Result).hasSize(2);
            assertThat(course2Result).isEmpty();
        }

        @Test
        @DisplayName("返回结果应包含题目内容（修复后 toDto 补充了 exerciseContent）")
        void shouldFillExerciseContentInDto() {
            Long courseId = 1L;
            AnswerRecord record = makeRecord(1L, 100L, 5L, AnswerRecord.Status.SUBMITTED);
            Exercise exercise = makeExercise(5L, "求x²+2x+1=0的根", Exercise.Type.SHORT_ANSWER);
            SysUser student = makeUser(100L, "student01", "张三");

            when(answerRecordRepository.findPendingGradingByCourse(courseId)).thenReturn(List.of(record));
            when(exerciseRepository.findById(5L)).thenReturn(Optional.of(exercise));
            when(userRepository.findById(100L)).thenReturn(Optional.of(student));

            List<AnswerRecordDto> result = gradeService.listPendingByCourse(courseId);

            assertThat(result).hasSize(1);
            AnswerRecordDto dto = result.get(0);
            assertThat(dto.getExerciseContent()).isEqualTo("求x²+2x+1=0的根");
            assertThat(dto.getExerciseAnswerKey()).isEqualTo("参考答案");
            assertThat(dto.getStudentName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("当学生没有 nickname 时应 fallback 到 username")
        void shouldFallbackToUsernameWhenNicknameIsNull() {
            Long courseId = 1L;
            AnswerRecord record = makeRecord(1L, 200L, 1L, AnswerRecord.Status.SUBMITTED);
            SysUser student = makeUser(200L, "user_no_nickname", null); // nickname 为 null

            when(answerRecordRepository.findPendingGradingByCourse(courseId)).thenReturn(List.of(record));
            when(exerciseRepository.findById(1L)).thenReturn(Optional.empty());
            when(userRepository.findById(200L)).thenReturn(Optional.of(student));

            List<AnswerRecordDto> result = gradeService.listPendingByCourse(courseId);

            assertThat(result.get(0).getStudentName()).isEqualTo("user_no_nickname");
        }

        @Test
        @DisplayName("课程下无待批改记录时应返回空列表")
        void shouldReturnEmptyListWhenNoPendingRecords() {
            when(answerRecordRepository.findPendingGradingByCourse(99L)).thenReturn(List.of());

            List<AnswerRecordDto> result = gradeService.listPendingByCourse(99L);

            assertThat(result).isEmpty();
        }
    }

    // ---------------------------------------------------------------
    // grade 测试组
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("grade — 教师批改")
    class Grade {

        @Test
        @DisplayName("批改成功后状态应变为 GRADED")
        void shouldSetStatusToGraded() {
            AnswerRecord record = makeRecord(1L, 100L, 1L, AnswerRecord.Status.SUBMITTED);
            GradeRequest request = new GradeRequest();
            request.setScore(85);
            request.setTeacherComment("解题过程清晰");

            when(answerRecordRepository.findById(1L)).thenReturn(Optional.of(record));
            when(answerRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(exerciseRepository.findById(1L)).thenReturn(Optional.empty());
            when(userRepository.findById(100L)).thenReturn(Optional.empty());

            AnswerRecordDto result = gradeService.grade(1L, request, 999L);

            assertThat(result.getStatus()).isEqualTo("GRADED");
            assertThat(result.getScore()).isEqualTo(85);
            assertThat(result.getTeacherComment()).isEqualTo("解题过程清晰");
        }

        @Test
        @DisplayName("批改后应发送通知给学生")
        void shouldSendNotificationToStudent() {
            AnswerRecord record = makeRecord(1L, 100L, 1L, AnswerRecord.Status.SUBMITTED);
            GradeRequest request = new GradeRequest();
            request.setScore(90);
            request.setTeacherComment("优秀");

            when(answerRecordRepository.findById(1L)).thenReturn(Optional.of(record));
            when(answerRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(exerciseRepository.findById(anyLong())).thenReturn(Optional.empty());
            when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

            gradeService.grade(1L, request, 999L);

            ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Notification.Type> typeCaptor = ArgumentCaptor.forClass(Notification.Type.class);
            verify(notificationService).send(
                    userIdCaptor.capture(),
                    typeCaptor.capture(),
                    anyString(),
                    contains("90"), // 通知内容应包含分数
                    eq(1L)
            );
            assertThat(userIdCaptor.getValue()).isEqualTo(100L); // 通知发给学生
            assertThat(typeCaptor.getValue()).isEqualTo(Notification.Type.GRADE_DONE);
        }

        @Test
        @DisplayName("记录不存在时应抛出 BusinessException")
        void shouldThrowWhenRecordNotFound() {
            when(answerRecordRepository.findById(999L)).thenReturn(Optional.empty());
            GradeRequest request = new GradeRequest();
            request.setScore(70);

            assertThatThrownBy(() -> gradeService.grade(999L, request, 1L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("批改时应记录 gradedBy 教师 ID")
        void shouldRecordTeacherIdWhenGrading() {
            AnswerRecord record = makeRecord(1L, 100L, 1L, AnswerRecord.Status.SUBMITTED);
            GradeRequest request = new GradeRequest();
            request.setScore(60);

            when(answerRecordRepository.findById(1L)).thenReturn(Optional.of(record));
            when(answerRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(exerciseRepository.findById(anyLong())).thenReturn(Optional.empty());
            when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

            gradeService.grade(1L, request, 888L);

            ArgumentCaptor<AnswerRecord> captor = ArgumentCaptor.forClass(AnswerRecord.class);
            verify(answerRecordRepository).save(captor.capture());
            assertThat(captor.getValue().getGradedBy()).isEqualTo(888L);
        }
    }
}
