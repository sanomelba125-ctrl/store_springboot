package com.example.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.store.dto.ReviewInteractionDTO;
import com.example.store.entity.GoodsReview;
import com.example.store.entity.ReviewInteraction;
import com.example.store.mapper.GoodsReviewMapper;
import com.example.store.mapper.ReviewInteractionMapper;
import com.example.store.service.ReviewInteractionService;
import com.example.store.utils.DateUtil;
import com.example.store.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewInteractionServiceImpl extends ServiceImpl<ReviewInteractionMapper, ReviewInteraction> implements ReviewInteractionService {

    @Autowired
    private GoodsReviewMapper goodsReviewMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result interact(String userId, ReviewInteractionDTO dto) {
        String reviewId = dto.getReviewId();
        Integer newType = dto.getType(); // 1=赞, 2=踩, 0=取消

        GoodsReview review = goodsReviewMapper.selectById(reviewId);
        if (review == null) return new Result().fail("评价不存在");

        QueryWrapper<ReviewInteraction> query = new QueryWrapper<>();
        query.eq("review_id", reviewId).eq("user_id", userId);
        ReviewInteraction exist = this.getOne(query);

        int likeDiff = 0;
        int dislikeDiff = 0;

        if (exist == null) {
            if (newType == 0) return new Result().success("操作成功");
            // 新增
            ReviewInteraction interaction = new ReviewInteraction();
            interaction.setReviewId(reviewId);
            interaction.setUserId(userId);
            interaction.setType(newType);
            interaction.setCreateTime(DateUtil.getCurrentTime());
            this.save(interaction);

            if (newType == 1) likeDiff = 1;
            else if (newType == 2) dislikeDiff = 1;
        } else {
            Integer oldType = exist.getType();
            if (newType == 0) {
                // 取消
                this.removeById(exist.getId());
                if (oldType == 1) likeDiff = -1;
                else if (oldType == 2) dislikeDiff = -1;
            } else if (!oldType.equals(newType)) {
                // 修改
                exist.setType(newType);
                this.updateById(exist);
                if (oldType == 1 && newType == 2) { likeDiff = -1; dislikeDiff = 1; }
                else if (oldType == 2 && newType == 1) { likeDiff = 1; dislikeDiff = -1; }
            }
        }

        if (likeDiff != 0 || dislikeDiff != 0) {
            review.setLikeCount(Math.max(0, review.getLikeCount() + likeDiff));
            review.setDislikeCount(Math.max(0, review.getDislikeCount() + dislikeDiff));
            goodsReviewMapper.updateById(review);
        }

        return new Result().success("操作成功");
    }
}
