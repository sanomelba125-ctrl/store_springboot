package com.example.store.vo;

import com.example.store.entity.Order;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 订单视图对象，包含订单项列表
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderVO extends Order {
    private List<OrderItemVO> items; // 订单下的商品列表
    private String refund;
    private String refundAdmin;
}