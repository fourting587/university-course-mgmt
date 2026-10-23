package com.university.cms.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EvalRule {
    private Integer id;
    private Integer courseId;
    private BigDecimal attendanceWeight;
    private BigDecimal performanceWeight;
    private BigDecimal experimentWeight;
    private BigDecimal homeworkWeight;
    private BigDecimal regularRatio;
    private BigDecimal examRatio;

    private String courseName;
}
