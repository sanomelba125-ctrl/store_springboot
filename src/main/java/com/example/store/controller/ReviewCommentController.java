package com.example.store.controller;

import com.example.store.dto.ReviewCommentDTO;
import com.example.store.service.ReviewCommentService;
import com.example.store.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "评价评论(回复)")
@RestController
@RequestMapping("/review/comment")
public class ReviewCommentController {

    @Autowired
    private ReviewCommentService commentService;

    private String getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof String) ? (String) principal : null;
    }

    @Operation(summary = "提交评价评论/回复")
    @PostMapping("/submit")
    public Result submitComment(@RequestBody ReviewCommentDTO dto) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请先登录");
        return commentService.submitComment(userId, dto);
    }

    @Operation(summary = "获取某条评价的跟评列表")
    @GetMapping("/list/{reviewId}")
    public Result getCommentList(@PathVariable String reviewId,
                                 @RequestParam(defaultValue = "1") int pageNum,
                                 @RequestParam(defaultValue = "10") int pageSize) {
        return commentService.getCommentsByReviewId(reviewId, pageNum, pageSize);
    }
}
