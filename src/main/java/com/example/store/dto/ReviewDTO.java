package com.example.store.dto;

import lombok.Data;

@Data
public class ReviewDTO {
    private String orderItemId;  // 订单项ID
    private Integer rating;      // 1-5 星
    private String content;      // 评价文字
    private String images;       // 图片URL逗号分隔
}
