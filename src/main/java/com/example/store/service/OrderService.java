package com.example.store.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.store.dto.OrderAuditDTO;
import com.example.store.dto.OrderSubmitDTO;
import com.example.store.entity.Order;
import com.example.store.utils.Result;

public interface OrderService extends IService<Order> {
    // 创建订单
    Result createOrder(String userId, OrderSubmitDTO submitDTO);

    // 支付订单（模拟）
    Result payOrder(String userId, String orderId);

    // 取消订单
    Result cancelOrder(String userId, String orderId);

    // 确认收货
    Result receiveOrder(String userId, String orderId);

    // 分页获取我的订单列表
    Result pageList(String userId, int pageNum, int pageSize, Integer status);

    // 获取订单详情
    Result getDetail(String userId, String orderId);


    // 商家获取订单列表
    Result getShopOrderPage(String adminId, int pageNum, int pageSize,Integer status);

    // 商家发货
    Result deliveryOrder(String adminId, String orderId);

    // 商家审核退单
    Result auditRefund(String adminId, OrderAuditDTO auditDTO);

    // 商家强制退单
    Result forceRefund(String adminId, String orderId, String reason);

    //用户申请退款
    Result applyRefund(String userId, String orderId, String reason);
}