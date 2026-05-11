package com.example.store.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.example.store.entity.Shop;
import com.example.store.utils.Result;
import java.util.List;

public interface ShopService extends IService<Shop> {
    // 新增：搜索店铺列表
    Result pageList(int pageNum, int pageSize, String name);
    // 获取我的店铺列表 (信息管理员)
    Result getMyShopsPage(String userId, int pageNum, int pageSize, String name);

    // 新增或修改店铺
    Result saveOrUpdateShop(Shop shop);

    // 删除店铺
    Result deleteShop(String shopId);
}