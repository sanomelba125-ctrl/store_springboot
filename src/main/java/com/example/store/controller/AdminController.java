package com.example.store.controller;

import com.example.store.dto.UserPageDTO; // 引入 DTO
import com.example.store.entity.User;
import com.example.store.service.UserService;
import com.example.store.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台管理")
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;


    @Operation(summary = "用户列表(分页)")
    @GetMapping("/user/list")
    public Result list(UserPageDTO userPageDTO) {
        return userService.getUserPage(userPageDTO);
    }

    @Operation(summary = "新增/修改信息管理员")
    @PostMapping("/user/save")
    public Result save(@RequestBody User user) {
        // 简单的后端校验，防止恶意修改超级管理员
        if (user.getType() != null && user.getType() == 1) {
            return new Result().fail("无法操作超级管理员");
        }
        return userService.saveOrUpdateInfoAdmin(user);
    }

    @Operation(summary = "启用/禁用用户")
    @PostMapping("/user/status")
    public Result status(@RequestParam String id, @RequestParam Integer useful) {
        return userService.changeStatus(id, useful);
    }

    @Operation(summary = "重置密码(前端用户)")
    @PostMapping("/user/resetPwd")
    public Result resetPwd(@RequestParam String id) {
        return userService.resetPassword(id);
    }

    @Operation(summary = "删除用户")
    @PostMapping("/user/delete")
    public Result delete(@RequestParam String id) {
        return userService.deleteUser(id);
    }
}