package com.example.store.controller;

import com.example.store.entity.Shopcart;
import com.example.store.service.ShopcartService;
import com.example.store.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "购物车管理")
@RestController
@RequestMapping("/cart")
public class ShopcartController {

    @Autowired
    private ShopcartService shopcartService;

    /**
     * 从 Spring Security 上下文获取当前登录用户的 ID
     * JwtAuthenticationFilter 在过滤阶段已将 userId 写入 SecurityContext。
     */
    private String getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof String) ? (String) principal : null;
    }

    @Operation(summary = "加入购物车")
    @PostMapping("/add")
    public Result add(@RequestBody Shopcart params) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请先登录");
        return shopcartService.addCart(userId, params.getGoodsId(), params.getNumber());
    }

    @Operation(summary = "我的购物车列表")
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) String keyword) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请先登录");
        return shopcartService.getMyCart(userId, keyword);
    }

    @Operation(summary = "修改商品数量")
    @PostMapping("/updateNumber")
    public Result updateNumber(@RequestParam String id, @RequestParam Integer number) {
        return shopcartService.updateNumber(id, number);
    }

    @Operation(summary = "删除购物车商品")
    @PostMapping("/delete")
    public Result delete(@RequestParam String ids) {
        return shopcartService.deleteCart(ids);
    }
}