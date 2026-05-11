package com.example.store.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.store.dto.OrderAuditDTO;
import com.example.store.dto.OrderSubmitDTO;
import com.example.store.service.OrderService;
import com.example.store.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    //从 Token 解析 UserId
    private String getUserId(String token) {
        if (!StringUtils.hasText(token)) return null;
        String userJson = redisTemplate.opsForValue().get("login_token:" + token);
        if (!StringUtils.hasText(userJson)) return null;
        JSONObject jsonObject = JSON.parseObject(userJson);
        return jsonObject.getString("id");
    }

    @Operation(summary = "提交订单（结算）")
    @PostMapping("/add")
    public Result add(@RequestHeader("token") String token, @RequestBody OrderSubmitDTO submitDTO) {
        String userId = getUserId(token);
        if (userId == null) return new Result().againLogin("请先登录");
        return orderService.createOrder(userId, submitDTO);
    }

    @Operation(summary = "支付订单（模拟）")
    @PostMapping("/pay")
    public Result pay(@RequestHeader("token") String token, @RequestParam String orderId) {
        String userId = getUserId(token);
        if (userId == null) return new Result().againLogin("请先登录");
        return orderService.payOrder(userId, orderId);
    }

    @Operation(summary = "取消订单")
    @PostMapping("/cancel")
    public Result cancel(@RequestHeader("token") String token, @RequestParam String orderId) {
        String userId = getUserId(token);
        if (userId == null) return new Result().againLogin("请先登录");
        return orderService.cancelOrder(userId, orderId);
    }

    @Operation(summary = "确认收货")
    @PostMapping("/receive")
    public Result receive(@RequestHeader("token") String token, @RequestParam String orderId) {
        String userId = getUserId(token);
        if (userId == null) return new Result().againLogin("请先登录");
        return orderService.receiveOrder(userId, orderId);
    }

    @Operation(summary = "我的订单列表")
    @GetMapping("/list")
    public Result list(@RequestHeader("token") String token,
                       @RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) Integer status) { // status不传查所有
        String userId = getUserId(token);
        if (userId == null) return new Result().againLogin("请先登录");
        return orderService.pageList(userId, pageNum, pageSize, status);
    }

    @Operation(summary = "订单详情")
    @GetMapping("/detail/{id}")
    public Result detail(@RequestHeader("token") String token, @PathVariable String id) {
        String userId = getUserId(token);
        if (userId == null) return new Result().againLogin("请先登录");
        return orderService.getDetail(userId, id);
    }
    //信息管理员相关接口

    @Operation(summary = "商家-我的订单列表")
    @GetMapping("/shop/list")
    public Result shopOrderList(@RequestHeader("token") String token,
                                @RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) Integer status) { // 【新增参数】
        String adminId = getUserId(token);
        if (adminId == null) return new Result().againLogin("请登录");
        // 传入 status
        return orderService.getShopOrderPage(adminId, pageNum, pageSize, status);
    }

    @Operation(summary = "商家-发货")
    @PostMapping("/delivery")
    public Result delivery(@RequestHeader("token") String token, @RequestParam String orderId) {
        String adminId = getUserId(token);
        return orderService.deliveryOrder(adminId, orderId);
    }

    @Operation(summary = "商家-审核退单")
    @PostMapping("/audit/refund")
    public Result auditRefund(@RequestHeader("token") String token, @RequestBody OrderAuditDTO auditDTO) {
        String adminId = getUserId(token);
        return orderService.auditRefund(adminId, auditDTO);
    }

    @Operation(summary = "商家-强制退单")
    @PostMapping("/force/refund")
    public Result forceRefund(@RequestHeader("token") String token,
                              @RequestParam String orderId,
                              @RequestParam String reason) {
        String adminId = getUserId(token);
        return orderService.forceRefund(adminId, orderId, reason);
    }

    @Operation(summary = "用户申请退款")
    @PostMapping("/refund/apply")
    public Result applyRefund(@RequestHeader("token") String token,
                              @RequestParam String orderId,
                              @RequestParam String reason) {
        String userId = getUserId(token);
        return orderService.applyRefund(userId, orderId, reason);
    }
}