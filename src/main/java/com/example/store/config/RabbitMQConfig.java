package com.example.store.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置：普通队列 + 死信队列（用于订单超时自动取消）
 *
 * 流程：
 *   下单 → 消息发送到普通交换机 → 普通队列（TTL 30s）
 *   → 消息过期 → 自动转发到死信交换机 → 死信队列
 *   → OrderTimeoutListener 消费死信队列 → 超时取消订单
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 库存同步队列常量 ====================
    public static final String STOCK_SYNC_EXCHANGE    = "stock.sync.exchange";
    public static final String STOCK_SYNC_QUEUE       = "stock.sync.queue";
    public static final String STOCK_SYNC_ROUTING_KEY = "stock.sync.routing.key";

    // ==================== 死信（DLX）常量 ====================
    public static final String DEAD_LETTER_EXCHANGE   = "order.dlx.exchange";
    public static final String DEAD_LETTER_QUEUE      = "order.dlx.queue";
    public static final String DEAD_LETTER_ROUTING_KEY = "order.dlx.routing.key";

    // ==================== 普通队列常量 ====================
    public static final String NORMAL_EXCHANGE    = "order.normal.exchange";
    public static final String NORMAL_QUEUE       = "order.normal.queue";
    public static final String NORMAL_ROUTING_KEY = "order.normal.routing.key";

    /** 消息 TTL：30 分钟 = 1800000 ms，测试时可改为 30000（30秒） */
    private static final int MESSAGE_TTL_MS = 1_800_000;

    // ==================== 死信 Exchange & Queue ====================

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DEAD_LETTER_ROUTING_KEY);
    }

    // ==================== 普通 Exchange & Queue ====================

    @Bean
    public DirectExchange normalExchange() {
        return new DirectExchange(NORMAL_EXCHANGE, true, false);
    }

    /**
     * 普通队列：绑定死信交换机，设置消息 TTL
     * 消息到期后自动路由到死信队列
     */
    @Bean
    public Queue normalQueue() {
        Map<String, Object> args = new HashMap<>();
        // 死信交换机
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        // 死信路由键
        args.put("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY);
        // 消息 TTL（毫秒）
        args.put("x-message-ttl", MESSAGE_TTL_MS);
        return QueueBuilder.durable(NORMAL_QUEUE).withArguments(args).build();
    }

    @Bean
    public Binding normalBinding() {
        return BindingBuilder
                .bind(normalQueue())
                .to(normalExchange())
                .with(NORMAL_ROUTING_KEY);
    }

    // ==================== 库存同步 Exchange & Queue ====================

    @Bean
    public DirectExchange stockSyncExchange() {
        return new DirectExchange(STOCK_SYNC_EXCHANGE, true, false);
    }

    @Bean
    public Queue stockSyncQueue() {
        return QueueBuilder.durable(STOCK_SYNC_QUEUE).build();
    }

    @Bean
    public Binding stockSyncBinding() {
        return BindingBuilder
                .bind(stockSyncQueue())
                .to(stockSyncExchange())
                .with(STOCK_SYNC_ROUTING_KEY);
    }
}
