package com.example.store.vo;

import com.example.store.entity.OrderItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderItemVO extends OrderItem {
    private String goodsName;
    private String goodsPic;
}