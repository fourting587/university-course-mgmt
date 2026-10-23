package com.university.cms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class Course {
    private Integer id;
    private String courseNo;
    private String courseName;
    private BigDecimal credit;
    private String semester;
    private Integer teacherId;
    private Date createdAt;

    private String teacherName;
    /** 该课程绑定的班级 id 列表（编辑回显用） */
    private List<Integer> clazzIds;
}
