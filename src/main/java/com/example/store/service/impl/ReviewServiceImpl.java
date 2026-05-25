package com.example.store.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.store.dto.ReviewDTO;
import com.example.store.entity.GoodsReview;
import com.example.store.entity.Order;
import com.example.store.entity.OrderItem;
import com.example.store.mapper.GoodsReviewMapper;
import com.example.store.mapper.OrderItemMapper;
import com.example.store.mapper.GoodsMapper;
import com.example.store.mapper.OrderMapper;
import com.example.store.mapper.ShopMapper;
import com.example.store.service.ReviewService;
import com.example.store.utils.DateUtil;
import com.example.store.utils.Result;
import com.example.store.vo.ReviewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ReviewServiceImpl extends ServiceImpl<GoodsReviewMapper, GoodsReview> implements ReviewService {

    @Autowired
    private GoodsReviewMapper goodsReviewMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private ShopMapper shopMapper;

    private static final String REVIEW_STATS_KEY_PREFIX = "goods:review:stats:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result submitReview(String userId, ReviewDTO dto) {
        if (!StringUtils.hasText(dto.getOrderItemId())) {
            return new Result().fail("订单项ID不能为空");
        }
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            return new Result().fail("星级必须在1到5之间");
        }

        // 1. 根据 orderItemId 查 OrderItem
        OrderItem orderItem = orderItemMapper.selectById(dto.getOrderItemId());
        if (orderItem == null) {
            return new Result().fail("订单商品不存在");
        }

        // 2. 查 Order 并校验归属
        Order order = orderMapper.selectById(orderItem.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            return new Result().fail("非法操作：无权评价他人订单");
        }

        // 3. 校验订单状态（3 = 已完成）
        if (order.getStatus() != 3) {
            return new Result().fail("订单未完成，无法评价");
        }

        // 4. 防止重复评价
        if (goodsReviewMapper.countByOrderItemId(dto.getOrderItemId()) > 0) {
            return new Result().fail("该商品已评价，不能重复提交");
        }

        // 5. 保存评价
        GoodsReview review = new GoodsReview();
        review.setOrderId(order.getId());
        review.setOrderItemId(orderItem.getId());
        review.setGoodsId(orderItem.getGoodsId());
        review.setUserId(userId);
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        review.setImages(dto.getImages());
        review.setCreateTime(DateUtil.getCurrentTime());

        this.save(review);

        // 6. 清除 Redis 中的聚合统计缓存
        redisTemplate.delete(REVIEW_STATS_KEY_PREFIX + orderItem.getGoodsId());

        return new Result().success("评价提交成功");
    }

    @Override
    public Result getReviewsByGoodsId(String goodsId, int pageNum, int pageSize, Boolean hasImages) {
        Page<ReviewVO> page = new Page<>(pageNum, pageSize);
        List<ReviewVO> records = goodsReviewMapper.getReviewsByGoodsId(page, goodsId, hasImages);
        Map<String, Object> map = new HashMap<>();
        map.put("list", records);
        map.put("total", page.getTotal());
        return new Result().success().setData(map);
    }

    @Override
    public Result getReviewStats(String goodsId) {
        String key = REVIEW_STATS_KEY_PREFIX + goodsId;

        // 1. 查 Redis
        String cached = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            Map<String, Object> stats = JSON.parseObject(cached, Map.class);
            return new Result().success().setData(stats);
        }

        // 2. 未命中则查 MySQL
        Map<String, Object> stats = goodsReviewMapper.getReviewStats(goodsId);
        if (stats == null) {
            stats = new HashMap<>();
            stats.put("avgRating", 0);
            stats.put("totalCount", 0);
        }

        // 3. 写入 Redis（1小时过期）
        redisTemplate.opsForValue().set(key, JSON.toJSONString(stats), 1, TimeUnit.HOURS);

        return new Result().success().setData(stats);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result appendReview(String userId, com.example.store.dto.AppendReviewDTO dto) {
        GoodsReview review = this.getById(dto.getReviewId());
        if (review == null) return new Result().fail("评价不存在");
        if (!review.getUserId().equals(userId)) return new Result().fail("无权追加他人的评价");
        if (StringUtils.hasText(review.getAppendContent())) return new Result().fail("已经追加过评价了");

        review.setAppendContent(dto.getContent());
        review.setAppendImages(dto.getImages());
        review.setAppendTime(DateUtil.getCurrentTime());
        this.updateById(review);
        return new Result().success("追评成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result merchantReply(String userId, com.example.store.dto.MerchantReplyDTO dto) {
        GoodsReview review = this.getById(dto.getReviewId());
        if (review == null) return new Result().fail("评价不存在");
        if (StringUtils.hasText(review.getMerchantReply())) return new Result().fail("已经回复过了");

        com.example.store.entity.Goods goods = goodsMapper.selectById(review.getGoodsId());
        if (goods == null) return new Result().fail("商品不存在");

        com.example.store.entity.Shop shop = shopMapper.selectById(goods.getShopId());
        if (shop == null || !shop.getUserId().equals(userId)) {
            return new Result().fail("无权回复，您不是该商品的卖家");
        }

        review.setMerchantReply(dto.getContent());
        review.setMerchantReplyTime(DateUtil.getCurrentTime());
        this.updateById(review);
        return new Result().success("回复成功");
    }
}
