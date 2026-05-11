// AlipayServiceImpl.java
package com.example.store.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.example.store.config.AlipayConfig;
import com.example.store.entity.Order;
import com.example.store.mapper.OrderMapper;
import com.example.store.service.AlipayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private AlipayConfig alipayConfig;
    @Autowired
    private OrderMapper orderMapper;

    @Override
    public String pagePay(String orderNo, String totalAmount, String subject) throws AlipayApiException {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        
        // 设置异步通知和同步跳转地址
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        request.setReturnUrl(alipayConfig.getReturnUrl());

        // 设置请求参数
        String bizContent = "{"
            + "\"out_trade_no\":\"" + orderNo + "\","
            + "\"total_amount\":\"" + totalAmount + "\","
            + "\"subject\":\"" + subject + "\","
            + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\""
            + "}";
        request.setBizContent(bizContent);

        return alipayClient.pageExecute(request).getBody();
    }

    @Override
    public boolean verifyNotify(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(
                params,
                alipayConfig.getAlipayPublicKey(),
                alipayConfig.getCharset(),
                alipayConfig.getSignType()
            );
        } catch (AlipayApiException e) {
            log.error("验签失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleNotify(Map<String, String> params) {
        String outTradeNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        String totalAmount = params.get("total_amount");

        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            updateOrderPaid(outTradeNo, totalAmount);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean queryAndUpdateOrder(String orderNo) throws AlipayApiException {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");

        AlipayTradeQueryResponse response = alipayClient.execute(request);
        log.info("支付宝交易查询结果 orderNo={}, tradeStatus={}, subCode={}",
                orderNo, response.getTradeStatus(), response.getSubCode());

        if (!response.isSuccess()) {
            log.warn("支付宝交易查询失败: {}", response.getSubMsg());
            return false;
        }

        String tradeStatus = response.getTradeStatus();
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            return updateOrderPaid(orderNo, response.getTotalAmount());
        }
        return false;
    }

    /**
     * 将订单状态更新为已支付（幂等：已是1则跳过）
     */
    private boolean updateOrderPaid(String orderNo, String totalAmount) {
        Order order = orderMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Order>()
                .eq("no", orderNo)
        );
        if (order == null) {
            log.warn("找不到订单: {}", orderNo);
            return false;
        }
        if (order.getStatus() != 0) {
            // 已经处理过，幂等返回 true
            log.info("订单 {} 已是状态 {}，无需重复更新", orderNo, order.getStatus());
            return true;
        }
        order.setStatus(1);
        order.setPayTime(com.example.store.utils.DateUtil.getCurrentTime());
        orderMapper.updateById(order);
        log.info("订单 {} 支付成功，金额：{}", orderNo, totalAmount);
        return true;
    }
}