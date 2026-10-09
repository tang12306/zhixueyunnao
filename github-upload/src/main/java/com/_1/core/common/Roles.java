package com._1.core.common;

/**
 * users.role 列的取值。数据库里存的是不带 ROLE_ 前缀的大写字符串。
 */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String TEACHER = "TEACHER";
    /** 学生端暂未开放，角色保留 */
    public static final String STUDENT = "STUDENT";

    private Roles() {
    }
}
