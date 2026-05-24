package com.example.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.store.config.RabbitMQConfig;
import com.example.store.dto.OrderSubmitDTO;
import com.example.store.dto.OrderAuditDTO;
import com.example.store.entity.*;
import com.example.store.mapper.*;
import com.example.store.service.OrderService;
import com.example.store.utils.DateUtil;
import com.example.store.utils.Result;
import com.example.store.utils.UrlHelper;
import com.example.store.vo.OrderItemVO;
import com.example.store.vo.OrderVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private ShopcartMapper shopcartMapper;
    @Autowired
    private GoodsMapper goodsMapper;
    @Autowired
    private UserAddressMapper addressMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ShopMapper shopMapper;
    @Autowired
    private UrlHelper urlHelper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * Redis 原子扣减库存 Lua 脚本
     * 参数：KEYS[1]=stockKey, ARGV[1]=扣减数量
     * 返回：
     *   -1 → key 不存在（Redis 中无该商品库存缓存）
     *    0 → 库存不足
     *    1 → 扣减成功
     */
    private static final DefaultRedisScript<Long> STOCK_DEDUCT_SCRIPT;
    static {
        STOCK_DEDUCT_SCRIPT = new DefaultRedisScript<>();
        STOCK_DEDUCT_SCRIPT.setResultType(Long.class);
        STOCK_DEDUCT_SCRIPT.setScriptText(
            "local stock = redis.call('get', KEYS[1]) " +
            "if stock == false then return -1 end " +
            "if tonumber(stock) < tonumber(ARGV[1]) then return 0 end " +
            "redis.call('decrby', KEYS[1], ARGV[1]) " +
            "return 1"
        );
    }

    @Value("${server.port}")
    private String port;
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createOrder(String userId, OrderSubmitDTO submitDTO) {
        // 1. 校验收货地址
        UserAddress address = addressMapper.selectById(submitDTO.getAddressId());
        if (address == null) return new Result().fail("收货地址不存在");

        // 2. 梳理待结算的商品
        List<Shopcart> settleItems = new ArrayList<>();
        boolean isFromCart = false; // 标记是否来自购物车

        // 场景A：购物车结算
        if (submitDTO.getCartIds() != null && !submitDTO.getCartIds().isEmpty()) {
            settleItems = shopcartMapper.selectBatchIds(submitDTO.getCartIds());
            if (settleItems.isEmpty()) {
                return new Result().fail("购物车数据异常");
            }
            isFromCart = true;
        }
        // 场景B：立即购买
        else if (submitDTO.getGoodsId() != null && submitDTO.getNumber() != null) {
            Shopcart tempItem = new Shopcart();
            tempItem.setGoodsId(submitDTO.getGoodsId());
            tempItem.setNumber(submitDTO.getNumber());
            settleItems.add(tempItem);
            isFromCart = false;
        }
        else {
            return new Result().fail("请选择要购买的商品");
        }

        // 3. 遍历结算项，为每种商品单独生成一个订单
        String firstOrderId = null;
        // 记录在 Redis 中预扣减成功的商品，用于异常时回滚
        List<Shopcart> redisSuccessItems = new ArrayList<>();

        try {
            for (int i = 0; i < settleItems.size(); i++) {
                Shopcart item = settleItems.get(i);
                Goods goods = goodsMapper.selectById(item.getGoodsId());

                if (goods == null || goods.getStatus() == 0) {
                    throw new RuntimeException("商品 [" + item.getGoodsId() + "] 已下架或不存在");
                }

                // ===== Redis 预扣减库存（Lua 脚本原子操作）=====
                String stockKey = "goods:stock:" + goods.getId();
                Long result = redisTemplate.execute(
                        STOCK_DEDUCT_SCRIPT,
                        Collections.singletonList(stockKey),
                        String.valueOf(item.getNumber())
                );

                if (result == null) {
                    throw new RuntimeException("商品 [" + goods.getName() + "] 库存服务异常，请稍后重试");
                }
                if (result == -1L) {
                    // Redis 中无缓存，降级到 MySQL 直接扣减（乐观锁）
                    UpdateWrapper<Goods> updateWrapper = new UpdateWrapper<>();
                    updateWrapper.setSql("inventory = inventory - " + item.getNumber())
                            .eq("id", goods.getId())
                            .ge("inventory", item.getNumber());
                    int updateRows = goodsMapper.update(null, updateWrapper);
                    if (updateRows == 0) {
                        throw new RuntimeException("商品 [" + goods.getName() + "] 库存不足！");
                    }
                    // 降级扣减成功，也记录到 redisSuccessItems（number 标记为负数表示走了 MySQL，回滚时跳过 Redis）
                    // 此处直接跳过 Redis 回滚记录，MySQL 回滚由 @Transactional 保证
                } else if (result == 0L) {
                    throw new RuntimeException("商品 [" + goods.getName() + "] 库存不足！");
                } else {
                    // Redis 预扣减成功，记录以备回滚
                    redisSuccessItems.add(item);
                }

                // ================= 生成独立的订单主表数据 =================
                Order order = new Order();
                // 使用 UUID 生成全局唯一订单号，彻底避免高并发下的重复风险
                String orderNo = "ORD" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
                order.setNo(orderNo);
                order.setUserId(userId);
                order.setReceiverName(address.getReceiverName());
                order.setReceiverMobile(address.getReceiverMobile());
                order.setReceiverAddress(address.getDetail());
                order.setStatus(0); // 待支付
                order.setCreateTime(DateUtil.getCurrentTime());
                order.setTotalPrice(goods.getPrice() * item.getNumber());

                this.save(order);
                if (firstOrderId == null) firstOrderId = order.getId();

                // ================= 生成对应的订单项数据 =================
                OrderItem orderItem = new OrderItem();
                orderItem.setGoodsId(goods.getId());
                orderItem.setNumber(item.getNumber());
                orderItem.setUnitPrice(goods.getPrice());
                orderItem.setOrderId(order.getId());
                orderItemMapper.insert(orderItem);

                // 发送延迟消息到普通交换机，TTL 到期后自动流转到死信队列触发超时取消
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.NORMAL_EXCHANGE,
                        RabbitMQConfig.NORMAL_ROUTING_KEY,
                        order.getId()
                );
            }

            // 数据库操作全部成功后，通过 MQ 异步将 Redis 预扣减同步到 MySQL
            for (Shopcart item : redisSuccessItems) {
                String msg = item.getGoodsId() + ":" + item.getNumber();
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.STOCK_SYNC_EXCHANGE,
                        RabbitMQConfig.STOCK_SYNC_ROUTING_KEY,
                        msg
                );
            }

        } catch (Exception e) {
            // Redis 回滚：将预扣减成功的库存加回去
            for (Shopcart item : redisSuccessItems) {
                String stockKey = "goods:stock:" + item.getGoodsId();
                redisTemplate.opsForValue().increment(stockKey, item.getNumber());
            }
            // 抛出异常触发 @Transactional 数据库回滚
            throw new RuntimeException(e.getMessage());
        }

        // 4. 如果是购物车结算，清空对应的购物车记录
        if (isFromCart) {
            shopcartMapper.deleteBatchIds(submitDTO.getCartIds());
        }

        // 返回第一个订单的 ID，供前端直接跳转到详情页/支付页
        return new Result().success("下单成功").setData(firstOrderId);
    }

    @Override
    public Result payOrder(String userId, String orderId) {
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return new Result().fail("订单不存在");
        }
        if (order.getStatus() != 0) {
            return new Result().fail("订单状态异常，无法支付");
        }

        order.setStatus(1); // 已支付
        order.setPayTime(DateUtil.getCurrentTime());
        this.updateById(order);
        return new Result().success("支付成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cancelOrder(String userId, String orderId) {
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return new Result().fail("订单不存在");
        }
        // 只有 待支付和 已支付待发货 可以由用户取消
        if (order.getStatus() != 0 && order.getStatus() != 1) {
            return new Result().fail("当前状态无法取消订单");
        }
        // 查询该订单下的所有商品条目
        QueryWrapper<OrderItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        List<OrderItem> items = orderItemMapper.selectList(queryWrapper);

        // 遍历条目，把库存加回去
        for (OrderItem item : items) {
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            if (goods != null) {
                // 回退 MySQL 库存
                goods.setInventory(goods.getInventory() + item.getNumber());
                goodsMapper.updateById(goods);

                // 回退 Redis 库存（若 key 存在说明下单时走了 Redis 预扣减路径，需同步加回）
                String stockKey = "goods:stock:" + item.getGoodsId();
                if (Boolean.TRUE.equals(redisTemplate.hasKey(stockKey))) {
                    redisTemplate.opsForValue().increment(stockKey, item.getNumber());
                }
            }
        }

        order.setStatus(-1); // 已取消
        order.setCancelReason("用户主动取消");

        this.updateById(order);
        return new Result().success("订单已取消");
    }

    @Override
    public Result receiveOrder(String userId, String orderId) {
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) return new Result().fail("订单不存在");

        if (order.getStatus() != 2) return new Result().fail("当前状态无法确认收货");

        order.setStatus(3); // 已收货
        this.updateById(order);
        return new Result().success("确认收货成功");
    }

    @Override
    public Result pageList(String userId, int pageNum, int pageSize, Integer status) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (status != null) {
            if (status == -99) {
                // -99 代表查询所有售后单：-2(申请中), -3(退单成功), -4(强制退单)
                wrapper.in("status", -2, -3, -4);
            } else {
                wrapper.eq("status", status);
            }
        }
        wrapper.orderByDesc("create_time");

        this.page(page, wrapper);

        List<OrderVO> voList = new ArrayList<>();

        for (Order order : page.getRecords()) {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);

            List<OrderItemVO> items = orderItemMapper.getItemsByOrderId(order.getId());

            for (OrderItemVO item : items) {
                item.setGoodsPic(urlHelper.enrichUrl(item.getGoodsPic()));
            }
            vo.setItems(items);
            voList.add(vo);
        }

        // 重新封装 Page 数据
        Page<OrderVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(page.getTotal());
        voPage.setRecords(voList);

        return new Result().success().setData(voPage);
    }

    @Override
    public Result getDetail(String userId, String orderId) {
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return new Result().fail("订单不存在");
        }

        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);

        List<OrderItemVO> items = orderItemMapper.getItemsByOrderId(orderId);

        for (OrderItemVO item : items) {
            item.setGoodsPic(urlHelper.enrichUrl(item.getGoodsPic()));
        }
        vo.setItems(items);

        return new Result().success().setData(vo);
    }


    //商家相关逻辑
    @Override
    public Result getShopOrderPage(String adminId, int pageNum, int pageSize, Integer status) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        // 调用 Mapper 时传入 status
        page = baseMapper.selectOrdersByAdminId(page, adminId, status);

        List<OrderVO> voList = new ArrayList<>();
        for (Order order : page.getRecords()) {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);

            // 查询订单项并处理图片链接
            List<OrderItemVO> items = orderItemMapper.getItemsByOrderId(order.getId());
            for (OrderItemVO item : items) {
                item.setGoodsPic(urlHelper.enrichUrl(item.getGoodsPic()));
            }
            vo.setItems(items);
            voList.add(vo);
        }

        // 重新封装分页结果
        Page<OrderVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(page.getTotal());
        voPage.setRecords(voList);

        return new Result().success().setData(voPage);
    }

    @Override
    public Result deliveryOrder(String adminId, String orderId) {
        //校验订单归属
        Order order = this.getById(orderId);
        if (order == null) return new Result().fail("订单不存在");

        // 只有待发货(1)状态才能发货
        if (order.getStatus() != 1) {
            return new Result().fail("当前状态无法发货");
        }

        order.setStatus(2); // 变更为已发货
        this.updateById(order);
        return new Result().success("发货成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result auditRefund(String adminId, OrderAuditDTO auditDTO) {
        Order order = this.getById(auditDTO.getOrderId());
        if (order == null) return new Result().fail("订单不存在");

        // 只有申请退单(-2)状态才能审核
        if (order.getStatus() != -2) {
            return new Result().fail("当前订单未申请退单");
        }

        if (auditDTO.getPass()) {
            //审核通过
            order.setStatus(-3); // 退单成功
            order.setRefundAdmin("审核通过：" + (auditDTO.getReason() == null ? "无" : auditDTO.getReason()));

            // 库存回退逻辑
            List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", order.getId()));
            for (OrderItem item : items) {
                Goods goods = goodsMapper.selectById(item.getGoodsId());
                if (goods != null) {
                    goods.setInventory(goods.getInventory() + item.getNumber());
                    goodsMapper.updateById(goods);
                }
            }
        } else {
            // --- 审核驳回 ---
            // 使用专用状态 -5（退款驳回），不能设为 3（已收货），否则语义完全错误
            order.setStatus(-5);
            order.setRefundAdmin("审核驳回：" + auditDTO.getReason());
        }

        this.updateById(order);
        return new Result().success("操作成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result forceRefund(String adminId, String orderId, String reason) {
        Order order = this.getById(orderId);
        if (order == null) return new Result().fail("订单不存在");

        // 强制退单通常用于纠纷处理，标记为 -4
        order.setStatus(-4);
        order.setRefundAdmin("管理员强制退单：" + reason);

        // 库存回退
        List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", order.getId()));
        for (OrderItem item : items) {
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            if (goods != null) {
                goods.setInventory(goods.getInventory() + item.getNumber());
                goodsMapper.updateById(goods);
            }
        }

        this.updateById(order);
        return new Result().success("强制退单成功");
    }

    // 用户申请退款实现
    @Override
    public Result applyRefund(String userId, String orderId, String reason) {
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return new Result().fail("订单不存在");
        }

        // 已发货(2)、已收货(3)、退款驳回(-5) 均可申请退款
        if (order.getStatus() != 2 && order.getStatus() != 3 && order.getStatus() != -5) {
            return new Result().fail("当前状态无法申请退款");
        }

        order.setStatus(-2); // 变更为退单审核中
        order.setRefund(reason); // 记录退款原因

        this.updateById(order);
        return new Result().success("退款申请已提交，请等待商家审核");
    }

    @Override
    public Result deleteByUser(String userId, String orderId) {
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return new Result().fail("订单不存在");
        }
        // 只允许删除已完结的订单：已取消(-1)、已收货(3)、退单成功(-3)、强制退单(-4)、退款驳回(-5)
        int status = order.getStatus();
        if (status != -1 && status != 3 && status != -3 && status != -4 && status != -5) {
            return new Result().fail("当前订单状态不允许删除，请先完结订单");
        }
        // 同步删除订单项
        orderItemMapper.delete(new QueryWrapper<OrderItem>().eq("order_id", orderId));
        this.removeById(orderId);
        return new Result().success("删除成功");
    }

    @Override
    public Result deleteByShop(String adminId, String orderId) {
        // 校验该订单是否属于该商家的店铺
        Order order = this.getById(orderId);
        if (order == null) {
            return new Result().fail("订单不存在");
        }
        // 通过 order_item -> goods -> shop 验证归属
        List<OrderItem> items = orderItemMapper.selectList(
                new QueryWrapper<OrderItem>().eq("order_id", orderId));
        if (items.isEmpty()) {
            return new Result().fail("订单数据异常");
        }
        Goods goods = goodsMapper.selectById(items.get(0).getGoodsId());
        if (goods == null) {
            return new Result().fail("商品数据异常");
        }
        // 查询该商品所属店铺是否归该管理员
        QueryWrapper<com.example.store.entity.Shop> shopWrapper = new QueryWrapper<>();
        shopWrapper.eq("id", goods.getShopId()).eq("user_id", adminId);
        // 使用 ShopMapper 验证归属
        long count = shopMapper.selectCount(shopWrapper);
        if (count == 0) {
            return new Result().fail("无权操作该订单");
        }
        // 只允许删除已完结的订单
        int status = order.getStatus();
        if (status != -1 && status != 3 && status != -3 && status != -4) {
            return new Result().fail("当前订单状态不允许删除，请先完结订单");
        }
        orderItemMapper.delete(new QueryWrapper<OrderItem>().eq("order_id", orderId));
        this.removeById(orderId);
        return new Result().success("删除成功");
    }
}