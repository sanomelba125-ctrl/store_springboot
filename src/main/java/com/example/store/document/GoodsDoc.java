package com.example.store.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * ES 商品文档，映射 goods 索引
 * 与 MySQL 的 Goods 实体解耦，只存搜索所需字段
 */
@Data
@Document(indexName = "goods")
public class GoodsDoc {

    @Id
    private String id;

    /** 商品名称，IK 最大化分词，支持模糊搜索 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    /** 商品描述，IK 最大化分词 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /** 分类ID，Keyword 不分词，用于精确过滤 */
    @Field(type = FieldType.Keyword)
    private String categoryId;

    /** 店铺ID，Keyword 不分词 */
    @Field(type = FieldType.Keyword)
    private String shopId;

    @Field(type = FieldType.Double)
    private Double price;

    /** 图片路径，前端展示用 */
    @Field(type = FieldType.Keyword, index = false)
    private String pic;

    /** 商品状态：1=上架，0=下架；只索引上架商品 */
    @Field(type = FieldType.Integer)
    private Integer status;
}
