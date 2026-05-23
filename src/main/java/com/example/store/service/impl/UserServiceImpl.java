package com.example.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.store.dto.LoginDTO;
import com.example.store.dto.RegisterDTO;
import com.example.store.dto.UserPageDTO;
import com.example.store.entity.User;
import com.example.store.mapper.UserMapper;
import com.example.store.security.JwtAuthenticationFilter;
import com.example.store.service.UserService;
import com.example.store.entity.Shopcart;
import com.example.store.mapper.ShopcartMapper;
import com.example.store.utils.DateUtil;
import com.example.store.utils.JwtUtil;
import com.example.store.utils.MD5Util;
import com.example.store.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ShopcartMapper shopcartMapper;
    @Autowired
    private JwtUtil jwtUtil;

    // Redis Key 前缀定义
    private static final String KEY_USER_CURRENT_JTI = "user:current:jti:"; // userId -> 当前 jti（互踢用）
    private static final long EXPIRE_TIME = 30;


    @Override
    public Result login(LoginDTO loginDTO) {
        // 验证码校验逻辑
        String inputCode = loginDTO.getCode();
        String uuid = loginDTO.getUuid();

        if (!StringUtils.hasText(inputCode) || !StringUtils.hasText(uuid)) {
            return new Result().fail("请输入验证码");
        }

        String redisKey = "captcha:" + uuid;
        String realCode = redisTemplate.opsForValue().get(redisKey);

        if (!StringUtils.hasText(realCode)) {
            return new Result().fail("验证码已过期，请刷新重试");
        }

        // 忽略大小写比较
        if (!realCode.equalsIgnoreCase(inputCode)) {
            return new Result().fail("验证码错误");
        }
        redisTemplate.delete(redisKey);

        //
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        // 校验用户名
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        User user = this.getOne(wrapper);

        if (user == null) {
            return new Result().fail("用户不存在");
        }

        // 校验用户状态
        if (user.getUseful() != null && user.getUseful() == 0) {
            return new Result().fail("账号已被禁用，请联系管理员");
        }

        String salt = user.getSalt();
        String inputMd5 =MD5Util.md5(password, salt);

        if (!user.getPassword().equals(inputMd5)) {
            return new Result().fail("密码错误");
        }

        //互踢逻辑：将旧 JWT 的 jti 加入黑名单
        String userId = user.getId();
        String userJtiKey = KEY_USER_CURRENT_JTI + userId;
        String oldJti = redisTemplate.opsForValue().get(userJtiKey);
        if (StringUtils.hasText(oldJti)) {
            // 旧 token 加入黑名单，TTL 设为 JWT 过期时间（保守处理）
            redisTemplate.opsForValue().set(
                    JwtAuthenticationFilter.BLACKLIST_PREFIX + oldJti,
                    "1", EXPIRE_TIME, TimeUnit.MINUTES);
        }

        // 用 etc 填充购物车数量
        QueryWrapper<Shopcart> cartWrapper = new QueryWrapper<>();
        cartWrapper.eq("user_id", user.getId());
        List<Shopcart> cartList = shopcartMapper.selectList(cartWrapper);
        int totalCount = cartList.stream().mapToInt(Shopcart::getNumber).sum();
        user.getEtc().put("cartCount", totalCount);

        // 生成 JWT
        String jwt = jwtUtil.generateToken(user.getId(), user.getType());
        String newJti = jwtUtil.getJti(jwt);

        // 记录当前用户的 jti（用于下次登录时互踢）
        redisTemplate.opsForValue().set(userJtiKey, newJti, EXPIRE_TIME, TimeUnit.MINUTES);

        // 脱敏后返回
        user.setPassword(null);
        user.setSalt(null);

        Map<String, Object> map = new HashMap<>();
        map.put("token", jwt);
        map.put("user", user);

        return new Result().success("登录成功").setData(map);
    }


    @Override
    public Result register(RegisterDTO registerDTO) {
        String username = registerDTO.getUsername();
        String password = registerDTO.getPassword();
        String mobile = registerDTO.getMobile();

        // 校验
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return new Result().fail("用户名和密码不能为空");
        }

        //正则校验手机号
        String mobileRegex = "^1[3-9]\\d{9}$";
        if (StringUtils.hasText(mobile) && !mobile.matches(mobileRegex)) {
            return new Result().fail("手机号格式不正确");
        }

        //检查用户名是否已存在
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        if (this.count(wrapper) > 0) {
            return new Result().fail("该用户名已被注册，请更换");
        }

        // 准备新用户数据
        User user = new User();
        user.setUsername(username);
        user.setNickname(registerDTO.getNickname());
        user.setMobile(mobile);
        user.setEmail(registerDTO.getEmail());
        user.setSex(registerDTO.getSex() == null ? 2 : registerDTO.getSex());

        //密码加密处理 (MD5 + 随机盐)
        String salt = MD5Util.generateSalt();
        String md5Password = MD5Util.md5(password, salt);
        user.setSalt(salt);
        user.setPassword(md5Password);

        // 设置默认值
        user.setType(3);
        user.setUseful(1);
        user.setPoints(0);
        user.setCreateTime(DateUtil.getCurrentTime());

        //保存到数据库
        boolean success = this.save(user);

        if (!success) {
            return new Result().fail("注册失败，请稍后重试");
        }

        return new Result().success("注册成功");
    }

    @Override
    public Result getUserPage(UserPageDTO dto) {
        // 获取分页参数，若为空则使用默认值 (DTO中已设默认值，双重保险)
        int pageNum = dto.getPageNum() == null ? 1 : dto.getPageNum();
        int pageSize = dto.getPageSize() == null ? 10 : dto.getPageSize();

        Page<User> page = new Page<>(pageNum, pageSize);
        QueryWrapper<User> wrapper = new QueryWrapper<>();

        // 筛选用户类型
        if (dto.getType() != null) {
            wrapper.eq("type", dto.getType());
        }

        // 关键词搜索
        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w.like("username", dto.getKeyword())
                    .or()
                    .like("nickname", dto.getKeyword()));
        }

        wrapper.orderByDesc("create_time");

        // 排除密码和盐值
        wrapper.select(User.class, info -> !info.getColumn().equals("password") && !info.getColumn().equals("salt"));

        this.page(page, wrapper);
        return new Result().success().setData(page);
    }

    @Override
    public Result saveOrUpdateInfoAdmin(User user) {
        // 如果 ID 为空，则是新增
        if (!StringUtils.hasText(user.getId())) {
            // 校验用户名是否存在
            QueryWrapper<User> checkWrapper = new QueryWrapper<>();
            checkWrapper.eq("username", user.getUsername());
            if (this.count(checkWrapper) > 0) {
                return new Result().fail("用户名已存在");
            }

            // 校验密码是否为空
            if (!StringUtils.hasText(user.getPassword())) {
                return new Result().fail("新增管理员必须设置密码");
            }

            // 设置基本信息
            user.setType(2); // 固定为信息管理员
            user.setCreateTime(DateUtil.getCurrentTime());
            user.setPoints(0);
            if (user.getUseful() == null) user.setUseful(1);

            // 加密前端传来的密码
            String salt = MD5Util.generateSalt();
            user.setSalt(salt);
            user.setPassword(MD5Util.md5(user.getPassword(), salt));

            this.save(user);
            return new Result().success("添加成功");
        } else {
            // 修改逻辑 (不修改密码，密码修改走重置或单独接口)
            User updateObj = new User();
            updateObj.setId(user.getId());
            updateObj.setNickname(user.getNickname());
            updateObj.setMobile(user.getMobile());
            updateObj.setEmail(user.getEmail());
            // ... 其他允许修改的字段

            this.updateById(updateObj);
            return new Result().success("修改成功");
        }
    }

    @Override
    public Result changeStatus(String id, Integer useful) {
        User user = new User();
        user.setId(id);
        user.setUseful(useful);
        boolean success = this.updateById(user);

        // 如果禁用，将该用户当前 JWT 的 jti 加入黑名单，立即踢下线
        if (success && useful == 0) {
            String userJtiKey = KEY_USER_CURRENT_JTI + id;
            String currentJti = redisTemplate.opsForValue().get(userJtiKey);
            if (StringUtils.hasText(currentJti)) {
                redisTemplate.opsForValue().set(
                        JwtAuthenticationFilter.BLACKLIST_PREFIX + currentJti,
                        "1", EXPIRE_TIME, TimeUnit.MINUTES);
                redisTemplate.delete(userJtiKey);
            }
        }

        return success ? new Result().success("操作成功") : new Result().fail("操作失败");
    }

    @Override
    public Result resetPassword(String id) {
        User user = this.getById(id);
        if (user == null) return new Result().fail("用户不存在");

        String salt = MD5Util.generateSalt();
        String newPwd = MD5Util.md5("123456", salt);

        user.setSalt(salt);
        user.setPassword(newPwd);

        this.updateById(user);
        return new Result().success("密码已重置为 123456");
    }

    @Override
    public Result deleteUser(String id) {
        User user = this.getById(id);
        if (user == null) {
            return new Result().fail("用户不存在");
        }

        boolean success = this.removeById(id);

        // 将该用户当前 JWT 的 jti 加入黑名单
        if (success) {
            String userJtiKey = KEY_USER_CURRENT_JTI + id;
            String currentJti = redisTemplate.opsForValue().get(userJtiKey);
            if (StringUtils.hasText(currentJti)) {
                redisTemplate.opsForValue().set(
                        JwtAuthenticationFilter.BLACKLIST_PREFIX + currentJti,
                        "1", EXPIRE_TIME, TimeUnit.MINUTES);
                redisTemplate.delete(userJtiKey);
            }
        }

        return success ? new Result().success("删除成功") : new Result().fail("删除失败");
    }
}