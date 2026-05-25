package com.example.store.controller;

import com.example.store.dto.ReviewDTO;
import com.example.store.service.ReviewService;
import com.example.store.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商品评价")
@RestController
@RequestMapping("/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    private String getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof String) ? (String) principal : null;
    }

    @Operation(summary = "提交评价")
    @PostMapping("/submit")
    public Result submitReview(@RequestBody ReviewDTO dto) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return new Result().againLogin("请先登录");
        }
        return reviewService.submitReview(userId, dto);
    }

    @Operation(summary = "获取商品评价列表")
    @GetMapping("/list/{goodsId}")
    public Result getReviewList(@PathVariable String goodsId,
                                @RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) Boolean hasImages) {
        return reviewService.getReviewsByGoodsId(goodsId, pageNum, pageSize, hasImages);
    }

    @Operation(summary = "获取商品评价聚合统计")
    @GetMapping("/stats/{goodsId}")
    public Result getReviewStats(@PathVariable String goodsId) {
        return reviewService.getReviewStats(goodsId);
    }

    @Operation(summary = "买家追加评价")
    @PostMapping("/append")
    public Result appendReview(@RequestBody com.example.store.dto.AppendReviewDTO dto) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请先登录");
        return reviewService.appendReview(userId, dto);
    }

    @Operation(summary = "获取某订单项的评价信息(用于追评)")
    @GetMapping("/item/{orderItemId}")
    public Result getReviewByOrderItem(@PathVariable String orderItemId) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请先登录");
        com.example.store.entity.GoodsReview review = reviewService.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.store.entity.GoodsReview>()
                .eq("order_item_id", orderItemId).eq("user_id", userId)
        );
        return new Result().success().setData(review);
    }

    @Operation(summary = "商家回复评价")
    @PostMapping("/merchant/reply")
    public Result merchantReply(@RequestBody com.example.store.dto.MerchantReplyDTO dto) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请先登录");
        return reviewService.merchantReply(userId, dto);
    }
}
