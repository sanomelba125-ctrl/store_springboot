package com.example.store.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.store.entity.Shopcart;
import com.example.store.utils.Result;

public interface ShopcartService extends IService<Shopcart> {


    Result addCart(String userId, String goodsId, Integer number);

    //获取我的购物车列表,增加 keyword 参数，用于搜索
    Result getMyCart(String userId, String keyword);

    /**
     * 修改购物车内商品数量
     */
    Result updateNumber(String id, Integer number);

    /**
     * 删除购物车商品
     */
    Result deleteCart(String ids);
}