package com.example.store.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.store.dto.LoginDTO;
import com.example.store.dto.RegisterDTO;
import com.example.store.dto.UserPageDTO;
import com.example.store.entity.User;
import com.example.store.utils.Result;

public interface UserService extends IService<User> {
    /**
     * 用户登录接口
     *
     * @param loginDTO 登录参数
     * @return 包含 Token 和用户信息的 Result
     */
    Result login(LoginDTO loginDTO);

    Result register(RegisterDTO registerDTO);

    /**
     * 分页查询用户列表（管理员用）
     * @param userPageDTO 分页查询参数对象
     */
    Result getUserPage(UserPageDTO userPageDTO);

    //新增或修改信息管理员
    Result saveOrUpdateInfoAdmin(User user);

    //修改用户状态 (启用/禁用)

    Result changeStatus(String id, Integer useful);

    //重置密码 (前端用户)
    Result resetPassword(String id);

    // 删除用户
    Result deleteUser(String id);
}