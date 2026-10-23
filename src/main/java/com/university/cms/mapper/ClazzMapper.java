package com.university.cms.mapper;

import com.university.cms.entity.Clazz;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ClazzMapper {

    @Select("SELECT * FROM clazz ORDER BY id")
    List<Clazz> findAll();

    @Select("SELECT * FROM clazz WHERE id = #{id}")
    Clazz findById(Integer id);

    @Insert("INSERT INTO clazz(class_no, class_name, grade, major) VALUES(#{classNo}, #{className}, #{grade}, #{major})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Clazz clazz);

    @Update("UPDATE clazz SET class_no=#{classNo}, class_name=#{className}, grade=#{grade}, major=#{major} WHERE id=#{id}")
    int update(Clazz clazz);

    @Delete("DELETE FROM clazz WHERE id = #{id}")
    int deleteById(Integer id);
}
