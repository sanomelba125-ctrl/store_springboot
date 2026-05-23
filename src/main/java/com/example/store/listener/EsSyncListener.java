package com.example.store.listener;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.store.config.RabbitMQConfig;
import com.example.store.document.GoodsDoc;
import com.example.store.entity.Goods;
import com.example.store.repository.GoodsDocRepository;
import com.example.store.service.GoodsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 监听 ES 商品同步队列，保持 MySQL 与 ES 数据一致
 *
 * 消息格式（JSON）：
 *   {"action": "upsert", "goodsId": "xxx"}   新增/更新
 *   {"action": "delete", "goodsId": "xxx"}   删除
 */
@Slf4j
@Component
public class EsSyncListener {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private GoodsDocRepository goodsDocRepository;

    @RabbitListener(queues = RabbitMQConfig.ES_GOODS_SYNC_QUEUE)
    public void handleGoodsSync(String message) {
        try {
            JSONObject msg = JSON.parseObject(message);
            String action = msg.getString("action");
            String goodsId = msg.getString("goodsId");

            if ("delete".equals(action)) {
                goodsDocRepository.deleteById(goodsId);
                log.info("[ES同步] 删除商品文档，id={}", goodsId);
            } else {
                // upsert：从 MySQL 查最新数据写入 ES
                Goods goods = goodsService.getById(goodsId);
                if (goods == null) {
                    log.warn("[ES同步] 商品不存在，跳过，id={}", goodsId);
                    return;
                }
                GoodsDoc doc = toDoc(goods);
                goodsDocRepository.save(doc);
                log.info("[ES同步] 更新商品文档，id={}, name={}", goodsId, goods.getName());
            }
        } catch (Exception e) {
            log.error("[ES同步] 处理消息失败，message={}", message, e);
        }
    }

    /** Goods 实体 → GoodsDoc */
    public static GoodsDoc toDoc(Goods goods) {
        GoodsDoc doc = new GoodsDoc();
        doc.setId(goods.getId());
        doc.setName(goods.getName());
        doc.setDescription(goods.getDescription());
        doc.setCategoryId(goods.getCategoryId());
        doc.setShopId(goods.getShopId());
        doc.setPrice(goods.getPrice());
        doc.setPic(goods.getPic());
        doc.setStatus(goods.getStatus());
        return doc;
    }
}
