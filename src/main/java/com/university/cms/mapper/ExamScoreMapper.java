package com.university.cms.mapper;

import com.university.cms.entity.ExamScore;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ExamScoreMapper {

    @Select("<script>" +
            "SELECT e.*, s.name AS studentName, s.student_no AS studentNo, c.course_name AS courseName " +
            "FROM exam_score e " +
            "LEFT JOIN student s ON e.student_id = s.id " +
            "LEFT JOIN course c ON e.course_id = c.id WHERE 1=1 " +
            "<if test='studentId != null'> AND e.student_id = #{studentId} </if>" +
            "<if test='courseId != null'> AND e.course_id = #{courseId} </if>" +
            "<if test='clazzId != null'> AND s.clazz_id = #{clazzId} </if>" +
            "<if test='teacherId != null'> AND c.teacher_id = #{teacherId} </if>" +
            " ORDER BY e.id" +
            "</script>")
    List<ExamScore> search(@Param("studentId") Integer studentId,
                           @Param("courseId") Integer courseId,
                           @Param("clazzId") Integer clazzId,
                           @Param("teacherId") Integer teacherId);

    @Select("SELECT * FROM exam_score WHERE id = #{id}")
    ExamScore findById(Integer id);

    @Select("SELECT student_id AS studentId, score FROM exam_score WHERE course_id=#{courseId}")
    List<Map<String, Object>> scoreByCourse(Integer courseId);

    @Select("SELECT * FROM exam_score WHERE student_id=#{studentId} AND course_id=#{courseId}")
    ExamScore findOne(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Insert("INSERT INTO exam_score(student_id, course_id, score, exam_date) VALUES(#{studentId}, #{courseId}, #{score}, #{examDate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamScore score);

    @Update("UPDATE exam_score SET score=#{score}, exam_date=#{examDate} WHERE id=#{id}")
    int update(ExamScore score);

    @Delete("DELETE FROM exam_score WHERE id = #{id}")
    int deleteById(Integer id);

    @Delete("DELETE FROM exam_score WHERE course_id = #{courseId}")
    int deleteByCourse(Integer courseId);

    @Delete("DELETE FROM exam_score WHERE student_id = #{studentId}")
    int deleteByStudent(Integer studentId);
}
