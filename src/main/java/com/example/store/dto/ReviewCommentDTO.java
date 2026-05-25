package com.example.store.dto;

import lombok.Data;

@Data
public class ReviewCommentDTO {
    private String reviewId;
    private String content;
    private String targetUserId; // 可选
}
