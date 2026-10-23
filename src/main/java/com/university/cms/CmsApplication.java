package com.university.cms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 大学智能课程管理系统启动类
 */
@SpringBootApplication
@MapperScan("com.university.cms.mapper")
public class CmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CmsApplication.class, args);
        System.out.println("========================================");
        System.out.println(" 大学智能课程管理系统已启动");
        System.out.println(" 访问地址: http://localhost:1001");
        System.out.println(" 演示账号: admin / T1001 / 202201001");
        System.out.println(" 默认密码: 123456");
        System.out.println("========================================");
    }
}
