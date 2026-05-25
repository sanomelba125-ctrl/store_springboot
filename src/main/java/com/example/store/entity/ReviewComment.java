package com.example.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 评价评论/回复表
 */
@Data
@TableName("review_comment")
public class ReviewComment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;
    private String reviewId;
    private String userId;
    private String targetUserId; // null if replying to review directly
    private String content;
    private String createTime;
}
