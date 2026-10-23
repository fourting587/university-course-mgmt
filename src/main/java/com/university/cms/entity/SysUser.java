package com.university.cms.entity;

import lombok.Data;

import java.util.Date;

@Data
public class SysUser {
    private Integer id;
    private String username;
    private String password;
    private String roleCode;
    private Integer refId;
    private String realName;
    private Boolean enabled;
    private Date createdAt;
}
