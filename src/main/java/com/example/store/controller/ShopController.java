package com.example.store.controller;

import com.example.store.dto.ShopDTO;
import com.example.store.entity.Shop;
import com.example.store.service.ShopService;
import com.example.store.utils.DateUtil;
import com.example.store.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "店铺管理")
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    private ShopService shopService;

    /**
     * 从 Spring Security 上下文获取当前登录用户的 ID
     * JwtAuthenticationFilter 在过滤阶段已将 userId 写入 SecurityContext。
     */
    private String getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof String) ? (String) principal : null;
    }

    @Operation(summary = "获取店铺详情")
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable String id) {
        Shop shop = shopService.getById(id);
        return shop != null ? new Result().success().setData(shop) : new Result().fail("店铺不存在");
    }

    @Operation(summary = "店铺列表/搜索(公开)")
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String name) {
        return shopService.pageList(pageNum, pageSize, name);
    }

    // 信息管理员专用

    @Operation(summary = "我的店铺列表")
    @GetMapping("/my")
    public Result myShops(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String name) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请登录");
        return shopService.getMyShopsPage(userId, pageNum, pageSize, name);
    }

    @PostMapping("/save")
    public Result save(@RequestBody ShopDTO shopDTO) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请登录");

        Shop shop = new Shop();
        BeanUtils.copyProperties(shopDTO, shop);

        if (!StringUtils.hasText(shop.getId())) {
            // 强制绑定当前登录用户
            shop.setUserId(userId);
            shop.setCreateTime(DateUtil.getCurrentTime());
            shopService.save(shop);
        } else {
            // 校验该店铺是否属于当前用户
            Shop oldShop = shopService.getById(shop.getId());
            if (oldShop == null || !oldShop.getUserId().equals(userId)) {
                return new Result().fail("非法操作");
            }
            shopService.updateById(shop);
        }
        return new Result().success("操作成功");
    }

    @Operation(summary = "删除店铺")
    @PostMapping("/delete")
    public Result delete(@RequestParam String id) {
        String userId = getCurrentUserId();
        if (userId == null) return new Result().againLogin("请登录");
        
        Shop oldShop = shopService.getById(id);
        if (oldShop == null || !oldShop.getUserId().equals(userId)) {
            return new Result().fail("非法操作：无权删除该店铺");
        }
        return shopService.deleteShop(id);
    }
}