package com.university.cms.mapper;

import com.university.cms.entity.Course;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CourseMapper {

    @Select("SELECT c.*, t.name AS teacherName FROM course c LEFT JOIN teacher t ON c.teacher_id = t.id ORDER BY c.id")
    List<Course> findAll();

    @Select("SELECT c.*, t.name AS teacherName FROM course c LEFT JOIN teacher t ON c.teacher_id = t.id WHERE c.id = #{id}")
    Course findById(Integer id);

    @Select("SELECT c.*, t.name AS teacherName FROM course c LEFT JOIN teacher t ON c.teacher_id = t.id " +
            "WHERE c.teacher_id = #{teacherId} ORDER BY c.id")
    List<Course> findByTeacherId(Integer teacherId);

    @Select("<script>" +
            "SELECT DISTINCT c.*, t.name AS teacherName FROM course c " +
            "LEFT JOIN teacher t ON c.teacher_id = t.id " +
            "INNER JOIN course_class cc ON cc.course_id = c.id " +
            "INNER JOIN student s ON s.clazz_id = cc.clazz_id " +
            "WHERE s.id = #{studentId} " +
            "<if test='semester != null and semester != \"\"'> AND c.semester = #{semester} </if>" +
            " ORDER BY c.id" +
            "</script>")
    List<Course> findByStudentId(@Param("studentId") Integer studentId, @Param("semester") String semester);

    @Select("<script>" +
            "SELECT c.*, t.name AS teacherName FROM course c LEFT JOIN teacher t ON c.teacher_id = t.id WHERE 1=1 " +
            "<if test='semester != null and semester != \"\"'> AND c.semester = #{semester} </if>" +
            "<if test='teacherId != null'> AND c.teacher_id = #{teacherId} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (c.course_no LIKE CONCAT('%', #{keyword}, '%') OR c.course_name LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            " ORDER BY c.id" +
            "</script>")
    List<Course> search(@Param("keyword") String keyword,
                        @Param("semester") String semester,
                        @Param("teacherId") Integer teacherId);

    @Insert("INSERT INTO course(course_no, course_name, credit, semester, teacher_id) " +
            "VALUES(#{courseNo}, #{courseName}, #{credit}, #{semester}, #{teacherId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Course course);

    @Update("UPDATE course SET course_no=#{courseNo}, course_name=#{courseName}, credit=#{credit}, " +
            "semester=#{semester}, teacher_id=#{teacherId} WHERE id=#{id}")
    int update(Course course);

    @Delete("DELETE FROM course WHERE id = #{id}")
    int deleteById(Integer id);

    @Insert("INSERT INTO course_class(course_id, clazz_id) VALUES(#{courseId}, #{clazzId})")
    int bindClass(@Param("courseId") Integer courseId, @Param("clazzId") Integer clazzId);

    @Delete("DELETE FROM course_class WHERE course_id = #{courseId}")
    int clearClasses(Integer courseId);

    @Select("SELECT clazz_id FROM course_class WHERE course_id = #{courseId}")
    List<Integer> findClazzIdsByCourse(Integer courseId);

    @Select("SELECT COUNT(*) FROM course WHERE teacher_id = #{teacherId}")
    int countByTeacher(Integer teacherId);
}
