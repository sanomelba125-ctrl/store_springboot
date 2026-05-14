package com.example.store.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.store.config.RabbitMQConfig;
import com.example.store.entity.Goods;
import com.example.store.entity.Order;
import com.example.store.entity.OrderItem;
import com.example.store.mapper.GoodsMapper;
import com.example.store.mapper.OrderItemMapper;
import com.example.store.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 订单超时自动取消监听器
 * 监听死信队列，当普通队列中的消息 TTL 到期后，消息流转到此队列
 * 检查订单状态，若仍为待支付（0），则自动取消并回退库存
 */
@Slf4j
@Component
public class OrderTimeoutListener {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @RabbitListener(queues = RabbitMQConfig.DEAD_LETTER_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderTimeout(String orderId) {
        log.info("[订单超时] 收到死信消息，orderId={}", orderId);

        // 1. 查询订单
        Order order = orderMapper.selectById(orderId);

        // 2. 健壮性判断：订单不存在或已不是待支付状态，直接丢弃
        if (order == null) {
            log.warn("[订单超时] 订单不存在，忽略。orderId={}", orderId);
            return;
        }
        if (order.getStatus() != 0) {
            log.info("[订单超时] 订单状态已变更（status={}），无需处理。orderId={}", order.getStatus(), orderId);
            return;
        }

        // 3. 将订单状态设为已取消（-1）
        order.setStatus(-1);
        order.setCancelReason("系统超时自动取消");
        orderMapper.updateById(order);
        log.info("[订单超时] 订单已自动取消，orderId={}", orderId);

        // 4. 回退库存：查出订单明细，逐条将购买数量加回商品库存
        List<OrderItem> items = orderItemMapper.selectList(
                new QueryWrapper<OrderItem>().eq("order_id", orderId)
        );

        for (OrderItem item : items) {
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            if (goods != null) {
                goods.setInventory(goods.getInventory() + item.getNumber());
                goodsMapper.updateById(goods);
                log.info("[订单超时] 库存回退，goodsId={}，回退数量={}", item.getGoodsId(), item.getNumber());
            }
        }
    }
}
