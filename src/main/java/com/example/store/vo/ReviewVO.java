package com.example.store.vo;

import lombok.Data;

@Data
public class ReviewVO {
    private String id;
    private Integer rating;
    private String content;
    private String images;
    private String createTime;
    private String nickname;    // 用户昵称（JOIN user 表）

    // 互动字段
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer commentCount;

    // 追评字段
    private String appendContent;
    private String appendImages;
    private String appendTime;

    // 商家回复字段
    private String merchantReply;
    private String merchantReplyTime;
}
