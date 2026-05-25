package com.example.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 评价互动表（点赞、点踩）
 */
@Data
@TableName("review_interaction")
public class ReviewInteraction implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;
    private String reviewId;
    private String userId;
    private Integer type; // 1=点赞，2=点踩
    private String createTime;
}
