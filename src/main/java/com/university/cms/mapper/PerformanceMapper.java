package com.university.cms.mapper;

import com.university.cms.entity.Performance;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface PerformanceMapper {

    @Select("<script>" +
            "SELECT p.*, s.name AS studentName, s.student_no AS studentNo, c.course_name AS courseName " +
            "FROM performance p " +
            "LEFT JOIN student s ON p.student_id = s.id " +
            "LEFT JOIN course c ON p.course_id = c.id WHERE 1=1 " +
            "<if test='studentId != null'> AND p.student_id = #{studentId} </if>" +
            "<if test='courseId != null'> AND p.course_id = #{courseId} </if>" +
            "<if test='clazzId != null'> AND s.clazz_id = #{clazzId} </if>" +
            "<if test='teacherId != null'> AND c.teacher_id = #{teacherId} </if>" +
            " ORDER BY p.record_date DESC" +
            "</script>")
    List<Performance> search(@Param("studentId") Integer studentId,
                             @Param("courseId") Integer courseId,
                             @Param("clazzId") Integer clazzId,
                             @Param("teacherId") Integer teacherId);

    @Insert("INSERT INTO performance(student_id, course_id, record_date, score, comment, recorder_id) " +
            "VALUES(#{studentId}, #{courseId}, #{recordDate}, #{score}, #{comment}, #{recorderId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Performance performance);

    @Update("UPDATE performance SET score=#{score}, comment=#{comment}, record_date=#{recordDate} WHERE id=#{id}")
    int update(Performance performance);

    @Delete("DELETE FROM performance WHERE id = #{id}")
    int deleteById(Integer id);

    @Select("SELECT * FROM performance WHERE id = #{id}")
    Performance findById(Integer id);

    @Delete("DELETE FROM performance WHERE course_id = #{courseId}")
    int deleteByCourse(Integer courseId);

    @Delete("DELETE FROM performance WHERE student_id = #{studentId}")
    int deleteByStudent(Integer studentId);

    @Select("SELECT AVG(score) FROM performance WHERE student_id=#{studentId} AND course_id=#{courseId}")
    Double avgScore(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Select("SELECT student_id AS studentId, AVG(score) AS avgScore FROM performance " +
            "WHERE course_id=#{courseId} GROUP BY student_id")
    List<Map<String, Object>> avgByCourse(Integer courseId);
}
