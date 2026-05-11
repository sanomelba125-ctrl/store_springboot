package com.example.store.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderSubmitDTO {
    private String addressId; // 用户选择的收货地址ID
    private List<String> cartIds; // 用户勾选的购物车记录ID列表

    //立即购买
    private String goodsId;
    private Integer number;
}