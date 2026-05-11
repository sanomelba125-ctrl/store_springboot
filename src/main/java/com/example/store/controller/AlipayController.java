package com.example.store.controller;

import com.alipay.api.AlipayApiException;
import com.example.store.service.AlipayService;
import com.example.store.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/alipay")
@RequiredArgsConstructor
public class AlipayController {

    private final AlipayService alipayService;

    /**
     * 前端发起支付请求，返回完整的支付表单 HTML
     */
    @PostMapping("/pay")
    public void pay(@RequestParam String orderNo,
                    @RequestParam String totalAmount,
                    @RequestParam String subject,
                    HttpServletResponse response) throws IOException, AlipayApiException {
        
        String formHtml = alipayService.pagePay(orderNo, totalAmount, subject);
        
        // 将支付宝返回的 form 直接写回给浏览器，浏览器会自动跳转到支付宝收银台
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write(formHtml);
        out.flush();
        out.close();
    }

    /**
     * 前端支付完成跳回后主动查询支付结果，更新订单状态
     */
    @GetMapping("/query")
    public Result<Boolean> query(@RequestParam String orderNo) {
        try {
            boolean paid = alipayService.queryAndUpdateOrder(orderNo);
            return new Result<Boolean>().success().setData(paid);
        } catch (AlipayApiException e) {
            log.error("查询支付结果异常 orderNo={}", orderNo, e);
            return new Result<Boolean>().addError("查询支付结果失败：" + e.getMessage());
        }
    }
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        // 把请求参数转为 Map
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            params.put(key, values[0]);
        });

        log.info("收到支付宝异步通知：{}", params);

        // 验签
        boolean signVerified = alipayService.verifyNotify(params);
        if (!signVerified) {
            log.warn("验签失败！");
            return "failure";
        }

        // 处理业务
        try {
            alipayService.handleNotify(params);
            return "success";
        } catch (Exception e) {
            log.error("处理通知异常", e);
            return "failure";
        }
    }
}