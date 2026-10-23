package com.university.cms.mapper;

import com.university.cms.entity.AuditLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AuditMapper {

    @Insert("INSERT INTO audit_log(user_id, username, action, target, detail, ip) " +
            "VALUES(#{userId}, #{username}, #{action}, #{target}, #{detail}, #{ip})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AuditLog log);

    @Select("SELECT * FROM audit_log ORDER BY id DESC LIMIT #{offset}, #{size}")
    List<AuditLog> findPage(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM audit_log")
    int count();

    @Delete("DELETE FROM audit_log")
    int clear();
}
