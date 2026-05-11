package com.example.store.vo;

import com.example.store.entity.Shopcart;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 购物车视图对象
 * 继承实体类，额外增加商品信息字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ShopcartVO extends Shopcart {
    // 额外展示的商品信息
    private String goodsName;
    private String goodsPic;
    private Double goodsPrice; // 商品当前最新价格
    private Integer goodsInventory; // 商品当前库存
    private String shopId;//店铺信息
    private String shopName;
}