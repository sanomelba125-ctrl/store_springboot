package com.example.store.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.store.dto.ReviewInteractionDTO;
import com.example.store.entity.ReviewInteraction;
import com.example.store.utils.Result;

public interface ReviewInteractionService extends IService<ReviewInteraction> {
    Result interact(String userId, ReviewInteractionDTO dto);
}
