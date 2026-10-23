package com.university.cms.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Student {
    private Integer id;
    private String studentNo;
    private String name;
    private String gender;
    private Integer clazzId;
    private String phone;
    private String email;
    private Date createdAt;

    /** 关联查询展示用 */
    private String className;
}
