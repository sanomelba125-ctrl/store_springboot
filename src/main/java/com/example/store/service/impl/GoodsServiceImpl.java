package com.example.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.store.document.GoodsDoc;
import com.example.store.entity.Goods;
import com.example.store.entity.Shop;
import com.example.store.entity.Category;
import com.example.store.mapper.CategoryMapper;
import com.example.store.mapper.GoodsMapper;
import com.example.store.mapper.ShopMapper;
import com.example.store.repository.GoodsDocRepository;
import com.example.store.service.GoodsService;
import com.example.store.utils.Result;
import com.example.store.utils.UrlHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    @Autowired
    private ShopMapper shopMapper;
    @Autowired
    private UrlHelper urlHelper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private GoodsDocRepository goodsDocRepository;



    @Override
    public Result pageList(int pageNum, int pageSize, String name, String shopId, String categoryId) {

        // ===== 有关键字：走 ES 分词搜索 =====
        if (StringUtils.hasText(name)) {
            return pageListFromEs(pageNum, pageSize, name, shopId, categoryId);
        }

        // ===== 无关键字：走 MySQL 分页（原逻辑） =====
        Page<Goods> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Goods> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);

        if (StringUtils.hasText(shopId)) {
            wrapper.eq("shop_id", shopId);
        }

        if (StringUtils.hasText(categoryId)) {
            List<String> searchCategoryIds = new ArrayList<>();
            searchCategoryIds.add(categoryId);
            QueryWrapper<Category> catWrapper = new QueryWrapper<>();
            catWrapper.eq("is_deleted", 0);
            List<Category> allCategories = categoryMapper.selectList(catWrapper);
            findAllChildIds(categoryId, allCategories, searchCategoryIds);
            wrapper.in("category_id", searchCategoryIds);
        }

        wrapper.orderByAsc("create_time");
        this.page(page, wrapper);

        for (Goods goods : page.getRecords()) {
            goods.setPic(urlHelper.enrichUrl(goods.getPic()));
        }
        return new Result().success().setData(page);
    }

    /**
     * 有关键字时走 ES 搜索，结果再按 shopId/categoryId 过滤，
     * 最后封装成与 MySQL 分页相同的 Page 结构返回，前端无感知。
     */
    private Result pageListFromEs(int pageNum, int pageSize, String name,
                                  String shopId, String categoryId) {
        try {
            // 1. ES 分词搜索（name 或 description 命中，且 status=1）
            Iterable<GoodsDoc> hits = goodsDocRepository
        .findByNameAndStatusOrDescriptionAndStatus(name, 1, name, 1);

            // 2. 转为列表，按 shopId / categoryId 过滤
            List<GoodsDoc> filtered = new ArrayList<>();

            // 若需要按 categoryId 过滤，先把子分类 ID 全部算出来
            List<String> searchCategoryIds = null;
            if (StringUtils.hasText(categoryId)) {
                searchCategoryIds = new ArrayList<>();
                searchCategoryIds.add(categoryId);
                QueryWrapper<Category> catWrapper = new QueryWrapper<>();
                catWrapper.eq("is_deleted", 0);
                List<Category> allCategories = categoryMapper.selectList(catWrapper);
                findAllChildIds(categoryId, allCategories, searchCategoryIds);
            }
            final List<String> finalCategoryIds = searchCategoryIds;

            for (GoodsDoc doc : hits) {
                if (StringUtils.hasText(shopId) && !shopId.equals(doc.getShopId())) {
                    continue;
                }
                if (finalCategoryIds != null && !finalCategoryIds.contains(doc.getCategoryId())) {
                    continue;
                }
                filtered.add(doc);
            }

            // 3. 手动分页
            int total = filtered.size();
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);
            List<GoodsDoc> pageRecords = (fromIndex >= total)
                    ? new ArrayList<>()
                    : filtered.subList(fromIndex, toIndex);

            // 4. 补全图片绝对路径
            for (GoodsDoc doc : pageRecords) {
                doc.setPic(urlHelper.enrichUrl(doc.getPic()));
            }

            // 5. 封装成与 MySQL 分页一致的 Map 结构，前端字段不变
            Map<String, Object> pageResult = new HashMap<>();
            pageResult.put("records", pageRecords);
            pageResult.put("total", total);
            pageResult.put("size", pageSize);
            pageResult.put("current", pageNum);
            pageResult.put("pages", (total + pageSize - 1) / pageSize);

            return new Result().success().setData(pageResult);

        } catch (Exception e) {
            // ES 不可用时降级到 MySQL 模糊查询，保证服务可用
            log.warn("[ES搜索] ES 不可用，降级到 MySQL 模糊查询，keyword={}, error={}", name, e.getMessage());
            return pageListFromMysqlFallback(pageNum, pageSize, name, shopId, categoryId);
        }
    }

    /** ES 降级方案：MySQL LIKE 查询 */
    private Result pageListFromMysqlFallback(int pageNum, int pageSize, String name,
                                              String shopId, String categoryId) {
        Page<Goods> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Goods> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        wrapper.like("name", name);

        if (StringUtils.hasText(shopId)) {
            wrapper.eq("shop_id", shopId);
        }
        if (StringUtils.hasText(categoryId)) {
            List<String> searchCategoryIds = new ArrayList<>();
            searchCategoryIds.add(categoryId);
            QueryWrapper<Category> catWrapper = new QueryWrapper<>();
            catWrapper.eq("is_deleted", 0);
            List<Category> allCategories = categoryMapper.selectList(catWrapper);
            findAllChildIds(categoryId, allCategories, searchCategoryIds);
            wrapper.in("category_id", searchCategoryIds);
        }
        wrapper.orderByAsc("create_time");
        this.page(page, wrapper);
        for (Goods goods : page.getRecords()) {
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