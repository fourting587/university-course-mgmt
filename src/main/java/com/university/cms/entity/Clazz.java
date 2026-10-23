package com.university.cms.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Clazz {
    private Integer id;
    private String classNo;
    private String className;
    private String grade;
    private String major;
    private Date createdAt;
}
