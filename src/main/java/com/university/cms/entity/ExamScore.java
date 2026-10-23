package com.university.cms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ExamScore {
    private Integer id;
    private Integer studentId;
    private Integer courseId;
    private BigDecimal score;
    private Date examDate;

    private String studentName;
    private String studentNo;
    private String courseName;
}
