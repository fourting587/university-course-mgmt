package com.university.cms.mapper;

import com.university.cms.entity.Student;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface StudentMapper {

    @Select("SELECT s.*, c.class_name AS className FROM student s LEFT JOIN clazz c ON s.clazz_id = c.id ORDER BY s.id")
    List<Student> findAll();

    @Select("SELECT s.*, c.class_name AS className FROM student s LEFT JOIN clazz c ON s.clazz_id = c.id WHERE s.id = #{id}")
    Student findById(Integer id);

    @Select("SELECT s.*, c.class_name AS className FROM student s LEFT JOIN clazz c ON s.clazz_id = c.id " +
            "WHERE s.clazz_id = #{clazzId} ORDER BY s.student_no")
    List<Student> findByClazzId(Integer clazzId);

    @Select("SELECT DISTINCT s.*, cl.class_name AS className FROM student s " +
            "JOIN clazz cl ON s.clazz_id = cl.id " +
            "JOIN course_class cc ON cc.clazz_id = cl.id " +
            "JOIN course co ON co.id = cc.course_id " +
            "WHERE co.teacher_id = #{teacherId} ORDER BY s.student_no")
    List<Student> findByTeacherId(Integer teacherId);

    @Select("<script>" +
            "SELECT s.*, c.class_name AS className FROM student s LEFT JOIN clazz c ON s.clazz_id = c.id WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND (s.student_no LIKE CONCAT('%', #{keyword}, '%') OR s.name LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='clazzId != null'> AND s.clazz_id = #{clazzId} </if>" +
            " ORDER BY s.id" +
            "</script>")
    List<Student> search(@Param("keyword") String keyword, @Param("clazzId") Integer clazzId);

    @Insert("INSERT INTO student(student_no, name, gender, clazz_id, phone, email) " +
            "VALUES(#{studentNo}, #{name}, #{gender}, #{clazzId}, #{phone}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Student student);

    @Update("UPDATE student SET student_no=#{studentNo}, name=#{name}, gender=#{gender}, " +
            "clazz_id=#{clazzId}, phone=#{phone}, email=#{email} WHERE id=#{id}")
    int update(Student student);

    @Delete("DELETE FROM student WHERE id = #{id}")
    int deleteById(Integer id);

    @Select("SELECT COUNT(*) FROM student WHERE clazz_id = #{clazzId}")
    int countByClazz(Integer clazzId);
}
