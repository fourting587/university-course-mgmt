package com.university.cms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Experiment {
    private Integer id;
    private Integer courseId;
    private String title;
    private BigDecimal fullScore;
    private Date dueDate;
    private Date createdAt;

    private String courseName;
}
