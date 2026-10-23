package com.university.cms.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Attendance {
    private Integer id;
    private Integer studentId;
    private Integer courseId;
    private Date attendDate;
    private String status;
    private String remark;
    private Integer recorderId;
    private Date createdAt;

    private String studentName;
    private String studentNo;
    private String courseName;
    private String className;
}
