// AlipayService.java
package com.example.store.service;

import com.alipay.api.AlipayApiException;
import java.util.Map;

public interface AlipayService {
    /**
     * 电脑网站支付（生成支付表单 HTML，前端直接渲染）
     */
    String pagePay(String orderNo, String totalAmount, String subject) throws AlipayApiException;

    /**
     * 验签并处理异步通知
     */
    boolean verifyNotify(Map<String, String> params);

    /**
     * 处理异步通知（更新订单状态）
     */
    void handleNotify(Map<String, String> params);

    /**
     * 主动查询支付宝交易状态，若已支付则更新订单状态
     * @param orderNo 订单号
     * @return true=已支付并更新成功，false=未支付或查询失败
     */
    boolean queryAndUpdateOrder(String orderNo) throws AlipayApiException;
}