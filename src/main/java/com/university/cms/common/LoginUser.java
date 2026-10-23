package com.university.cms.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录会话用户信息
 */
@Data
public class LoginUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer userId;
    private String username;
    private String realName;
    private String roleCode;
    /** 学生或教师业务主键 */
    private Integer refId;

    public boolean isAdmin() {
        return "ADMIN".equals(roleCode);
    }

    public boolean isTeacher() {
        return "TEACHER".equals(roleCode);
    }

    public boolean isStudent() {
        return "STUDENT".equals(roleCode);
    }

    public boolean getAdmin() {
        return isAdmin();
    }

    public boolean getTeacher() {
        return isTeacher();
    }

    public boolean getStudent() {
        return isStudent();
    }
}
