package com.example.store.dto;

import lombok.Data;

@Data
public class ReviewInteractionDTO {
    private String reviewId;
    private Integer type; // 1=like, 2=dislike, 0=cancel
}
