package com.example.store.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.example.store.dto.LoginDTO;
import com.example.store.dto.RegisterDTO;
import com.example.store.entity.User;
import com.example.store.service.UserService;
import com.example.store.utils.Result;
import com.example.store.service.ShopcartService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.store.entity.Shopcart;
import io.swagger.v3.oas.annotations.Operation; // 对应 SpringDoc/Swagger3
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ShopcartService shopcartService;

    @Autowired
    private StringRedisTemplate redisTemplate; // 引入 Redis 用于退出登录

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO loginDTO) {

        if (loginDTO.getUsername() == null || loginDTO.getPassword() == null) {
            Result res = new Result();
            res.fail("用户名或密码不能为空");
            return res;
        }
        return userService.login(loginDTO);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result register(@RequestBody RegisterDTO registerDTO) {
        // 简单的非空判断
        if (registerDTO.getUsername() == null || registerDTO.getPassword() == null) {
            return new Result().fail("注册信息不完整");
        }
        return userService.register(registerDTO);
    }

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result getCaptcha() {
        // 成验证码图片
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String code = captcha.getCode();

        // 生成一个唯一标识 UUID，作为 Redis 的 Key
        String uuid = UUID.randomUUID().toString();
        String redisKey = "captcha:" + uuid;

        // 存入 Redis，设置 2 分钟过期
        redisTemplate.opsForValue().set(redisKey, code, 2, TimeUnit.MINUTES);

        // 组装结果返回给前端
        Map<String, String> map = new HashMap<>();
        map.put("uuid", uuid);
        map.put("img", captcha.getImageBase64Data());

        return new Result().success().setData(map);
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/info/{id}")
    public Result getUserInfo(@PathVariable String id) {
        User user = userService.getById(id);
        if (user == null) {
            return new Result().fail("用户不存在");
        }

        // 填充 etc数据
        QueryWrapper<Shopcart> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", id);
        int count = 0;
        // 统计所有购物车项的 number 之和
        for(Shopcart item : shopcartService.list(wrapper)) {
            count += item.getNumber();
        }
        user.getEtc().put("cartCount", count);

        // 脱敏处理，不返回密码和盐值
        user.setPassword(null);
        user.setSalt(null);
        return new Result().success().setData(user);
    }

    @Operation(summary = "修改个人信息")
    @PostMapping("/updateInfo")
    public Result updateInfo(@RequestBody User user) {
        // 禁止修改敏感字段
        user.setUsername(null); // 用户名不可改
        user.setPassword(null); // 密码这里不能修改
        user.setSalt(null);

        // 如果用户修改了手机号，必须校验格式
        if (StringUtils.hasText(user.getMobile())) {
            String mobileRegex = "^1[3-9]\\d{9}$";
            if (!user.getMobile().matches(mobileRegex)) {
                return new Result().fail("手机号格式错误");
            }
        }

        boolean success = userService.updateById(user);
        if (success) {
            // 返回最新的用户信息给前端更新缓存
            User newUser = userService.getById(user.getId());
            newUser.setPassword(null);
            newUser.setSalt(null);
            return new Result().success("修改成功").setData(newUser);
        }
        return new Result().fail("修改失败");
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result logout(@RequestHeader("token") String token) {
        // 从 Redis 中删除 Token
        String keyToken = "login_token:" + token;
        redisTemplate.delete(keyToken);
        return new Result().success("已退出登录");
    }
}