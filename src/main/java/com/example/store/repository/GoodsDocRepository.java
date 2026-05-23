package com.example.store.repository;

import com.example.store.document.GoodsDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data ES Repository
 * 方法名遵循命名规范，框架自动生成查询逻辑
 */
@Repository
public interface GoodsDocRepository extends ElasticsearchRepository<GoodsDoc, String> {

    /**
     * 按 name 或 description 分词模糊搜索，只返回上架商品
     * 等价于：WHERE (name MATCH keyword OR description MATCH keyword) AND status = 1
     */
    Iterable<GoodsDoc> findByNameAndStatusOrDescriptionAndStatus(String name, Integer status1, String description, Integer status2);
}
