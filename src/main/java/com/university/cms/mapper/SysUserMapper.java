package com.university.cms.mapper;

import com.university.cms.entity.SysUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysUserMapper {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser findByUsername(String username);

    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    SysUser findById(Integer id);

    @Select("SELECT * FROM sys_user ORDER BY id")
    List<SysUser> findAll();

    @Insert("INSERT INTO sys_user(username, password, role_code, ref_id, real_name, enabled) " +
            "VALUES(#{username}, #{password}, #{roleCode}, #{refId}, #{realName}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    @Update("UPDATE sys_user SET password=#{password}, real_name=#{realName}, enabled=#{enabled} WHERE id=#{id}")
    int update(SysUser user);

    @Delete("DELETE FROM sys_user WHERE id = #{id}")
    int deleteById(Integer id);

    @Update("UPDATE sys_user SET real_name=#{realName} WHERE role_code=#{roleCode} AND ref_id=#{refId}")
    int updateRealNameByRef(@Param("roleCode") String roleCode,
                            @Param("refId") Integer refId,
                            @Param("realName") String realName);

    @Delete("DELETE FROM sys_user WHERE role_code=#{roleCode} AND ref_id=#{refId}")
    int deleteByRef(@Param("roleCode") String roleCode, @Param("refId") Integer refId);

    @Select("SELECT * FROM sys_user WHERE role_code=#{roleCode} AND ref_id=#{refId}")
    SysUser findByRef(@Param("roleCode") String roleCode, @Param("refId") Integer refId);

    @Update("UPDATE sys_user SET enabled=#{enabled} WHERE id=#{id}")
    int updateEnabled(@Param("id") Integer id, @Param("enabled") boolean enabled);
}
