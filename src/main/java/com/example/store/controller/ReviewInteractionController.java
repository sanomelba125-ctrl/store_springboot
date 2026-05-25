package com.example.store.controller;

import com.example.store.dto.ReviewInteractionDTO;
import com.example.store.service.ReviewInteractionService;
import com.example.store.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "评价互动(点赞)")
@RestController
@RequestMapping("/review/interact")
public class ReviewInteractionController {

    @Autowired
    private ReviewInteractionService interactionService;

    private String getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof String) ? (String) principal : null;
    }

    @Operation(summary = "点赞或点踩")
    @PostMapping
    public Result interact(@RequestBody ReviewInteractionDTO dto) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请先登录");
        return interactionService.interact(userId, dto);
    }
}
