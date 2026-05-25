package com.example.store.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.store.dto.ReviewCommentDTO;
import com.example.store.entity.ReviewComment;
import com.example.store.utils.Result;

public interface ReviewCommentService extends IService<ReviewComment> {
    Result submitComment(String userId, ReviewCommentDTO dto);
    Result getCommentsByReviewId(String reviewId, int pageNum, int pageSize);
}
