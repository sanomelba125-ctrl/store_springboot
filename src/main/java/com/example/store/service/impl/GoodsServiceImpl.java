package com.example.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.store.entity.Goods;
import com.example.store.entity.Shop;
import com.example.store.entity.Category;
import com.example.store.mapper.CategoryMapper;
import com.example.store.mapper.GoodsMapper;
import com.example.store.mapper.ShopMapper;
import com.example.store.service.GoodsService;
import com.example.store.utils.Result;
import com.example.store.utils.UrlHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    @Autowired
    private ShopMapper shopMapper;
    @Autowired
    private UrlHelper urlHelper;
    @Autowired
    private CategoryMapper categoryMapper;



    @Override
    public Result pageList(int pageNum, int pageSize, String name, String shopId, String categoryId) {
        //创建分页对象
        Page<Goods> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Goods> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);

        // 搜索,模糊查询
        if (StringUtils.hasText(name)) {
            wrapper.like("name", name);
        }

        //如果传入了 shopId，则增加过滤条件
        if (StringUtils.hasText(shopId)) {
            wrapper.eq("shop_id", shopId);
        }

        //分类过滤逻辑
        if (StringUtils.hasText(categoryId)) {
            // 定义一个列表，存放要查的所有分类ID
            List<String> searchCategoryIds = new ArrayList<>();
            searchCategoryIds.add(categoryId);

            // 查出数据库中所有分类数据
            QueryWrapper<Category> catWrapper = new QueryWrapper<>();
            catWrapper.eq("is_deleted", 0);
            List<Category> allCategories = categoryMapper.selectList(catWrapper);

            // 递归查找当前分类的所有子分类ID
            findAllChildIds(categoryId, allCategories, searchCategoryIds);

            wrapper.in("category_id", searchCategoryIds);
        }
        // 默认按创建时间倒序
        wrapper.orderByAsc("create_time");

        this.page(page, wrapper);
        for (Goods goods : page.getRecords()) {
            //把相对路径变为绝对路径
            goods.setPic(urlHelper.enrichUrl(goods.getPic()));
        }

        return new Result().success().setData(page);
    }
    //辅助方法，递归查找子节点ID
    private void findAllChildIds(String parentId, List<Category> allList, List<String> resultList) {
        for (Category cat : allList) {
            if (cat.getParentId() != null && cat.getParentId().equals(parentId)) {
                resultList.add(cat.getId());
                // 继续找这个子分类下面的子分类（
                findAllChildIds(cat.getId(), allList, resultList);
            }
        }
    }


    @Override
    public Result getHotGoods(int limit) {
        QueryWrapper<Goods> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        wrapper.orderByDesc("price");
        wrapper.last("limit " + limit);

        // 执行查询，直接从数据库拿数据
        List<Goods> list = this.list(wrapper);
        for (Goods goods : list) {
            goods.setPic(urlHelper.enrichUrl(goods.getPic()));
        }

        return new Result().success().setData(list);
    }

    @Override
    public Result getDetail(String id) {
        Goods goods = this.getById(id);
        if (goods == null) {
            return new Result().fail("商品不存在或已下架");
        }

        goods.setPic(urlHelper.enrichUrl(goods.getPic()));

        return new Result().success().setData(goods);
    }

    @Override
    public Result getMyGoodsPage(int pageNum, int pageSize, String name, String userId, String shopId) {
        // 先查询该用户拥有的所有店铺ID
        QueryWrapper<Shop> shopWrapper = new QueryWrapper<>();
        shopWrapper.eq("user_id", userId);
        List<Shop> myShops = shopMapper.selectList(shopWrapper);

        if (myShops == null || myShops.isEmpty()) {
            // 如果名下没店，直接返回空分页
            return new Result().success().setData(new Page<Goods>(pageNum, pageSize));
        }

        List<String> myShopIds = myShops.stream().map(Shop::getId).collect(Collectors.toList());

        // 构造商品查询条件
        Page<Goods> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Goods> goodsWrapper = new QueryWrapper<>();

        // 前端传了具体的 shopId，要校验这个 shopId 是否属于该用户
        if (StringUtils.hasText(shopId)) {
            if (!myShopIds.contains(shopId)) {
                return new Result().fail("非法操作：您无权查看非名下店铺的商品");
            }
            goodsWrapper.eq("shop_id", shopId);
        } else {
            // 如果没传 shopId，则查询该用户所有店铺下的商品
            goodsWrapper.in("shop_id", myShopIds);
        }

        if (StringUtils.hasText(name)) {
            goodsWrapper.like("name", name);
        }

        goodsWrapper.orderByDesc("create_time");

        this.page(page, goodsWrapper);

        for (Goods goods : page.getRecords()) {
            goods.setPic(urlHelper.enrichUrl(goods.getPic()));
        }

        return new Result().success().setData(page);
    }
}