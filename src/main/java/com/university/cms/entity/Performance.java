package com.university.cms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Performance {
    private Integer id;
    private Integer studentId;
    private Integer courseId;
    private Date recordDate;
    private BigDecimal score;
    private String comment;
    private Integer recorderId;
    private Date createdAt;

    private String studentName;
    private String studentNo;
    private String courseName;
}
