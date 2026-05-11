package com.example.store.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.store.entity.Goods;
import com.example.store.utils.Result;

public interface GoodsService extends IService<Goods> {


    // 首页公开查询
    Result pageList(int pageNum, int pageSize, String name, String shopId, String categoryId);

    // 热门商品
    Result getHotGoods(int limit);

    // 商品详情
    Result getDetail(String id);

    // 获取我的商品（信息管理员）
    Result getMyGoodsPage(int pageNum, int pageSize, String name, String userId, String shopId);
}