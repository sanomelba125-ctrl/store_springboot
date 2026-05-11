package com.example.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户实体类
 * 对应数据库表：user
 */
@Data
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID) // 使用UUID生成策略
    private String id;

    private String username;

    private String password;

    private String salt; // 加密盐值

    private String nickname;

    private String telephone;

    private String mobile;

    private String email;

    private Integer sex; // 0=女, 1=男, 2=保密

    private Integer useful; // 1=启用, 0=禁用

    private Integer points; // 积分

    private String createTime;

    //用户类型1=超级管理员, 2=信息管理员, 3=前端用户
    private Integer type;
    // etc 扩展字段，用于携带购物车数量
    @TableField(exist = false)
    private Map<String, Object> etc = new HashMap<>();
}