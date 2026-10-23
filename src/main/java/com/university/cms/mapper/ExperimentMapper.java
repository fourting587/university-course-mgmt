package com.university.cms.mapper;

import com.university.cms.entity.Experiment;
import com.university.cms.entity.ExperimentRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ExperimentMapper {

    @Select("SELECT e.*, c.course_name AS courseName FROM experiment e LEFT JOIN course c ON e.course_id = c.id " +
            "WHERE e.course_id = #{courseId} ORDER BY e.id")
    List<Experiment> findByCourseId(Integer courseId);

    @Select("SELECT e.*, c.course_name AS courseName FROM experiment e LEFT JOIN course c ON e.course_id = c.id ORDER BY e.id")
    List<Experiment> findAll();

    @Select("SELECT * FROM experiment WHERE id = #{id}")
    Experiment findById(Integer id);

    @Insert("INSERT INTO experiment(course_id, title, full_score, due_date) VALUES(#{courseId}, #{title}, #{fullScore}, #{dueDate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Experiment experiment);

    @Update("UPDATE experiment SET title=#{title}, full_score=#{fullScore}, due_date=#{dueDate} WHERE id=#{id}")
    int update(Experiment experiment);

    @Delete("DELETE FROM experiment WHERE id = #{id}")
    int deleteById(Integer id);

    @Select("<script>" +
            "SELECT r.*, s.name AS studentName, s.student_no AS studentNo, e.title AS experimentTitle, c.course_name AS courseName " +
            "FROM experiment_record r " +
            "LEFT JOIN experiment e ON r.experiment_id = e.id " +
            "LEFT JOIN student s ON r.student_id = s.id " +
            "LEFT JOIN course c ON e.course_id = c.id WHERE 1=1 " +
            "<if test='studentId != null'> AND r.student_id = #{studentId} </if>" +
            "<if test='courseId != null'> AND e.course_id = #{courseId} </if>" +
            "<if test='experimentId != null'> AND r.experiment_id = #{experimentId} </if>" +
            "<if test='teacherId != null'> AND c.teacher_id = #{teacherId} </if>" +
            " ORDER BY r.id DESC" +
            "</script>")
    List<ExperimentRecord> searchRecords(@Param("studentId") Integer studentId,
                                         @Param("courseId") Integer courseId,
                                         @Param("experimentId") Integer experimentId,
                                         @Param("teacherId") Integer teacherId);

    @Insert("INSERT INTO experiment_record(experiment_id, student_id, score, completion, submit_time, remark) " +
            "VALUES(#{experimentId}, #{studentId}, #{score}, #{completion}, #{submitTime}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRecord(ExperimentRecord record);

    @Update("UPDATE experiment_record SET score=#{score}, completion=#{completion}, submit_time=#{submitTime}, remark=#{remark} WHERE id=#{id}")
    int updateRecord(ExperimentRecord record);

    @Delete("DELETE FROM experiment_record WHERE id = #{id}")
    int deleteRecord(Integer id);

    @Delete("DELETE FROM experiment_record WHERE experiment_id IN (SELECT id FROM experiment WHERE course_id = #{courseId})")
    int deleteRecordsByCourse(Integer courseId);

    @Delete("DELETE FROM experiment_record WHERE student_id = #{studentId}")
    int deleteRecordsByStudent(Integer studentId);

    @Delete("DELETE FROM experiment WHERE course_id = #{courseId}")
    int deleteByCourse(Integer courseId);

    @Select("SELECT e.course_id FROM experiment_record r JOIN experiment e ON r.experiment_id = e.id WHERE r.id = #{recordId}")
    Integer findCourseIdByRecordId(Integer recordId);

    @Select("SELECT e.*, c.course_name AS courseName FROM experiment e " +
            "JOIN course c ON e.course_id = c.id WHERE c.teacher_id = #{teacherId} ORDER BY e.id")
    List<Experiment> findByTeacherId(Integer teacherId);

    @Select("SELECT AVG(COALESCE(score,0)) FROM experiment_record r " +
            "INNER JOIN experiment e ON r.experiment_id = e.id " +
            "WHERE r.student_id=#{studentId} AND e.course_id=#{courseId}")
    Double avgScore(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Select("SELECT r.student_id AS studentId, AVG(COALESCE(r.score,0)) AS avgScore " +
            "FROM experiment_record r INNER JOIN experiment e ON r.experiment_id = e.id " +
            "WHERE e.course_id=#{courseId} GROUP BY r.student_id")
    List<Map<String, Object>> avgByCourse(Integer courseId);
}
