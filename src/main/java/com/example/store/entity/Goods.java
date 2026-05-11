package com.example.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("goods")
public class Goods implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private Double price;

    private String pic;         // 图片URL

    private String description;

    private Integer status;

    private Integer inventory;  // 库存

    private String shopId;      // 所属商店ID

    private String categoryId;  // 所属分类ID

    private String createTime;
}