package com.university.cms.entity;

import lombok.Data;

import java.util.Date;

@Data
public class AuditLog {
    private Integer id;
    private Integer userId;
    private String username;
    private String action;
    private String target;
    private String detail;
    private String ip;
    private Date createdAt;
}
