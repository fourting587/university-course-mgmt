package com.university.cms.mapper;

import com.university.cms.entity.Attendance;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface AttendanceMapper {

    @Select("<script>" +
            "SELECT a.*, s.name AS studentName, s.student_no AS studentNo, c.course_name AS courseName, cl.class_name AS className " +
            "FROM attendance a " +
            "LEFT JOIN student s ON a.student_id = s.id " +
            "LEFT JOIN course c ON a.course_id = c.id " +
            "LEFT JOIN clazz cl ON s.clazz_id = cl.id WHERE 1=1 " +
            "<if test='studentId != null'> AND a.student_id = #{studentId} </if>" +
            "<if test='courseId != null'> AND a.course_id = #{courseId} </if>" +
            "<if test='clazzId != null'> AND s.clazz_id = #{clazzId} </if>" +
            "<if test='semester != null and semester != \"\"'> AND c.semester = #{semester} </if>" +
            "<if test='teacherId != null'> AND c.teacher_id = #{teacherId} </if>" +
            " ORDER BY a.attend_date DESC, a.id DESC" +
            "</script>")
    List<Attendance> search(@Param("studentId") Integer studentId,
                            @Param("courseId") Integer courseId,
                            @Param("clazzId") Integer clazzId,
                            @Param("semester") String semester,
                            @Param("teacherId") Integer teacherId);

    @Select("SELECT * FROM attendance WHERE id = #{id}")
    Attendance findById(Integer id);

    @Insert("INSERT INTO attendance(student_id, course_id, attend_date, status, remark, recorder_id) " +
            "VALUES(#{studentId}, #{courseId}, #{attendDate}, #{status}, #{remark}, #{recorderId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Attendance attendance);

    @Update("UPDATE attendance SET status=#{status}, remark=#{remark} WHERE id=#{id}")
    int update(Attendance attendance);

    @Delete("DELETE FROM attendance WHERE id = #{id}")
    int deleteById(Integer id);

    @Delete("DELETE FROM attendance WHERE course_id = #{courseId}")
    int deleteByCourse(Integer courseId);

    @Delete("DELETE FROM attendance WHERE student_id = #{studentId}")
    int deleteByStudent(Integer studentId);

    @Select("SELECT status, COUNT(*) AS cnt FROM attendance WHERE student_id=#{studentId} AND course_id=#{courseId} GROUP BY status")
    List<Map<String, Object>> countByStatus(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Select("SELECT student_id AS studentId, status, COUNT(*) AS cnt FROM attendance " +
            "WHERE course_id=#{courseId} GROUP BY student_id, status")
    List<Map<String, Object>> countByCourse(Integer courseId);
}
