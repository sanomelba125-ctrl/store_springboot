package com.example.store.controller;

import com.example.store.entity.UserAddress;
import com.example.store.service.UserAddressService;
import com.example.store.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "地址管理")
@RestController
@RequestMapping("/address")
public class UserAddressController {

    @Autowired
    private UserAddressService addressService;

    @Operation(summary = "获取当前用户的地址列表")
    @GetMapping("/list/{userId}")
    public Result list(@PathVariable String userId) {
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
        if (address.getUserId() == null) {
            return new Result().fail("用户ID缺失");
        }
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
        addressService.removeById(id);
        return new Result().success("删除成功");
    }
}