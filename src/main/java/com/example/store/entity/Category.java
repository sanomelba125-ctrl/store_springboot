package com.example.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
@TableName("category")
public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    private String name;
    private String parentId;
    private Integer isLeaf;
    private Integer level;
    private Integer sort;
    private Integer isDeleted;
    private String createTime;

    // 这是一个非数据库字段，用于前端展示树形结构（子分类）
    @TableField(exist = false)
    private List<Category> children;
}