package com.example.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 购物车实体类
 * 对应表：shopcart
 */
@Data
@TableName("shopcart")
public class Shopcart implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;

    private String goodsId;

    private Integer number; // 商品数量

    private Double addPrice; // 加入时的价格

    private String createTime;
}