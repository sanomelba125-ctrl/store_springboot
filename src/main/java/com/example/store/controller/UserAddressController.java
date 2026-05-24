package com.example.store.controller;

import com.example.store.entity.UserAddress;
import com.example.store.service.UserAddressService;
import com.example.store.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "地址管理")
@RestController
@RequestMapping("/address")
public class UserAddressController {

    @Autowired
    private UserAddressService addressService;

    private String getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof String) ? (String) principal : null;
    }

    @Operation(summary = "获取当前用户的地址列表")
    @GetMapping("/list/{userId}")
    public Result list(@PathVariable String userId) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return new Result().againLogin("请先登录");
        if (!currentUserId.equals(userId)) {
            return new Result().fail("非法操作：无权查看他人地址");
        }

        List<UserAddress> list = addressService.getListByUserId(userId);
        return new Result().success().setData(list);
    }

    @Operation(summary = "新增地址")
    @PostMapping("/add")
    public Result add(@RequestBody UserAddress address) {
        if (address.getUserId() == null) {
            return new Result().fail("用户ID不能为空");
        }
        //手机号正则校验
        String mobileRegex = "^1[3-9]\\d{9}$";
        if (address.getReceiverMobile() != null && !address.getReceiverMobile().matches(mobileRegex)) {
            return new Result().fail("收货人手机号格式错误");
        }

        addressService.saveAddress(address);
        return new Result().success("添加成功");
    }

    @Operation(summary = "修改地址")
    @PostMapping("/update")
    public Result update(@RequestBody UserAddress address) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return new Result().againLogin("请先登录");

        if (address.getId() == null) {
            return new Result().fail("地址ID缺失");
        }
        
        UserAddress oldAddress = addressService.getById(address.getId());
        if (oldAddress == null || !oldAddress.getUserId().equals(currentUserId)) {
            return new Result().fail("非法操作：无权修改该地址");
        }

        // 强制绑定为当前用户
        address.setUserId(currentUserId);

        //手机号正则校验
        String mobileRegex = "^1[3-9]\\d{9}$";
        if (address.getReceiverMobile() != null && !address.getReceiverMobile().matches(mobileRegex)) {
            return new Result().fail("收货人手机号格式错误");
        }

        addressService.updateAddress(address);
        return new Result().success("修改成功");
    }

    @Operation(summary = "删除地址")
    @PostMapping("/delete")
    public Result delete(@RequestParam String id) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return new Result().againLogin("请先登录");

        UserAddress oldAddress = addressService.getById(id);
        if (oldAddress == null || !oldAddress.getUserId().equals(currentUserId)) {
            return new Result().fail("非法操作：无权删除该地址");
        }

        addressService.removeById(id);
        return new Result().success("删除成功");
    }
}