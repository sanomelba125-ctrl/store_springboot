package com.example.store.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.store.dto.ReviewDTO;
import com.example.store.entity.GoodsReview;
import com.example.store.utils.Result;

public interface ReviewService extends IService<GoodsReview> {

    /**
     * 提交评价
     */
    Result submitReview(String userId, ReviewDTO dto);

    /**
     * 商品详情页分页获取评价列表
     */
    Result getReviewsByGoodsId(String goodsId, int pageNum, int pageSize, Boolean hasImages);

    /**
     * 买家追评
     */
    Result appendReview(String userId, com.example.store.dto.AppendReviewDTO dto);

    /**
     * 商家回复
     */
    Result merchantReply(String userId, com.example.store.dto.MerchantReplyDTO dto);

    /**
     * 获取评价聚合数据（优先读 Redis）
     */
    Result getReviewStats(String goodsId);
}
