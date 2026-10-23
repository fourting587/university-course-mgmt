package com.university.cms.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Teacher {
    private Integer id;
    private String teacherNo;
    private String name;
    private String gender;
    private String title;
    private String phone;
    private String email;
    private Date createdAt;
}
