package com.example.store.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.store.entity.Shopcart;
import com.example.store.service.ShopcartService;
import com.example.store.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "购物车管理")
@RestController
@RequestMapping("/cart")
public class ShopcartController {

    @Autowired
    private ShopcartService shopcartService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 辅助方法：从 Token 中获取 UserId
     */
    private String getUserId(String token) {
        if (!StringUtils.hasText(token)) return null;
        String userJson = redisTemplate.opsForValue().get("login_token:" + token);
        if (!StringUtils.hasText(userJson)) return null;
        JSONObject jsonObject = JSON.parseObject(userJson);
        return jsonObject.getString("id");
    }

    @Operation(summary = "加入购物车")
    @PostMapping("/add")
    public Result add(@RequestHeader("token") String token,
                      @RequestBody Shopcart params) { // 前端传 goodsId 和 number
        String userId = getUserId(token);
        if (userId == null) return new Result().againLogin("请先登录");

        return shopcartService.addCart(userId, params.getGoodsId(), params.getNumber());
    }

    @Operation(summary = "我的购物车列表")
    @GetMapping("/list")
    public Result list(@RequestHeader("token") String token,
                       @RequestParam(required = false) String keyword) {
        String userId = getUserId(token);
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