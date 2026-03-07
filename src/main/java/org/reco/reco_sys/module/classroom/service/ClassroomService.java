package org.reco.reco_sys.module.classroom.service;

import org.reco.reco_sys.module.classroom.dto.ClassroomCreateRequest;
import org.reco.reco_sys.module.classroom.dto.ClassroomDto;

import java.util.List;

public interface ClassroomService {
    /** 教师创建班级 */
    ClassroomDto create(ClassroomCreateRequest request, Long teacherId);

    /** 教师查看自己的所有班级 */
    List<ClassroomDto> myClassrooms(Long teacherId);

    /** 获取班级详情（含学生和课程列表） */
    ClassroomDto getDetail(Long classroomId, Long currentUserId);

    /** 学生通过邀请码加入班级 */
    ClassroomDto joinByCode(String inviteCode, Long studentId);

    /** 学生退出班级 */
    void leave(Long classroomId, Long studentId);

    /** 教师/管理员移除学生 */
    void removeStudent(Long classroomId, Long studentId, Long currentUserId);

    /** 教师/管理员解散班级 */
    void dissolve(Long classroomId, Long currentUserId);

    /** 学生查看自己加入的班级 */
    List<ClassroomDto> myJoinedClassrooms(Long studentId);

    /** 管理员查看所有班级 */
    List<ClassroomDto> allClassrooms();

    /** 教师/管理员：将课程加入班级 */
    void addCourseToClassroom(Long classroomId, Long courseId, Long currentUserId);

    /** 教师/管理员：从班级移除课程 */
    void removeCourseFromClassroom(Long classroomId, Long courseId, Long currentUserId);

    /** 教师/管理员：批量将班级学生加入课程（studentIds为空则全部加入） */
    void enrollStudentsInCourse(Long classroomId, Long courseId, List<Long> studentIds, Long currentUserId);

    /** 教师/管理员：批量将班级学生移出课程（studentIds为空则全部移出） */
    void unenrollStudentsFromCourse(Long classroomId, Long courseId, List<Long> studentIds, Long currentUserId);
}
