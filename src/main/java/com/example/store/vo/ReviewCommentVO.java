package com.example.store.vo;

import lombok.Data;

@Data
public class ReviewCommentVO {
    private String id;
    private String reviewId;
    private String userId;
    private String nickname;
    private String targetUserId;
    private String targetNickname;
    private String content;
    private String createTime;
}
