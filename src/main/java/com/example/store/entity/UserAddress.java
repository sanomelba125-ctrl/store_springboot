package com.example.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;


@Data
@TableName("user_address")
public class UserAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;        // 所属用户ID

    private String receiverName;  // 收货人姓名

    private String receiverMobile; // 收货人手机号

    private String detail;        // 完整收货地址 (直接填：XX省XX市XX区XX街道XX号)

    private Integer isDefault;    // 是否默认地址 (0=否, 1=是)

    private String createTime;    // 创建时间
}