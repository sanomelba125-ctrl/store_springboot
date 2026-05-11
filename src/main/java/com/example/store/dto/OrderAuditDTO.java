package com.example.store.dto;

import lombok.Data;

@Data
public class OrderAuditDTO {
    private String orderId;
    private Boolean pass; // true=通过, false=驳回
    private String reason; // 审核意见/驳回理由
}