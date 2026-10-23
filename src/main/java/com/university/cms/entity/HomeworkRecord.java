package com.university.cms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class HomeworkRecord {
    private Integer id;
    private Integer homeworkId;
    private Integer studentId;
    private BigDecimal score;
    private String status;
    private Date submitTime;
    private String remark;

    private String studentName;
    private String studentNo;
    private String homeworkTitle;
    private String courseName;
}
