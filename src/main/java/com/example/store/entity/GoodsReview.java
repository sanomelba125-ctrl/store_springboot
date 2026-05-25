package com.example.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品评价表
 */
@Data
@TableName("goods_review")
public class GoodsReview implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;
    private String orderId;
    private String orderItemId;
    private String goodsId;
    private String userId;
    private Integer rating;     // 1-5
    private String content;
    private String images;      // 逗号分隔URL
    private String createTime;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer commentCount;
    private String appendContent;
    private String appendImages;
    private String appendTime;
    private String merchantReply;
    private String merchantReplyTime;
}
