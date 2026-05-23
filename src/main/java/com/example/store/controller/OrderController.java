package com.example.store.controller;

import com.example.store.dto.OrderAuditDTO;
import com.example.store.dto.OrderSubmitDTO;
import com.example.store.service.OrderService;
import com.example.store.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 从 Spring Security 上下文获取当前登录用户的 ID
     * JwtAuthenticationFilter 在过滤阶段已将 userId 写入 SecurityContext，
     * 此处直接读取，无需再查 Redis。
     */
    private String getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof String) ? (String) principal : null;
    }

    @Operation(summary = "提交订单（结算）")
    @PostMapping("/add")
    public Result add(@RequestBody OrderSubmitDTO submitDTO) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请先登录");
        return orderService.createOrder(userId, submitDTO);
    }

    @Operation(summary = "支付订单（模拟）")
    @PostMapping("/pay")
    public Result pay(@RequestParam String orderId) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请先登录");
        return orderService.payOrder(userId, orderId);
    }

    @Operation(summary = "取消订单")
    @PostMapping("/cancel")
    public Result cancel(@RequestParam String orderId) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请先登录");
        return orderService.cancelOrder(userId, orderId);
    }

    @Operation(summary = "确认收货")
    @PostMapping("/receive")
    public Result receive(@RequestParam String orderId) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请先登录");
        return orderService.receiveOrder(userId, orderId);
    }

    @Operation(summary = "我的订单列表")
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) Integer status) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请先登录");
        return orderService.pageList(userId, pageNum, pageSize, status);
    }

    @Operation(summary = "订单详情")
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable String id) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请先登录");
        return orderService.getDetail(userId, id);
    }

    // 信息管理员相关接口

    @Operation(summary = "商家-我的订单列表")
    @GetMapping("/shop/list")
    public Result shopOrderList(@RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) Integer status) {
        String adminId = getCurrentUserId();
        if (adminId == null) return new Result().againLogin("请登录");
        return orderService.getShopOrderPage(adminId, pageNum, pageSize, status);
    }

    @Operation(summary = "商家-发货")
    @PostMapping("/delivery")
    public Result delivery(@RequestParam String orderId) {
        String adminId = getCurrentUserId();
        return orderService.deliveryOrder(adminId, orderId);
    }

    @Operation(summary = "商家-审核退单")
    @PostMapping("/audit/refund")
    public Result auditRefund(@RequestBody OrderAuditDTO auditDTO) {
        String adminId = getCurrentUserId();
        return orderService.auditRefund(adminId, auditDTO);
    }

    @Operation(summary = "商家-强制退单")
    @PostMapping("/force/refund")
    public Result forceRefund(@RequestParam String orderId,
                              @RequestParam String reason) {
        String adminId = getCurrentUserId();
        return orderService.forceRefund(adminId, orderId, reason);
    }

    @Operation(summary = "用户申请退款")
    @PostMapping("/refund/apply")
    public Result applyRefund(@RequestParam String orderId,
                              @RequestParam String reason) {
        String userId = getCurrentUserId();
        return orderService.applyRefund(userId, orderId, reason);
    }

    @Operation(summary = "用户删除订单")
    @PostMapping("/delete")
    public Result deleteByUser(@RequestParam String orderId) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请先登录");
        return orderService.deleteByUser(userId, orderId);
    }

    @Operation(summary = "商家删除订单")
    @PostMapping("/shop/delete")
    public Result deleteByShop(@RequestParam String orderId) {
        String adminId = getCurrentUserId();
        if (adminId == null) return new Result().againLogin("请登录");
        return orderService.deleteByShop(adminId, orderId);
    }
}