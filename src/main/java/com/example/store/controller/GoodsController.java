package com.example.store.controller;

import com.alibaba.fastjson2.JSON;
import com.example.store.dto.GoodsDTO;
import com.example.store.entity.Goods;
import com.example.store.service.GoodsService;
import com.example.store.utils.DateUtil;
import com.example.store.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商品管理")
@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    // 从 Token 获取 UserId
    private String getUserId(String token) {
        if (!StringUtils.hasText(token)) return null;
        String userJson = redisTemplate.opsForValue().get("login_token:" + token);
        if (!StringUtils.hasText(userJson)) return null;
        return JSON.parseObject(userJson).getString("id");
    }

    @Operation(summary = "首页商品列表/搜索")
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) String shopId,
                       @RequestParam(required = false) String categoryId) {
        return goodsService.pageList(pageNum, pageSize, name, shopId, categoryId);
    }

    @Operation(summary = "首页轮播图推荐")
    @GetMapping("/hot")
    public Result hot() {
        return goodsService.getHotGoods(5);
    }

    @Operation(summary = "商品详情")
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable String id) {
        return goodsService.getDetail(id);
    }

    //信息管理员专用接口

    @Operation(summary = "获取我的商品列表")
    @GetMapping("/my")
    public Result myGoods(@RequestHeader("token") String token,
                          @RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) String shopId) {
        String userId = getUserId(token);
        if (userId == null) return new Result().againLogin("登录已过期");

        return goodsService.getMyGoodsPage(pageNum, pageSize, name, userId, shopId);
    }

    @PostMapping("/save")
    public Result save(@RequestBody GoodsDTO goodsDTO) {
        Goods goods = new Goods();
        // 把前端传过来的JSON对象转为实体对象
        BeanUtils.copyProperties(goodsDTO, goods);

        if (!StringUtils.hasText(goods.getName())) return new Result().fail("名称不能为空");

        // 如果 ID 为空，说明是新增
        if (!StringUtils.hasText(goods.getId())) {
            goods.setCreateTime(DateUtil.getCurrentTime());
            goods.setStatus(1); // 默认上架
            goodsService.save(goods);
        } else {
            goodsService.updateById(goods);
        }
        return new Result().success("保存成功");
    }

    @Operation(summary = "删除商品")
    @PostMapping("/delete")
    public Result delete(@RequestParam String id) {
        goodsService.removeById(id);
        return new Result().success("删除成功");
    }

    @Operation(summary = "商品上架/下架")
    @PostMapping("/status")
    public Result status(@RequestParam String id, @RequestParam Integer status) {
        Goods goods = new Goods();
        goods.setId(id);
        goods.setStatus(status);
        goodsService.updateById(goods);
        return new Result().success("操作成功");
    }
}