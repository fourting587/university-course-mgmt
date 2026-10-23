package com.university.cms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ExperimentRecord {
    private Integer id;
    private Integer experimentId;
    private Integer studentId;
    private BigDecimal score;
    private BigDecimal completion;
    private Date submitTime;
    private String remark;

    private String studentName;
    private String studentNo;
    private String experimentTitle;
    private String courseName;
}
