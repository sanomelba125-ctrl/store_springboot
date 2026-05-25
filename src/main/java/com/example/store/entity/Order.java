package com.example.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * 订单主表
 */
@Data
@TableName("`order`") // order是MySQL关键字，需要加反引号
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    private String no; // 订单编号

    private String userId;

    private Double totalPrice;

    // 收货人信息快照
    private String receiverName;
    private String receiverMobile;
    private String receiverAddress;

    /**
     * 订单状态
     * 0-已下单（待支付）
     * 1-已支付（待发货）
     * 2-已发货（待收货）
     * 3-已收货（已完成）
     * -1-已取消
     * -2-申请退单
     * -3-退单成功
     * -4-强制退单
     * -5-退款驳回
     */
    private Integer status;

    private String createTime;
    private String payTime;

    private String cancelReason; // 取消原因

    //用户申请退单的原因
    private String refund;

    //管理员审核备注或强退理由
    private String refundAdmin;

}