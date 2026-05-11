package com.example.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.store.entity.UserAddress;
import com.example.store.mapper.UserAddressMapper;
import com.example.store.service.UserAddressService;
import com.example.store.utils.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {

    @Override
    public List<UserAddress> getListByUserId(String userId) {
        QueryWrapper<UserAddress> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        // 默认地址排第一，其次按创建时间倒序
        wrapper.orderByDesc("is_default");
        wrapper.orderByDesc("create_time");
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) //以此保证数据一致性
    public boolean saveAddress(UserAddress address) {
        // 如果当前设置为默认地址，则先将该用户其他地址设为非默认
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(address.getUserId());
        }
        if (address.getCreateTime() == null) {
            address.setCreateTime(DateUtil.getCurrentTime());
        }
        return this.save(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAddress(UserAddress address) {
        // 如果修改为默认地址，则先将该用户其他地址设为非默认
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            // 修改时前端必须把 userId 传回来
            clearDefault(address.getUserId());
        }

        return this.updateById(address);
    }

    /**
     * 辅助方法将指定用户的所有地址设为非默认
     */
    private void clearDefault(String userId) {
        if (userId == null) return;
        UpdateWrapper<UserAddress> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", userId);
        updateWrapper.set("is_default", 0);
        this.update(updateWrapper);
    }
}