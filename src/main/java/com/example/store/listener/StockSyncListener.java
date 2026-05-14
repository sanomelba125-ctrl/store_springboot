package com.example.store.listener;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.store.config.RabbitMQConfig;
import com.example.store.entity.Goods;
import com.example.store.mapper.GoodsMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 库存同步消费者
 *
 * 职责：消费 MQ 中的库存扣减消息，将 Redis 预扣减的结果异步同步到 MySQL。
 * 消息格式："goodsId:number"
 *
 * 注意：此处不加 @Transactional，因为乐观锁更新本身是原子的；
 * 若需要重试机制，可结合 RabbitMQ 的 nack + requeue 策略。
 */
@Component
public class StockSyncListener {

    @Autowired
    private GoodsMapper goodsMapper;

    @RabbitListener(queues = RabbitMQConfig.STOCK_SYNC_QUEUE)
    public void handleStockSync(String message) {
        try {
            // 解析消息格式："goodsId:number"
            String[] parts = message.split(":");
            if (parts.length != 2) {
                System.err.println("[库存同步] 消息格式异常，已丢弃：" + message);
                return;
            }

            String goodsId = parts[0];
            int number = Integer.parseInt(parts[1]);

            // 使用乐观锁形式扣减 MySQL 库存
            // 条件：inventory >= number，防止超卖
            UpdateWrapper<Goods> updateWrapper = new UpdateWrapper<>();
            updateWrapper.setSql("inventory = inventory - " + number)
                    .eq("id", goodsId)
                    .ge("inventory", number);

            int rows = goodsMapper.update(null, updateWrapper);

            if (rows == 0) {
                // 受影响行数为 0：说明 MySQL 库存已不足，而 Redis 已扣减成功
                // 这意味着 Redis 与 MySQL 基础数据出现了不一致，需要人工介入排查
                System.err.println("[库存同步] ⚠️ 严重警告：MySQL 库存扣减失败（受影响行数为0），" +
                        "Redis 与 MySQL 数据可能已不一致！goodsId=" + goodsId + "，扣减数量=" + number);
            }

        } catch (Exception e) {
            System.err.println("[库存同步] 处理消息时发生异常，message=" + message + "，原因：" + e.getMessage());
        }
    }
}
