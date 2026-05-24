package com.example.store.controller;

import com.alibaba.fastjson2.JSONObject;
import com.example.store.config.RabbitMQConfig;
import com.example.store.document.GoodsDoc;
import com.example.store.dto.GoodsDTO;
import com.example.store.entity.Goods;
import com.example.store.entity.Shop;
import com.example.store.mapper.ShopMapper;
import com.example.store.listener.EsSyncListener;
import com.example.store.repository.GoodsDocRepository;
import com.example.store.service.GoodsService;
import com.example.store.utils.DateUtil;
import com.example.store.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "商品管理")
@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private GoodsDocRepository goodsDocRepository;
    @Autowired
    private ShopMapper shopMapper;

    private boolean checkShopOwner(String userId, String shopId) {
        if (!StringUtils.hasText(shopId)) return false;
        Shop shop = shopMapper.selectById(shopId);
        return shop != null && userId.equals(shop.getUserId());
    }

    // 从 Token 获取 UserId
   // 直接从 Security 上下文中拿，不需要再查 Redis
    private String getUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        }
        return null;
    }

    /** 向 ES 同步队列发送消息 */
    private void sendEsSyncMsg(String action, String goodsId) {
        JSONObject msg = new JSONObject();
        msg.put("action", action);
        msg.put("goodsId", goodsId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ES_GOODS_SYNC_EXCHANGE,
                RabbitMQConfig.ES_GOODS_SYNC_ROUTING_KEY,
                msg.toJSONString()
        );
    }

    @Operation(summary = "首页商品列表/搜索")
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) String shopId,
                       @RequestParam(required = false) String categoryId) {
        return goodsService.pageList(pageNum, pageSize, name, shopId, categoryId);
    }

    @Operation(summary = "首页轮播图推荐")
    @GetMapping("/hot")
    public Result hot() {
        return goodsService.getHotGoods(5);
    }

    @Operation(summary = "商品详情")
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable String id) {
        return goodsService.getDetail(id);
    }

    // 信息管理员专用接口

    @Operation(summary = "获取我的商品列表")
    @GetMapping("/my")
    public Result myGoods(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) String shopId) {
        String userId = getUserId();
        if (userId == null) return new Result().againLogin("登录已过期");
        return goodsService.getMyGoodsPage(pageNum, pageSize, name, userId, shopId);
    }

    @Operation(summary = "新增/更新商品")
    @PostMapping("/save")
    public Result save(@RequestBody GoodsDTO goodsDTO) {
        String userId = getUserId();
        if (userId == null) return new Result().againLogin("请先登录");

        if (StringUtils.hasText(goodsDTO.getId())) {
            // 更新：校验原商品是否属于当前用户的店铺
            Goods oldGoods = goodsService.getById(goodsDTO.getId());
            if (oldGoods == null || !checkShopOwner(userId, oldGoods.getShopId())) {
                return new Result().fail("非法操作：无权修改该商品");
            }
        } else {
            // 新增：校验目标店铺是否属于当前用户
            if (!checkShopOwner(userId, goodsDTO.getShopId())) {
                return new Result().fail("非法操作：无权在该店铺新增商品");
            }
        }

        Goods goods = new Goods();
        BeanUtils.copyProperties(goodsDTO, goods);

        if (!StringUtils.hasText(goods.getName())) return new Result().fail("名称不能为空");

        if (!StringUtils.hasText(goods.getId())) {
            goods.setCreateTime(DateUtil.getCurrentTime());
            goods.setStatus(1);
            goodsService.save(goods);
        } else {
            goodsService.updateById(goods);
        }

        // 通知 ES 同步（异步，不阻塞主流程）
        sendEsSyncMsg("upsert", goods.getId());

        return new Result().success("保存成功");
    }

    @Operation(summary = "删除商品")
    @PostMapping("/delete")
    public Result delete(@RequestParam String id) {
        String userId = getUserId();
        if (userId == null) return new Result().againLogin("请先登录");

        Goods goods = goodsService.getById(id);
        if (goods == null || !checkShopOwner(userId, goods.getShopId())) {
            return new Result().fail("非法操作：无权删除该商品");
        }

        goodsService.removeById(id);
        // 通知 ES 删除文档
        sendEsSyncMsg("delete", id);
        return new Result().success("删除成功");
    }

    @Operation(summary = "商品上架/下架")
    @PostMapping("/status")
    public Result status(@RequestParam String id, @RequestParam Integer status) {
        String userId = getUserId();
        if (userId == null) return new Result().againLogin("请先登录");

        Goods oldGoods = goodsService.getById(id);
        if (oldGoods == null || !checkShopOwner(userId, oldGoods.getShopId())) {
            return new Result().fail("非法操作：无权操作该商品");
        }

        Goods goods = new Goods();
        goods.setId(id);
        goods.setStatus(status);
        goodsService.updateById(goods);
        // 状态变更同步 ES（下架时 status=0，ES 搜索会过滤掉）
        sendEsSyncMsg("upsert", id);
        return new Result().success("操作成功");
    }

    @Operation(summary = "全量同步商品数据到 ES（一次性初始化）")
    @PostMapping("/syncToEs")
    public Result syncToEs() {
        List<Goods> goodsList = goodsService.list();
        List<GoodsDoc> docs = new ArrayList<>();
        for (Goods goods : goodsList) {
            docs.add(EsSyncListener.toDoc(goods));
        }
        goodsDocRepository.saveAll(docs);
        return new Result().success().setData("全量同步 ES 成功，共导入 " + docs.size() + " 条数据");
    }
}
