package org.reco.reco_sys.module.classroom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.module.classroom.dto.ClassroomCreateRequest;
import org.reco.reco_sys.module.classroom.dto.ClassroomDto;
import org.reco.reco_sys.module.classroom.entity.Classroom;
import org.reco.reco_sys.module.classroom.entity.ClassroomStudent;
import org.reco.reco_sys.module.classroom.repository.ClassroomRepository;
import org.reco.reco_sys.module.classroom.repository.ClassroomStudentRepository;
import org.reco.reco_sys.module.classroom.service.impl.ClassroomServiceImpl;
import org.reco.reco_sys.module.user.entity.SysUser;
import org.reco.reco_sys.module.user.repository.SysUserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ClassroomServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClassroomServiceImpl 单元测试")
class ClassroomServiceImplTest {

    @Mock private ClassroomRepository classroomRepository;
    @Mock private ClassroomStudentRepository classroomStudentRepository;
    @Mock private SysUserRepository userRepository;

    @InjectMocks private ClassroomServiceImpl classroomService;

    // ---------------------------------------------------------------
    // 辅助方法
    // ---------------------------------------------------------------

    private Classroom makeClassroom(Long id, String name, Long teacherId, boolean active) {
        Classroom c = new Classroom();
        c.setId(id);
        c.setName(name);
        c.setTeacherId(teacherId);
        c.setInviteCode("ABCD1234");
        c.setIsActive(active);
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }

    private ClassroomStudent makeEnrollment(Long id, Long classroomId, Long userId) {
        ClassroomStudent cs = new ClassroomStudent();
        cs.setId(id);
        cs.setClassroomId(classroomId);
        cs.setUserId(userId);
        cs.setJoinedAt(LocalDateTime.now());
        return cs;
    }

    private SysUser makeUser(Long id, String username, String nickname) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(nickname);
        return u;
    }

    // ---------------------------------------------------------------
    // create 测试组
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("create — 教师创建班级")
    class Create {

        @Test
        @DisplayName("创建班级时应自动生成邀请码")
        void shouldGenerateInviteCode() {
            ClassroomCreateRequest req = new ClassroomCreateRequest();
            req.setName("软件工程2班");
            req.setDescription("2024级");

            Classroom saved = makeClassroom(1L, "软件工程2班", 10L, true);
            when(classroomRepository.findByInviteCode(anyString())).thenReturn(Optional.empty());
            when(classroomRepository.save(any())).thenReturn(saved);
            when(userRepository.findById(10L)).thenReturn(Optional.empty());

            ClassroomDto dto = classroomService.create(req, 10L);

            assertThat(dto.getId()).isEqualTo(1L);
            // 验证 save 被调用，且传入的 Classroom 有 inviteCode
            ArgumentCaptor<Classroom> captor = ArgumentCaptor.forClass(Classroom.class);
            verify(classroomRepository).save(captor.capture());
            assertThat(captor.getValue().getInviteCode()).isNotBlank();
            assertThat(captor.getValue().getInviteCode()).hasSize(8); // 8位大写字母数字
        }

        @Test
        @DisplayName("创建班级时应正确绑定 teacherId")
        void shouldBindTeacherId() {
            ClassroomCreateRequest req = new ClassroomCreateRequest();
            req.setName("测试班级");

            Classroom saved = makeClassroom(1L, "测试班级", 99L, true);
            when(classroomRepository.findByInviteCode(anyString())).thenReturn(Optional.empty());
            when(classroomRepository.save(any())).thenReturn(saved);
            when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

            classroomService.create(req, 99L);

            ArgumentCaptor<Classroom> captor = ArgumentCaptor.forClass(Classroom.class);
            verify(classroomRepository).save(captor.capture());
            assertThat(captor.getValue().getTeacherId()).isEqualTo(99L);
        }
    }

    // ---------------------------------------------------------------
    // joinByCode 测试组
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("joinByCode — 学生通过邀请码加入班级")
    class JoinByCode {

        @Test
        @DisplayName("有效邀请码应成功加入班级")
        void shouldJoinWithValidCode() {
            Classroom classroom = makeClassroom(1L, "AI班", 10L, true);
            when(classroomRepository.findByInviteCode("ABCD1234")).thenReturn(Optional.of(classroom));
            when(classroomStudentRepository.existsByClassroomIdAndUserId(1L, 200L)).thenReturn(false);
            when(classroomStudentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(10L)).thenReturn(Optional.empty());

            ClassroomDto result = classroomService.joinByCode("ABCD1234", 200L);

            assertThat(result.getId()).isEqualTo(1L);
            ArgumentCaptor<ClassroomStudent> captor = ArgumentCaptor.forClass(ClassroomStudent.class);
            verify(classroomStudentRepository).save(captor.capture());
            assertThat(captor.getValue().getClassroomId()).isEqualTo(1L);
            assertThat(captor.getValue().getUserId()).isEqualTo(200L);
        }

        @Test
        @DisplayName("无效邀请码应抛出 BusinessException")
        void shouldThrowForInvalidCode() {
            when(classroomRepository.findByInviteCode("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> classroomService.joinByCode("INVALID", 200L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("已解散的班级不能加入")
        void shouldRejectDisbandedClassroom() {
            Classroom dissolved = makeClassroom(1L, "已解散班级", 10L, false); // isActive = false
            when(classroomRepository.findByInviteCode("ABCD1234")).thenReturn(Optional.of(dissolved));

            assertThatThrownBy(() -> classroomService.joinByCode("ABCD1234", 200L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("解散");
        }

        @Test
        @DisplayName("重复加入同一班级应抛出 BusinessException")
        void shouldRejectDuplicateJoin() {
            Classroom classroom = makeClassroom(1L, "班级", 10L, true);
            when(classroomRepository.findByInviteCode("ABCD1234")).thenReturn(Optional.of(classroom));
            when(classroomStudentRepository.existsByClassroomIdAndUserId(1L, 200L)).thenReturn(true); // 已加入

            assertThatThrownBy(() -> classroomService.joinByCode("ABCD1234", 200L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已在");
        }
    }

    // ---------------------------------------------------------------
    // leave 测试组
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("leave — 学生退出班级")
    class Leave {

        @Test
        @DisplayName("已加入的学生应可以成功退出")
        void shouldLeaveSuccessfully() {
            ClassroomStudent cs = makeEnrollment(1L, 1L, 200L);
            when(classroomStudentRepository.findByClassroomIdAndUserId(1L, 200L)).thenReturn(Optional.of(cs));

            classroomService.leave(1L, 200L);

            verify(classroomStudentRepository).delete(cs);
        }

        @Test
        @DisplayName("未加入的学生退出应抛出 BusinessException")
        void shouldThrowIfNotMember() {
            when(classroomStudentRepository.findByClassroomIdAndUserId(1L, 300L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> classroomService.leave(1L, 300L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ---------------------------------------------------------------
    // removeStudent 测试组
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("removeStudent — 教师移除学生")
    class RemoveStudent {

        @Test
        @DisplayName("班级所属教师可以移除学生")
        void teacherShouldRemoveStudent() {
            Classroom classroom = makeClassroom(1L, "班级", 10L, true);
            ClassroomStudent cs = makeEnrollment(1L, 1L, 200L);
            when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
            when(classroomStudentRepository.findByClassroomIdAndUserId(1L, 200L)).thenReturn(Optional.of(cs));

            classroomService.removeStudent(1L, 200L, 10L); // 教师ID=10

            verify(classroomStudentRepository).delete(cs);
        }

        @Test
        @DisplayName("非班级教师尝试移除学生应抛出 BusinessException")
        void nonTeacherShouldNotRemoveStudent() {
            Classroom classroom = makeClassroom(1L, "班级", 10L, true);
            when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));

            assertThatThrownBy(() -> classroomService.removeStudent(1L, 200L, 999L)) // 不是该班级的教师
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权");
        }
    }

    // ---------------------------------------------------------------
    // dissolve 测试组
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("dissolve — 教师解散班级")
    class Dissolve {

        @Test
        @DisplayName("解散后班级 isActive 应变为 false")
        void shouldSetInactiveFlagOnDissolve() {
            Classroom classroom = makeClassroom(1L, "待解散", 10L, true);
            when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
            when(classroomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            classroomService.dissolve(1L, 10L);

            ArgumentCaptor<Classroom> captor = ArgumentCaptor.forClass(Classroom.class);
            verify(classroomRepository).save(captor.capture());
            assertThat(captor.getValue().getIsActive()).isFalse();
        }

        @Test
        @DisplayName("非班级教师解散班级应抛出 BusinessException")
        void nonTeacherShouldNotDissolve() {
            Classroom classroom = makeClassroom(1L, "班级", 10L, true);
            when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));

            assertThatThrownBy(() -> classroomService.dissolve(1L, 888L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权");
        }
    }

    // ---------------------------------------------------------------
    // myClassrooms / myJoinedClassrooms 测试组
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("查询班级列表")
    class ListClassrooms {

        @Test
        @DisplayName("教师应只能查到自己的班级")
        void teacherShouldSeeOnlyOwnClassrooms() {
            Long teacherId = 10L;
            Classroom c1 = makeClassroom(1L, "班级A", teacherId, true);
            Classroom c2 = makeClassroom(2L, "班级B", teacherId, true);
            when(classroomRepository.findByTeacherIdAndIsActiveTrue(teacherId)).thenReturn(List.of(c1, c2));
            when(classroomStudentRepository.findByClassroomId(anyLong())).thenReturn(List.of());
            when(userRepository.findById(teacherId)).thenReturn(Optional.empty());

            List<ClassroomDto> result = classroomService.myClassrooms(teacherId);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ClassroomDto::getName)
                    .containsExactlyInAnyOrder("班级A", "班级B");
        }

        @Test
        @DisplayName("学生查看已加入班级：已解散的班级应被过滤")
        void studentShouldNotSeeDisbandedClassrooms() {
            Long studentId = 200L;
            Long activeId = 1L, dissolvedId = 2L;

            when(classroomStudentRepository.findClassroomIdsByUserId(studentId))
                    .thenReturn(List.of(activeId, dissolvedId));
            Classroom active = makeClassroom(activeId, "活跃班级", 10L, true);
            Classroom dissolved = makeClassroom(dissolvedId, "已解散班级", 10L, false);
            when(classroomRepository.findAllById(List.of(activeId, dissolvedId)))
                    .thenReturn(List.of(active, dissolved));
            when(classroomStudentRepository.findByClassroomId(anyLong())).thenReturn(List.of());
            when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

            List<ClassroomDto> result = classroomService.myJoinedClassrooms(studentId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("活跃班级");
        }
    }
}
