package com.university.cms.mapper;

import com.university.cms.entity.Teacher;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TeacherMapper {

    @Select("SELECT * FROM teacher ORDER BY id")
    List<Teacher> findAll();

    @Select("SELECT * FROM teacher WHERE id = #{id}")
    Teacher findById(Integer id);

    @Insert("INSERT INTO teacher(teacher_no, name, gender, title, phone, email) " +
            "VALUES(#{teacherNo}, #{name}, #{gender}, #{title}, #{phone}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Teacher teacher);

    @Update("UPDATE teacher SET teacher_no=#{teacherNo}, name=#{name}, gender=#{gender}, " +
            "title=#{title}, phone=#{phone}, email=#{email} WHERE id=#{id}")
    int update(Teacher teacher);

    @Delete("DELETE FROM teacher WHERE id = #{id}")
    int deleteById(Integer id);
}
