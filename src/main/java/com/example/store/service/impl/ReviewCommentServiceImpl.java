package com.example.store.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.store.dto.ReviewCommentDTO;
import com.example.store.entity.GoodsReview;
import com.example.store.entity.ReviewComment;
import com.example.store.mapper.GoodsReviewMapper;
import com.example.store.mapper.ReviewCommentMapper;
import com.example.store.service.ReviewCommentService;
import com.example.store.utils.DateUtil;
import com.example.store.utils.Result;
import com.example.store.vo.ReviewCommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewCommentServiceImpl extends ServiceImpl<ReviewCommentMapper, ReviewComment> implements ReviewCommentService {

    @Autowired
    private GoodsReviewMapper goodsReviewMapper;

    @Autowired
    private ReviewCommentMapper reviewCommentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result submitComment(String userId, ReviewCommentDTO dto) {
        GoodsReview review = goodsReviewMapper.selectById(dto.getReviewId());
        if (review == null) return new Result().fail("评价不存在");

        ReviewComment comment = new ReviewComment();
        comment.setReviewId(dto.getReviewId());
        comment.setUserId(userId);
        comment.setTargetUserId(dto.getTargetUserId());
        comment.setContent(dto.getContent());
        comment.setCreateTime(DateUtil.getCurrentTime());

        this.save(comment);

        review.setCommentCount(review.getCommentCount() + 1);
        goodsReviewMapper.updateById(review);

        return new Result().success("评论成功");
    }

    @Override
    public Result getCommentsByReviewId(String reviewId, int pageNum, int pageSize) {
        Page<ReviewCommentVO> page = new Page<>(pageNum, pageSize);
        List<ReviewCommentVO> records = reviewCommentMapper.getCommentsByReviewId(page, reviewId);
        Map<String, Object> map = new HashMap<>();
        map.put("list", records);
        map.put("total", page.getTotal());
        return new Result().success().setData(map);
    }
}
