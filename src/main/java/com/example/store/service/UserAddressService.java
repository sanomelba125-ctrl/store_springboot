package com.example.store.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.store.entity.UserAddress;
import java.util.List;

public interface UserAddressService extends IService<UserAddress> {

    // 根据用户ID获取地址列表
    List<UserAddress> getListByUserId(String userId);

    // 新增地址（包含默认地址逻辑处理）
    boolean saveAddress(UserAddress address);

    // 修改地址（包含默认地址逻辑处理）
    boolean updateAddress(UserAddress address);
}