package com.university.cms.mapper;

import com.university.cms.entity.Homework;
import com.university.cms.entity.HomeworkRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface HomeworkMapper {

    @Select("SELECT h.*, c.course_name AS courseName FROM homework h LEFT JOIN course c ON h.course_id = c.id " +
            "WHERE h.course_id = #{courseId} ORDER BY h.id")
    List<Homework> findByCourseId(Integer courseId);

    @Select("SELECT h.*, c.course_name AS courseName FROM homework h LEFT JOIN course c ON h.course_id = c.id ORDER BY h.id")
    List<Homework> findAll();

    @Select("SELECT * FROM homework WHERE id = #{id}")
    Homework findById(Integer id);

    @Insert("INSERT INTO homework(course_id, title, full_score, due_date) VALUES(#{courseId}, #{title}, #{fullScore}, #{dueDate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Homework homework);

    @Update("UPDATE homework SET title=#{title}, full_score=#{fullScore}, due_date=#{dueDate} WHERE id=#{id}")
    int update(Homework homework);

    @Delete("DELETE FROM homework WHERE id = #{id}")
    int deleteById(Integer id);

    @Select("<script>" +
            "SELECT r.*, s.name AS studentName, s.student_no AS studentNo, h.title AS homeworkTitle, c.course_name AS courseName " +
            "FROM homework_record r " +
            "LEFT JOIN homework h ON r.homework_id = h.id " +
            "LEFT JOIN student s ON r.student_id = s.id " +
            "LEFT JOIN course c ON h.course_id = c.id WHERE 1=1 " +
            "<if test='studentId != null'> AND r.student_id = #{studentId} </if>" +
            "<if test='courseId != null'> AND h.course_id = #{courseId} </if>" +
            "<if test='homeworkId != null'> AND r.homework_id = #{homeworkId} </if>" +
            "<if test='teacherId != null'> AND c.teacher_id = #{teacherId} </if>" +
            " ORDER BY r.id DESC" +
            "</script>")
    List<HomeworkRecord> searchRecords(@Param("studentId") Integer studentId,
                                       @Param("courseId") Integer courseId,
                                       @Param("homeworkId") Integer homeworkId,
                                       @Param("teacherId") Integer teacherId);

    @Insert("INSERT INTO homework_record(homework_id, student_id, score, status, submit_time, remark) " +
            "VALUES(#{homeworkId}, #{studentId}, #{score}, #{status}, #{submitTime}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRecord(HomeworkRecord record);

    @Select("SELECT * FROM homework_record WHERE homework_id=#{homeworkId} AND student_id=#{studentId}")
    HomeworkRecord findRecord(@Param("homeworkId") Integer homeworkId, @Param("studentId") Integer studentId);

    @Update("UPDATE homework_record SET score=#{score}, status=#{status}, submit_time=#{submitTime}, remark=#{remark} WHERE id=#{id}")
    int updateRecord(HomeworkRecord record);

    @Delete("DELETE FROM homework_record WHERE id = #{id}")
    int deleteRecord(Integer id);

    @Delete("DELETE FROM homework_record WHERE homework_id IN (SELECT id FROM homework WHERE course_id = #{courseId})")
    int deleteRecordsByCourse(Integer courseId);

    @Delete("DELETE FROM homework_record WHERE student_id = #{studentId}")
    int deleteRecordsByStudent(Integer studentId);

    @Delete("DELETE FROM homework WHERE course_id = #{courseId}")
    int deleteByCourse(Integer courseId);

    @Select("SELECT h.course_id FROM homework_record r JOIN homework h ON r.homework_id = h.id WHERE r.id = #{recordId}")
    Integer findCourseIdByRecordId(Integer recordId);

    @Select("SELECT h.*, c.course_name AS courseName FROM homework h " +
            "JOIN course c ON h.course_id = c.id WHERE c.teacher_id = #{teacherId} ORDER BY h.id")
    List<Homework> findByTeacherId(Integer teacherId);

    @Select("SELECT AVG(COALESCE(score,0)) FROM homework_record r " +
            "INNER JOIN homework h ON r.homework_id = h.id " +
            "WHERE r.student_id=#{studentId} AND h.course_id=#{courseId} AND r.score IS NOT NULL")
    Double avgScore(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Select("SELECT r.student_id AS studentId, AVG(COALESCE(r.score,0)) AS avgScore " +
            "FROM homework_record r INNER JOIN homework h ON r.homework_id = h.id " +
            "WHERE h.course_id=#{courseId} AND r.score IS NOT NULL GROUP BY r.student_id")
    List<Map<String, Object>> avgByCourse(Integer courseId);
}
