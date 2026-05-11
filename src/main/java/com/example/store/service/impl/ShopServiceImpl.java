package com.example.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.store.entity.Goods;
import com.example.store.entity.Shop;
import com.example.store.mapper.GoodsMapper;
import com.example.store.mapper.ShopMapper;
import com.example.store.service.ShopService;
import com.example.store.utils.DateUtil;
import com.example.store.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Override
    public Result pageList(int pageNum, int pageSize, String name) {
        Page<Shop> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Shop> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(name)) {
            wrapper.like("shopname", name);
        }
        wrapper.orderByDesc("create_time");
        this.page(page, wrapper);
        return new Result().success().setData(page);
    }

    @Override
    public Result getMyShopsPage(String userId, int pageNum, int pageSize, String name) {
        Page<Shop> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Shop> wrapper = new QueryWrapper<>();

        // 必须是当前用户的店铺
        wrapper.eq("user_id", userId);

        // 如果有搜索词，进行模糊查询
        if (StringUtils.hasText(name)) {
            wrapper.like("shopname", name);
        }

        wrapper.orderByDesc("create_time");

        this.page(page, wrapper);
        return new Result().success().setData(page);
    }

    @Override
    public Result saveOrUpdateShop(Shop shop) {
        // 校验
        if (!StringUtils.hasText(shop.getShopname())) {
            return new Result().fail("店铺名称不能为空");
        }

        //正则校验
        String mobileRegex = "^1[3-9]\\d{9}$";
        if (!shop.getTelephone().matches(mobileRegex)) {
            return new Result().fail("店铺联系电话格式错误");
        }
        if (!StringUtils.hasText(shop.getId())) {
            // 新增
            shop.setCreateTime(DateUtil.getCurrentTime());
            this.save(shop);
        } else {
            // 修改
            this.updateById(shop);
        }
        return new Result().success("操作成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteShop(String shopId) {
        // 检查该店铺下是否有商品
        QueryWrapper<Goods> goodsWrapper = new QueryWrapper<>();
        goodsWrapper.eq("shop_id", shopId);
        if (goodsMapper.selectCount(goodsWrapper) > 0) {
            return new Result().fail("该店铺下还有商品，无法删除！请先清空商品。");
        }

        this.removeById(shopId);
        return new Result().success("删除成功");
    }
}