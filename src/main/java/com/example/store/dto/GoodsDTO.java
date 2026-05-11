package com.example.store.dto;
import lombok.Data;

@Data
public class GoodsDTO {
    private String id; // 修改时需要，新增时为空
    private String name;
    private Double price;
    private String pic;
    private String description;
    private Integer inventory;
    private String shopId;
    private String categoryId;

}