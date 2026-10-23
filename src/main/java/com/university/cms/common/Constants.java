package com.university.cms.common;

public final class Constants {
    private Constants() {
    }

    public static final String SESSION_USER = "loginUser";
    /** 会话标记：本次登录仍在使用默认密码，必须先去修改密码 */
    public static final String SESSION_MUST_CHANGE = "mustChangePassword";
    /** 系统默认初始密码 */
    public static final String DEFAULT_PASSWORD = "123456";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_STUDENT = "STUDENT";

    public static final String ATT_PRESENT = "PRESENT";
    public static final String ATT_ABSENT = "ABSENT";
    public static final String ATT_LATE = "LATE";
    public static final String ATT_LEAVE = "LEAVE";
}
